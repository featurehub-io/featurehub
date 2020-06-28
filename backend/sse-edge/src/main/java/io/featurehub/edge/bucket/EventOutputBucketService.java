package io.featurehub.edge.bucket;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.edge.client.TimedBucketClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
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
  private Map<Integer, TimedBucket> discardTimerBuckets = new ConcurrentHashMap<>();
  private int timerSlice;

  @ConfigKey("maxSlots")
  private Integer maxSlots = 30;

  public EventOutputBucketService() {
    DeclaredConfigResolver.resolve(this);

    timerSlice = maxSlots - 1;
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
      log.debug("kickout: {}", timerSlice);
      timedBucketClientConnections.expireConnections();
    }
  }

  // adds the new connection to the bucket
  public void putInBucket(TimedBucketClientConnection b) {
    discardTimerBuckets.computeIfAbsent(timerSlice, k -> new TimedBucket()).addConnection(b);
  }
}
