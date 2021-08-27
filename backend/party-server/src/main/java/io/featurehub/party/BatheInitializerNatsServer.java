package io.featurehub.party;

import bathe.BatheInitializer;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BatheInitializerNatsServer implements BatheInitializer {
  private static final Logger log = LoggerFactory.getLogger(BatheInitializerNatsServer.class);
  @Override
  public int getOrder() {
    return 100;
  }

  @Override
  public String getName() {
    return "nats-bathe";
  }

  @Override
  public String[] initialize(String[] args, String jumpClass) {
    if (System.getProperty("dont-run-nats-server", System.getenv("DONT-RUN-NATS-SERVER")) != null) {
      log.info("dont-run-nats-server configured, so not running.");
    }

    String natsLocation = System.getProperty("nats.executable") == null ? "/target/nats-server" : System.getProperty(
      "nats.executable", System.getenv("NATS.EXECUTABLE"));

    if (!new File(natsLocation).exists()) {
      log.info("No NATS server in /target, skipping.");
    } else {
      new Thread(new NatsRunner(natsLocation)).start();
    }

    return args;
  }
}

class NatsRunner implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(NatsRunner.class);
  private final String natsLocation;

  public NatsRunner(String natsLocation) {
    this.natsLocation = natsLocation;
  }

  @Override
  public void run() {
    log.info("waiting for nats to start");
    try {
      Process process = new ProcessBuilder(natsLocation)
        .redirectErrorStream(true)
        .start();

      InputStream is = process.getInputStream();
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      String line;

      while ((line = br.readLine()) != null) {
        log.debug("{}", line);
        if (line.contains("Error listening on port")) {
          log.error("Unable to start NATS server");
        } else if (line.contains("Server is ready")) {
          ApplicationLifecycleManager.registerListener(trans -> {
            if (trans.next == LifecycleStatus.TERMINATING) {
              log.info("shutting down nats-server");
              process.destroyForcibly();
            }
          });
        }
      }

      log.info("Nats Server stopped.");
    } catch (IOException e) {
      log.error("failed to run nats server", e);
    }

  }
}

