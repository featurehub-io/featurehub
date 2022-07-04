package io.featurehub.dacha;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.dacha.model.PublishEnvironment;
import io.featurehub.dacha.model.PublishFeatureValue;
import io.featurehub.dacha.model.PublishServiceAccount;
import io.featurehub.dacha.resource.DachaEdgeNATSAdapter;
import io.featurehub.jersey.config.CacheJsonMapper;
import io.featurehub.metrics.MetricsCollector;
import io.featurehub.mr.model.DachaNATSRequest;
import io.featurehub.mr.model.DachaNATSResponse;
import io.featurehub.publish.ChannelConstants;
import io.featurehub.publish.ChannelNames;
import io.featurehub.publish.NATSSource;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.MessageHandler;
import io.prometheus.client.Counter;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig {
  private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);
  private final InternalCache cache;
  private final NATSSource natsServer;
  private final DachaEdgeNATSAdapter edgeNatsAdapter;

  @ConfigKey("cache.name")
  public String name = ChannelConstants.DEFAULT_CACHE_NAME;

  private final List<Runnable> shutdownSubscriptionRunners = new ArrayList<>();

  private final Counter serviceAccountCounter = MetricsCollector.Companion.counter(
    "dacha_service_account_msg_counter", "Service Account Messages received");
  private final Counter featureCounter = MetricsCollector.Companion.counter("dacha_feature_msg_counter",
    "Feature Messages received");
  private final Counter environmentsCounter = MetricsCollector.Companion.counter("dacha_environments_msg_counter",
    "Environment Messages received");
  private final Counter publishCounter = MetricsCollector.Companion.counter("dacha_publish_msg_counter",
    "Publish Messages received");

  @Inject
  public ServerConfig(
      InternalCache cache, NATSSource natsServer, DachaEdgeNATSAdapter edgeNatsAdapter) {
    this.cache = cache;
    this.natsServer = natsServer;
    this.edgeNatsAdapter = edgeNatsAdapter;

    DeclaredConfigResolver.resolve(this);

    listenForEnvironments();
    listenForFeatureValues();
    listenForServiceAccounts();
    listenForFeatureRequests();

    ApplicationLifecycleManager.registerListener(
        trans -> {
          if (trans.next == LifecycleStatus.TERMINATING) {
            shutdown();
          }
        });
  }

  void shutdown() {
    shutdownSubscriptionRunners.forEach(Runnable::run);
    shutdownSubscriptionRunners.clear();
  }

  private void listen(MessageHandler handler, String subject) {
    try {
      log.info("listening to subject {}", subject);
      final Dispatcher dispatcher = natsServer.getConnection().createDispatcher(handler);
      final Dispatcher subscribe = dispatcher.subscribe(subject);
      shutdownSubscriptionRunners.add(
          () -> subscribe.unsubscribe(subject));
    } catch (Exception e) {
      log.error("Failed to subscribe to subject {}", subject, e);
      System.exit(-1);
    }
  }

  private void listenForServiceAccounts() {
    listen(
        message -> {
          try {
            serviceAccountCounter.inc();
            PublishServiceAccount sa =
                CacheJsonMapper.readFromZipBytes(message.getData(), PublishServiceAccount.class);
            log.trace("service account received {}", sa);
            cache.updateServiceAccount(sa);
          } catch (Exception e) {
            log.error("Unable to read message on SA channel", e);
          }
        },
        ChannelNames.serviceAccountChannel(name));
  }

  private void listenForFeatureValues() {
    listen(
        message -> {
          try {
            featureCounter.inc();
            PublishFeatureValue fv =
                CacheJsonMapper.readFromZipBytes(message.getData(), PublishFeatureValue.class);
            log.trace("feature value received {}", fv);
            cache.updateFeatureValue(fv);
          } catch (Exception e) {
            log.error("Failure to decode featue value message", e);
          }
        },
        ChannelNames.featureValueChannel(name));
  }

  private void listenForEnvironments() {
    listen(
        message -> {
          try {
            environmentsCounter.inc();
            PublishEnvironment e =
                CacheJsonMapper.readFromZipBytes(message.getData(), PublishEnvironment.class);
            log.trace("environment received {}", e);
            cache.updateEnvironment(e);
          } catch (Exception ex) {
            log.error("unable to decode message on environment channel", ex);
          }
        },
        ChannelNames.environmentChannel(name));
  }

  private void listenForFeatureRequests() {
    final Connection connection = natsServer.getConnection();
    final Dispatcher dispatcher =
        connection.createDispatcher(
            (msg) -> {
              try {
                publishCounter.inc();
                DachaNATSRequest req =
                    CacheJsonMapper.readFromZipBytes(msg.getData(), DachaNATSRequest.class);
                log.trace("received NATs Edge request {}", req);
                final DachaNATSResponse response = edgeNatsAdapter.edgeRequest(req);
                log.trace("responded with NATs Edge request {}", response);
                connection.publish(msg.getReplyTo(), CacheJsonMapper.writeAsZipBytes(response));
              } catch (Exception e) {
                log.error("Unable to write response to feature request", e);
              }
            });

    final String subject = ChannelNames.cache(name, ChannelConstants.EDGE_CACHE_CHANNEL);
    final Dispatcher subscribe = dispatcher.subscribe(subject);

    shutdownSubscriptionRunners.add(
        () -> subscribe.unsubscribe(subject));
  }

  private byte[] encode(Object o) throws JsonProcessingException {
    return CacheJsonMapper.mapper.writeValueAsBytes(o);
  }

  public void publish(String subject, Object o, String errorMessage) {
    try {
      //      log.debug("publishing: {} => {} ", subject, o);
      natsServer.getConnection().publish(subject, encode(o));
    } catch (JsonProcessingException e) {
      log.error(errorMessage, e);
    }
  }
}
