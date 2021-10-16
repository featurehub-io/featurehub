package io.featurehub.edge.bucket;

import io.featurehub.edge.client.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A segment of a time sequence that gathers all of the clients that connected in that time
 * sequence together. This allows them to all be kicked off at the same time as well.
 */
public class TimedBucket {
  private static final Logger log = LoggerFactory.getLogger(TimedBucket.class);
  private List<ClientConnection> timedConnections = Collections.synchronizedList(new ArrayList<>());
  private boolean expiring = false;
  private final int timerSlice;

  public TimedBucket(int timerSlice) {
    this.timerSlice = timerSlice;
  }

  public void addConnection(ClientConnection conn) {
    timedConnections.add(conn);
    // make sure we don't try and reclose a closed connection, helps release memory
    conn.registerEjection(client -> {
      if (!expiring) {
        timedConnections.remove(client);
      }
    });
  }

  /**
   * expire the connections as quickly as possible, allowing for clocking over quickly as well
   * but poshing the expiry list off elsewhere and replacing the current list.
   */
  public void expireConnections() {
    if (!timedConnections.isEmpty()) {
      log.debug("kickout: {}: {}", timerSlice, timedConnections.size());

      final List<ClientConnection> conns = this.timedConnections;

      this.timedConnections = new ArrayList<>();

      expiring = true;

      conns.parallelStream().forEach((c) -> {
//        if (c != null) {
          try {
            c.close();
          } catch (Exception e) {
            log.error("Failed to kick out connections", e);
          }
//        }
      });

      expiring = false;
    }
  }
}
