package io.featurehub.db.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.jersey.config.CacheJsonMapper;
import io.featurehub.mr.model.CacheManagementMessage;
import io.featurehub.mr.model.CacheRequestType;
import io.featurehub.mr.model.CacheState;
import io.featurehub.mr.model.EnvironmentCacheItem;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.ServiceAccountCacheItem;
import io.featurehub.publish.ChannelNames;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

/**
 *
 */
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
  public void onMessage(Message message) throws InterruptedException {
    try {
      CacheManagementMessage cmm = CacheJsonMapper.mapper.readValue(message.getData(), CacheManagementMessage.class);

      log.trace("incoming message {}", cmm.toString());

      // ignore messages not directed at us or our own messages
      if (cmm.getDestId() != null && !id.equals(cmm.getDestId()) || id.equals(cmm.getId())) {
        return;
      }

      if (cmm.getRequestType() == CacheRequestType.SEEKING_COMPLETE_CACHE) {
        sayHelloToNewNamedCache();
      } else if (id.equals(cmm.getDestId()) && cmm.getRequestType() == CacheRequestType.SEEKING_REFRESH) {
        cacheSource.publishToCache(name);
      }
    } catch (IOException e) {
      log.error("Malformed cache management message", e);
    }

  }

  private void sayHelloToNewNamedCache() {
    try {
      log.trace("responding with complete cache message to {}", managementSubject);
      connection.publish(managementSubject, CacheJsonMapper.mapper.writeValueAsBytes(
        new CacheManagementMessage().mit(1L).id(id).cacheState(CacheState.COMPLETE).requestType(CacheRequestType.CACHE_SOURCE)));
    } catch (JsonProcessingException e) {
      log.error("Unable to say hello as cannot encode message", e);
    }
  }

  @Override
  public void publishEnvironment(EnvironmentCacheItem eci) {
    try {
      if (log.isTraceEnabled())
        log.trace("eci: {}", CacheJsonMapper.mapper.writeValueAsString(eci));

      connection.publish(environmentSubject, CacheJsonMapper.mapper.writeValueAsBytes(eci));
//      connection.publish(environmentSubject, CacheJsonMapper.writeAsZipBytes(eci));
    } catch (IOException e) {
      log.error("Could not encode environment update", e);
    }
  }

  @Override
  public void publishServiceAccount(ServiceAccountCacheItem saci) {
    try {
      if (log.isTraceEnabled())
        log.trace("saci: {}", CacheJsonMapper.mapper.writeValueAsString(saci));
//      connection.publish(serviceAccountSubject, CacheJsonMapper.writeAsZipBytes(saci));
      connection.publish(serviceAccountSubject, CacheJsonMapper.mapper.writeValueAsBytes(saci));
    } catch (IOException e) {
      log.error("Could not encode service account", e);
    }
  }

  @Override
  public void publishFeature(FeatureValueCacheItem feature) {
    try {
      log.trace("publishing feature {}", feature);
//      connection.publish(featureSubject, CacheJsonMapper.writeAsZipBytes(feature));
      connection.publish(featureSubject, CacheJsonMapper.mapper.writeValueAsBytes(feature));
    } catch (IOException e) {
      log.error("Could not encode feature");
    }
  }
}
