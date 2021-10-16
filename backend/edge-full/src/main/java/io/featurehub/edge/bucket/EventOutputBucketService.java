package io.featurehub.edge.bucket;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.edge.client.ClientConnection;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * this service manages the allocation of incoming connections to a timeslot and the regular
 * kicking off of connections across that time period. It defaults to cycle clients every 30 seconds
 * but this can be configured in system properties.
 */
@Singleton
public class EventOutputBucketService {
  private static final Logger log = LoggerFactory.getLogger(EventOutputBucketService.class);
  private final Map<Integer, TimedBucket> discardTimerBuckets = new ConcurrentHashMap<>();
  private int timerSlice;

  @ConfigKey("maxSlots")
  protected Integer maxSlots = 30;
  @ConfigKey("edge.dacha.response-timeout")
  protected Integer namedCacheTimeout = 2000; // milliseconds to wait for dacha to responsd

  public EventOutputBucketService() {
    DeclaredConfigResolver.resolve(this);

    if (namedCacheTimeout / 1000 > maxSlots) {
      throw new RuntimeException("You cannot wait for longer than the connection will be open. Please increase your " +
        "maxSlots or decrease your Dacha timeout.");
    }

    timerSlice = maxSlots - 1;
    startTimer();
  }

  protected void startTimer() {
    Timer secondTimer = new Timer("countdown-to-kickoff");
    secondTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        kickout();
      }
    }, 0, 1000);
  }

  // every second, we clear out the buckets going down the list
  void kickout() {
    timerSlice --;

    if (timerSlice < 0) {
      timerSlice = maxSlots - 1;
    }

    TimedBucket timedBucketClientConnections = discardTimerBuckets.remove(timerSlice);

    if (timedBucketClientConnections != null) {
      timedBucketClientConnections.expireConnections();
    }
  }

  // adds the new connection to the bucket
  public void putInBucket(ClientConnection b) {
    discardTimerBuckets.computeIfAbsent(timerSlice, k -> new TimedBucket(timerSlice)).addConnection(b);
  }
}
