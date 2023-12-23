package io.featurehub.party;

import bathe.BatheInitializer;
import io.featurehub.lifecycle.ApplicationLifecycleManager;
import io.featurehub.lifecycle.LifecycleStatus;
import io.featurehub.utils.FallbackPropertyConfig;
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
    if (FallbackPropertyConfig.Companion.getConfig("dont-run-nats-server") != null) {
      log.info("dont-run-nats-server configured, so not running.");
    }

    String natsLocation = FallbackPropertyConfig.Companion.getConfig("nats.executable");
    if (natsLocation == null) {
      natsLocation = "/target/nats-server";
    }

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
          ApplicationLifecycleManager.Companion.registerListener(
              trans -> {
                if (trans.getNext() == LifecycleStatus.TERMINATING) {
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

