package io.featurehub.logging;

import bathe.BatheInitializer;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * This ensures that it gets loaded FIRST before any logging happens.
 */
public class BatheLoggingInitializer implements BatheInitializer {
  @Override
  public int getOrder() {
    return -100;
  }

  @Override
  public String getName() {
    return "logging-initializer";
  }

  @Override
  public String[] initialize(String[] args, String jumpClass) {
    SLF4JBridgeHandler.install();

    return args;
  }
}
