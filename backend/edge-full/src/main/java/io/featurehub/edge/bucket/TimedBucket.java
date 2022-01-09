package io.featurehub.edge.bucket;

import io.featurehub.edge.client.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A segment of a time sequence that gathers all of the clients that connected in that time
 * sequence together. This allows them to all be kicked off at the same time as well.
 */
public class TimedBucket implements TimedBucketSlot {
  private static final Logger log = LoggerFactory.getLogger(TimedBucket.class);
  private Map<UUID, ClientConnection> timedConnections = new ConcurrentHashMap<>();
  // connection id, ejection handler id
  private Map<UUID, UUID> ejectionHandlers = new ConcurrentHashMap<>();
  private boolean expiring = false;
  private final int timerSlice;

  public TimedBucket(int timerSlice) {
    this.timerSlice = timerSlice;
  }

  public void addConnection(ClientConnection conn) {
    timedConnections.put(conn.connectionId(), conn);

    // make sure we don't try and reclose a closed connection, helps release memory
    UUID ejectionId = conn.registerEjection(client -> {
      if (!expiring) {
        timedConnections.remove(client);
      }
    });

    conn.setTimedBucketSlot(this);

    ejectionHandlers.put(conn.connectionId(), ejectionId);
  }

  public void swapConnection(ClientConnection conn, TimedBucket newBucket) {
    timedConnections.remove(conn.connectionId());
    conn.deregisterEjection(ejectionHandlers.get(conn.connectionId()));
    newBucket.addConnection(conn);
  }

  /**
   * expire the connections as quickly as possible, allowing for clocking over quickly as well
   * but poshing the expiry list off elsewhere and replacing the current list.
   */
  public void expireConnections() {
    if (!timedConnections.isEmpty()) {
      log.debug("kickout: {}: {}", timerSlice, timedConnections.size());

      final Map<UUID, ClientConnection> conns = this.timedConnections;

      this.timedConnections = new ConcurrentHashMap<>();

      expiring = true;

      conns.values().parallelStream().forEach((c) -> {
          try {
            c.close();
          } catch (Exception e) {
            log.error("Failed to kick out connections", e);
          }
      });

      expiring = false;
    }
  }
}
