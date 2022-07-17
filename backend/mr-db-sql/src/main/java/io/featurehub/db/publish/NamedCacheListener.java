package io.featurehub.db.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.dacha.model.CacheManagementMessage;
import io.featurehub.dacha.model.CacheRequestType;
import io.featurehub.dacha.model.CacheState;
import io.featurehub.dacha.model.PublishEnvironment;
import io.featurehub.dacha.model.PublishFeatureValue;
import io.featurehub.dacha.model.PublishServiceAccount;
import io.featurehub.jersey.config.CacheJsonMapper;
import io.featurehub.metrics.MetricsCollector;
import io.featurehub.publish.ChannelNames;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

/** */
public class NamedCacheListener implements MessageHandler, CacheBroadcast {
  private static final Logger log = LoggerFactory.getLogger(NamedCacheListener.class);
  private final String name;
  private final Dispatcher dispatcher;
  private final String managementSubject;
  private final UUID id;
  private final CacheSource cacheSource;
  private final Connection connection;
  private final String environmentSubject;
  private final String serviceAccountSubject;
  private final String featureSubject;
  private final Counter environmentCounter =
      MetricsCollector.Companion.counter(
          "mr_publish_environments_bytes", "Bytes published to NATS for environment updates");
  private final Counter featureCounter =
      MetricsCollector.Companion.counter(
          "mr_publish_features_bytes", "Bytes published to NATS for feature updates.");
  private final Counter serviceAccountCounter =
      MetricsCollector.Companion.counter(
          "mr_publish_service_accounts_bytes", "Bytes published to NATS for service account updates.");

  private final Counter environmentFailureCounter =
      MetricsCollector.Companion.counter(
          "mr_publish_environments_failed", "Failed to publish to NATS for environment updates");
  private final Counter featureFailureCounter =
      MetricsCollector.Companion.counter(
          "mr_publish_features_failed", "Failed to publish to NATS for feature updates.");
  private final Counter serviceAccountFailureCounter =
      MetricsCollector.Companion.counter(
          "mr_publish_service_accounts_failed", "Failed to publish to NATS for service account updates.");

  private final Histogram environmentGram =
      MetricsCollector.Companion.histogram(
          "mr_publish_environments_histogram", "Histogram for publishing environments");
  private final Histogram featureGram =
      MetricsCollector.Companion.histogram(
          "mr_publish_features_histogram", "Histogram for publishing features");
  private final Histogram serviceAccountsGram =
      MetricsCollector.Companion.histogram(
          "mr_publish_service_accounts_histogram", "Histogram for publishing service account");

  public NamedCacheListener(String name, Connection connection, UUID id, CacheSource cacheSource) {
    this.name = name;

    dispatcher = connection.createDispatcher(this);

    managementSubject = ChannelNames.managementChannel(name);

    log.debug("listening on management subject `{}`", managementSubject);

    this.id = id;
    this.cacheSource = cacheSource;
    dispatcher.subscribe(managementSubject);
    this.connection = connection;

    environmentSubject = ChannelNames.environmentChannel(name);
    serviceAccountSubject = ChannelNames.serviceAccountChannel(name);
    featureSubject = ChannelNames.featureValueChannel(name);

    cacheSource.registerCache(name, this);
  }

  public void close() {
    dispatcher.unsubscribe(managementSubject);
  }

  @Override
  public void onMessage(Message message) {
    try {
      CacheManagementMessage cmm =
          CacheJsonMapper.mapper.readValue(message.getData(), CacheManagementMessage.class);

      log.trace("incoming message {}", cmm.toString());

      // ignore messages not directed at us or our own messages
      if (cmm.getDestId() != null && !id.equals(cmm.getDestId()) || id.equals(cmm.getId())) {
        return;
      }

      if (cmm.getRequestType() == CacheRequestType.SEEKING_COMPLETE_CACHE) {
        sayHelloToNewNamedCache();
      } else if (id.equals(cmm.getDestId())
          && cmm.getRequestType() == CacheRequestType.SEEKING_REFRESH) {
        cacheSource.publishToCache(name);
      }
    } catch (Exception e) {
      log.error("Malformed cache management message", e);
    }
  }

  private void sayHelloToNewNamedCache() {
    try {
      log.trace("responding with complete cache message to {}", managementSubject);
      connection.publish(
          managementSubject,
          CacheJsonMapper.mapper.writeValueAsBytes(
              new CacheManagementMessage()
                  .mit(1L)
                  .id(id)
                  .cacheState(CacheState.COMPLETE)
                  .requestType(CacheRequestType.CACHE_SOURCE)));
    } catch (JsonProcessingException e) {
      log.error("Unable to say hello as cannot encode message", e);
    }
  }

  @Override
  public void publishEnvironment(PublishEnvironment eci) {
    try {
      if (log.isTraceEnabled())
        log.trace("eci: {}", CacheJsonMapper.mapper.writeValueAsString(eci));

      publish(
          environmentSubject,
          CacheJsonMapper.writeAsZipBytes(eci),
          environmentCounter,
          environmentGram);
    } catch (IOException e) {
      environmentFailureCounter.inc();
      log.error("Could not encode environment update", e);
    }
  }

  @Override
  public void publishServiceAccount(PublishServiceAccount saci) {
    try {
      if (log.isTraceEnabled())
        log.trace("saci: {}", CacheJsonMapper.mapper.writeValueAsString(saci));

      publish(
          serviceAccountSubject,
          CacheJsonMapper.writeAsZipBytes(saci),
          serviceAccountCounter,
          serviceAccountsGram);
    } catch (IOException e) {
      serviceAccountFailureCounter.inc();
      log.error("Could not encode service account", e);
    }
  }

  @Override
  public void publishFeature(PublishFeatureValue feature) {
    try {
      log.trace("publishing feature {}", feature);

      publish(
          featureSubject, CacheJsonMapper.writeAsZipBytes(feature), featureCounter, featureGram);
    } catch (IOException e) {
      featureFailureCounter.inc();
      log.error("Could not encode feature");
    }
  }

  private void publish(String subject, byte[] body, Counter counter, Histogram histogram) {
    counter.inc(body.length);

    final Histogram.Timer timer = histogram.startTimer();

    try {
      connection.publish(subject, body);
    } finally {
      timer.observeDuration();
    }
  }
}
