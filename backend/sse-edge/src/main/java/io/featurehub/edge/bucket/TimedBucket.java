package io.featurehub.edge.bucket;

import io.featurehub.edge.client.TimedBucketClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A segment of a time sequence that gathers all of the clients that connected in that time
 * sequence together. This allows them to all be kicked off at the same time as well.
 */
public class TimedBucket {
  private static final Logger log = LoggerFactory.getLogger(TimedBucket.class);
  private List<TimedBucketClientConnection> timedConnections = new ArrayList<>();
  private boolean expiring = false;

  public void addConnection(TimedBucketClientConnection conn) {
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
    final List<TimedBucketClientConnection> conns = this.timedConnections;

    this.timedConnections = new ArrayList<>();

    expiring = true;

    conns.parallelStream().forEach(TimedBucketClientConnection::close);

    expiring = false;
  }
}
