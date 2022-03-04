package io.featurehub.edge.bucket;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.edge.client.ClientConnection;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * this service manages the allocation of incoming connections to a timeslot and the regular kicking
 * off of connections across that time period. It defaults to cycle clients every 30 seconds but
 * this can be configured in system properties.
 */
@Singleton
public class EventOutputBucketService implements BucketService {
  private static final Logger log = LoggerFactory.getLogger(EventOutputBucketService.class);
  private final Map<Integer, TimedBucket> discardTimerBuckets = new ConcurrentHashMap<>();
  private final Map<Integer, TimedBucket> heartbeatTimerBuckets = new ConcurrentHashMap<>();
  private final Map<Integer, List<ClientConnection>> dachaNotReadyConnections = new ConcurrentHashMap<>();
  private int dropConnectionTimer;
  private int heartbeatConnectionTimer;
  private int delaySlotTimer;

  @ConfigKey("maxSlots")
  protected Integer maxSlots = -1;

  @ConfigKey("edge.dacha.delay-slots")
  protected Integer delaySlots = 10;

  // if > 0 then we will drop the connection automatically after this period, used ot be dropAfterSeconds
  @ConfigKey("edge.sse.drop-after-seconds")
  protected Integer dropAfterSeconds = 30;

  // if zero, then no heartbeat
  @ConfigKey("edge.sse.heartbeat-period")
  protected Integer heartbeatAfterSeconds = 0;

  @ConfigKey("edge.dacha.response-timeout")
  protected Integer namedCacheTimeout = 2000; // milliseconds to wait for dacha to response

  public EventOutputBucketService() {
    DeclaredConfigResolver.resolve(this);

    if (maxSlots > 0 && dropAfterSeconds != 30) {
      dropAfterSeconds = maxSlots;
    }

    if (dropAfterSeconds > 0 && namedCacheTimeout / 1000 > dropAfterSeconds) {
      throw new RuntimeException(
          "You cannot wait for longer than the connection will be open. Please increase your "
              + "edge.sse.drop-after-seconds or decrease your edge.dacha.response-timeout millisecond value.");
    }

    if (dropAfterSeconds > 0) {
      dropConnectionTimer = dropAfterSeconds - 1;
    }

    if (heartbeatAfterSeconds > 0) {
      heartbeatConnectionTimer = heartbeatAfterSeconds - 1;
    }

    delaySlotTimer = delaySlots - 1;

    startTimer();
  }

  protected void startTimer() {
    Timer secondTimer = new Timer("countdown-to-kickoff");
    secondTimer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            // we are kicking people off? if so, kick them off first
            if (dropAfterSeconds > 0) {
              kickout();
            }

            // are we heartbeating? if so, check them too
            if (heartbeatAfterSeconds > 0) {
              heartbeatRemainingConnections();
            }

            delaySlotKickout();
          }
        },
        0,
        1000);
  }

  /**
   * every second, we check if there are connections who couldn't
   */
  void delaySlotKickout() {
    final List<ClientConnection> conns = dachaNotReadyConnections.replace(delaySlotTimer, new ArrayList<>());

    if (conns != null && !conns.isEmpty()) {
      conns.parallelStream().forEach(ClientConnection::close);
    }

    delaySlotTimer --;

    if (delaySlotTimer < 0) {
      delaySlotTimer = delaySlots -1;
    }
  }

  void heartbeatRemainingConnections() {
    heartbeatConnectionTimer --;

    if (heartbeatConnectionTimer < 0) {
      heartbeatConnectionTimer = heartbeatAfterSeconds - 1;
    }

    // in this case, we are going to write something and see if they are still there
    // if we get a connection error, it will automatically
    TimedBucket bucketToCheck = heartbeatTimerBuckets.get(heartbeatConnectionTimer);

    if (bucketToCheck != null) {
      bucketToCheck.ensureConnectionsInBucketAreActive();
    }
  }

  // every second, we clear out the buckets going down the list
  void kickout() {
    dropConnectionTimer--;

    if (dropConnectionTimer < 0) {
      dropConnectionTimer = dropAfterSeconds - 1;
    }

    // traditional behaviour, kick them off
    TimedBucket timedBucketClientConnections = discardTimerBuckets.remove(dropConnectionTimer);

    if (timedBucketClientConnections != null) {
      log.debug("expiring slice {} has {} connections", dropConnectionTimer,
        timedBucketClientConnections.numConnections());
      timedBucketClientConnections.expireConnections();
    }
  }

  // adds the new connection to the bucket
  public void putInBucket(ClientConnection b) {
    if (dropAfterSeconds > 0) {
      log.trace("Adding client connection at {}", dropConnectionTimer);
      discardTimerBuckets
        .computeIfAbsent(dropConnectionTimer, k -> new TimedBucket(TimerBucketPurpose.DROP_CONNECTIONS, dropConnectionTimer))
        .addConnection(b);
    }

    if (heartbeatAfterSeconds > 0) {
      log.trace("Adding to heartbeat timer {}", heartbeatConnectionTimer);
      heartbeatTimerBuckets.computeIfAbsent(heartbeatConnectionTimer, k -> new TimedBucket(TimerBucketPurpose.HEARTBEAT,
          heartbeatConnectionTimer))
        .addConnection(b);
    }
  }

  public void dachaIsUnavailable(ClientConnection b) {
    int newSlot = (int) (Math.random() * delaySlots);

    final List<ClientConnection> dachaDropConnections = dachaNotReadyConnections.computeIfAbsent(newSlot,
      k -> new ArrayList<>());

    dachaDropConnections.add(b);
  }
}
