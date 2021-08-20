package io.featurehub.db.listener;

import cd.connect.app.config.ConfigKey;
import io.featurehub.dacha.api.CacheJsonMapper;
import io.featurehub.db.api.FeatureApi;
import io.featurehub.db.api.RolloutStrategyValidator;
import io.featurehub.mr.messaging.StreamedFeatureUpdate;
import io.featurehub.mr.model.FeatureValue;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
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

    dispatcher.subscribe(subject);
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
      log.debug("received update {}", update);
      featureUpdateBySDKApi.updateFeature(update.getApiKey(), update.getEnvironmentId(), update.getFeatureKey(), Boolean.TRUE.equals(update.getUpdatingValue()),
        (valueType) -> {
          FeatureValue fv = new FeatureValue()
              .key(update.getFeatureKey())
              .locked(update.getLock());

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
    } catch (IOException e) {
      log.error("Unable to decompose incoming message to update feature.", e);
    } catch (RolloutStrategyValidator.InvalidStrategyCombination ignoreEx) {
      // ignore
    }
  }
}
