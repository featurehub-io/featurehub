package io.featurehub.db.listener;

import io.featurehub.db.api.RolloutStrategyValidator;
import io.featurehub.jersey.config.CacheJsonMapper;
import io.featurehub.mr.messaging.StreamedFeatureUpdate;
import io.featurehub.mr.model.FeatureValue;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FeatureUpdateListener implements EdgeUpdateListener, MessageHandler {
  private static final Logger log = LoggerFactory.getLogger(FeatureUpdateListener.class);
  private Dispatcher dispatcher;
  private final String subject;
  private final FeatureUpdateBySDKApi featureUpdateBySDKApi;

  public FeatureUpdateListener(String namedCache, Connection connection, FeatureUpdateBySDKApi featureUpdateBySDKApi) {
    this.featureUpdateBySDKApi = featureUpdateBySDKApi;

    dispatcher = connection.createDispatcher(this);
    subject = "/" + namedCache + "/feature-updates";

    log.info("Listening for updates on {}", subject);

    dispatcher.subscribe(subject, "feature-updates");
  }

  @Override
  public void close() {
    if (dispatcher != null) {
      dispatcher.unsubscribe(subject);
      dispatcher = null;
    }
  }


  @Override
  public void onMessage(Message msg) throws InterruptedException {

    try {
      final StreamedFeatureUpdate update = CacheJsonMapper.readFromZipBytes(msg.getData(), StreamedFeatureUpdate.class);

      processUpdate(update);
    } catch (Exception e) {
      log.error("Unable to parse incoming request for update", e);
    }

  }

  protected void processUpdate(@NotNull StreamedFeatureUpdate update) {
    try {
      log.debug("received update {}", update);
      featureUpdateBySDKApi.updateFeature(update.getApiKey(), update.getEnvironmentId(), update.getFeatureKey(), Boolean.TRUE.equals(update.getUpdatingValue()),
        (valueType) -> {
          FeatureValue fv = new FeatureValue()
            .key(update.getFeatureKey())
            .locked(update.getLock() != null && update.getLock());

          switch (valueType) {
            case BOOLEAN:
              fv.valueBoolean(update.getValueBoolean());
              break;
            case STRING:
              fv.valueString(update.getValueString());
              break;
            case NUMBER:
              fv.setValueNumber(update.getValueNumber());
              break;
            case JSON:
              fv.valueJson(update.getValueString());
              break;
          }

          return fv;
        });
    } catch (RolloutStrategyValidator.InvalidStrategyCombination ignoreEx) {
      // ignore
    } catch (Exception failed) {
      log.error("Failed to update {}", update, failed);
    }
  }
}
