package io.featurehub.dacha;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.jersey.config.CacheJsonMapper;
import io.featurehub.mr.model.EnvironmentCacheItem;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.ServiceAccountCacheItem;
import io.featurehub.publish.ChannelConstants;
import io.featurehub.publish.ChannelNames;
import io.featurehub.publish.NATSSource;
import io.nats.client.Dispatcher;
import io.nats.client.MessageHandler;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig {
  private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);
  private final InternalCache cache;
  private final NATSSource natsServer;

  @ConfigKey("cache.name")
  public String name = ChannelConstants.DEFAULT_CACHE_NAME;
  private final List<Runnable> dispatchers = new ArrayList<>();

  @Inject
  public ServerConfig(InternalCache cache, NATSSource natsServer) {
    this.cache = cache;
    this.natsServer = natsServer;

    DeclaredConfigResolver.resolve(this);

    listenForEnvironments();
    listenForFeatureValues();
    listenForServiceAccounts();

    ApplicationLifecycleManager.registerListener(trans -> {
      if (trans.next == LifecycleStatus.TERMINATING) {
        shutdown();
      }
    });
  }

  void shutdown() {
    dispatchers.forEach(Runnable::run);
    dispatchers.clear();
  }

  private void listen(MessageHandler handler, String subject) {
    final Dispatcher dispatcher = natsServer.getConnection().createDispatcher(handler);
    final Dispatcher subscribe = dispatcher.subscribe(subject);
    dispatchers.add(() -> {
      subscribe.unsubscribe(subject);
    });
  }

  private void listenForServiceAccounts() {
    listen(message -> {
      try {
        ServiceAccountCacheItem sa = CacheJsonMapper.readFromZipBytes(message.getData(), ServiceAccountCacheItem.class);
        cache.serviceAccount(sa);
//        log.debug("cache received {}", sa);
      } catch (Exception e) {
        log.error("Unable to read message on SA channel", e);
      }
    }, ChannelNames.serviceAccountChannel(name));
  }

  private void listenForFeatureValues() {
    listen(message -> {
      try {
        FeatureValueCacheItem fv = CacheJsonMapper.readFromZipBytes(message.getData(), FeatureValueCacheItem.class);
        cache.updateFeatureValue(fv);
      } catch (Exception e) {
        log.error("Failure to decode featue value message", e);
      }
    }, ChannelNames.featureValueChannel(name));
  }

  private void listenForEnvironments() {
    listen(message -> {
      try {
        EnvironmentCacheItem e = CacheJsonMapper.readFromZipBytes(message.getData(), EnvironmentCacheItem.class);
        cache.environment(e);
//        log.debug("cache received {}", e);
      } catch (Exception ex) {
        log.error("unable to decode message on environment channel", ex);
      }
    }, ChannelNames.environmentChannel(name));
  }

  private byte[] encode(Object o) throws JsonProcessingException {
//    log.debug("encoding as as{}:{}", o.getClass().getName(), CacheJsonMapper.mapper.writeValueAsString(o));
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
