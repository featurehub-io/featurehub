package io.featurehub.dacha;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.dacha.api.CacheAction;
import io.featurehub.dacha.model.CacheManagementMessage;
import io.featurehub.dacha.model.CacheRequestType;
import io.featurehub.dacha.model.CacheState;
import io.featurehub.health.HealthSource;
import io.featurehub.jersey.config.CacheJsonMapper;
import io.featurehub.publish.ChannelNames;
import io.featurehub.publish.NATSSource;
import io.featurehub.utils.FallbackPropertyConfig;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.opentelemetry.context.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CacheManager implements MessageHandler, HealthSource {
  private static final Logger log = LoggerFactory.getLogger(CacheManager.class);
  private Long mit;
  private Dispatcher cacheManagerDispatcher;
  private UUID id;
  @ConfigKey("cache.timeout")
  Integer timeout = 5000;
  @ConfigKey("cache.complete-timeout")
  Integer cacheCompleteTimeout = 15000;
  private CacheTimer actionTimer;
  private CacheTimer masterTimer;
  private CacheAction currentAction;
  private final InternalCache internalCache;
  private final ServerConfig config;
  private final NATSSource natsSource;
  private boolean foundMR = false;
  private UUID idOfRefreshSource = null;
  private ExecutorService executor;
  @ConfigKey("cache.pool-size")
  Integer cachePoolSize = 10;


  @Inject
  public CacheManager(InternalCache internalCache, ServerConfig config, NATSSource natsSource) {
    this.internalCache = internalCache;
    this.config = config;
    this.natsSource = natsSource;

    DeclaredConfigResolver.resolve(this);

    if (FallbackPropertyConfig.Companion.getConfig("cache.mit")  != null) {
      mit = Long.parseLong(FallbackPropertyConfig.Companion.getConfig("cache.mit"));
    } else {
      mit = (long)(Math.random() * Long.MAX_VALUE);
      if (mit == 1) {
        throw new RuntimeException("cannot be 1");
      }
    }

    id = UUID.randomUUID();

    log.info("starting cache: {}:{}", id, mit);

    executor = Context.taskWrapping(Executors.newFixedThreadPool(cachePoolSize));

    internalCache.onCompletion(this::cacheLoaded);
  }

  private void cacheLoaded() {
    actionTimer.cancel();
    if (masterTimer != null) {
      masterTimer.cancel();
    }

    setCurrentAction(CacheAction.AT_REST);
    log.info("cache ({}:{}) filled, at rest.", id, mit);
  }

  @PostConstruct
  public void init() {
    final String channelName = ChannelNames.managementChannel(config.name);

    log.info("subscribing {}:{} to channel `{}`", id, mit, channelName);

    cacheManagerDispatcher = natsSource.getConnection().createDispatcher(this);
    cacheManagerDispatcher.subscribe(channelName);

    actionTimer = new CacheTimer();

    requestCompleteCache(false);

    ApplicationLifecycleManager.registerListener(trans -> {
      if (trans.next == LifecycleStatus.TERMINATING) {
        shutdown();
      }
    });
  }

  void shutdown() {
    final String channelName = ChannelNames.managementChannel(config.name);
    log.info("unsubscribing {}:{} from channel `{}`", id, mit, channelName);
    cacheManagerDispatcher.unsubscribe(channelName);
  }

  private void missedTimer() {
    log.info("timeout ({}:{}) - action: {}", id, mit, currentAction);
    switch (currentAction) {
      case WAITING_FOR_COMPLETE_SOURCE:
        requestCompleteCache(true);
        break;
      case WAITING_FOR_NEW_MASTER:
        // we received a message to say a master had requested the cache and so we will timeout for it
        requestCompleteCache(false);
        break;
      case ATTEMPTING_TO_BECOME_MASTER:
        becomeMaster();
        break;
      case AM_MASTER:
        asMasterTimeout();
        break;
    }
  }

  /**
   * called when we are master and we timeout on the request
   */
  private void asMasterTimeout() {
    if (internalCache.cacheComplete()) {
      atRest();
    } else {
      becomeMaster();
    }
  }

  /**
   * the only time this will succeed is if we didn't see another node with a higher MIT also
   * asking to become master
   */
  private void becomeMaster() {
    log.info("becomeMaster triggered");
    actionTimer.cancel();
    masterTimer.cancel();
    setCurrentAction(CacheAction.AM_MASTER);

    config.publish(ChannelNames.managementChannel(config.name),
      new CacheManagementMessage()
        .cacheState(CacheState.REQUESTED)
        .requestType(CacheRequestType.SEEKING_REFRESH)
        .mit(1L)
        .id(id)
        .destId(idOfRefreshSource)
      , "unable to initialize");

    startTimer(cacheCompleteTimeout);
  }


  private void startTimer() {
    startTimer(this.timeout);
  }

  private void startTimer(int timeout) {
    final int realTimeout = timeout + (int)(Math.random() * 50);
    log.debug("timer ({}) set for {}", id, realTimeout);
    actionTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        missedTimer();
      }
    }, realTimeout);
  }

  private void atRest() {
    log.info("Cache is full, at rest and serving traffic.");

    actionTimer.cancel();
    if (masterTimer != null) {
      masterTimer.cancel();
    }
    setCurrentAction(CacheAction.AT_REST);
  }

  private void requestCompleteCache(boolean requestMasterStatus) {
    if (!internalCache.cacheComplete()) {
      if (foundMR && requestMasterStatus) {
        requestMaster();
      } else {
        setCurrentAction(CacheAction.WAITING_FOR_COMPLETE_SOURCE);
        config.publish(ChannelNames.managementChannel(config.name),
          new CacheManagementMessage()
            .cacheState(CacheState.NONE)
            .requestType(CacheRequestType.SEEKING_COMPLETE_CACHE)
            .mit(mit)
            .id(id)
          , "unable to initialize");

        startTimer();
      }
    } else if (currentAction != CacheAction.AT_REST) {
      atRest();
    }
  }

  public CacheAction getCurrentAction() {
    return currentAction;
  }
  private void setCurrentAction(CacheAction ca) {
    if (ca != currentAction) {
      log.debug("cache {} swapping from {} -> {}", id, currentAction, ca);
      currentAction = ca;
    } else {
      log.debug("cache {} not swapping from {} -> {}", id, currentAction, ca);
    }
  }

  /**
   * everyone should attempt to claim master if there is non master other than MR
   */
  private void requestMaster() {
      actionTimer.cancel();
      setCurrentAction(CacheAction.ATTEMPTING_TO_BECOME_MASTER);
      sendClaimingMaster();
      startTimer();
      masterTimer = new CacheTimer();
      masterTimer.schedule(new TimerTask() {
        @Override
        public void run() {
          sendClaimingMaster();
        }
      }, timeout / 2);
  }

  private void sendClaimingMaster() {
    config.publish(ChannelNames.managementChannel(config.name),
      new CacheManagementMessage()
        .cacheState(CacheState.NONE)
        .requestType(CacheRequestType.CLAIMING_MASTER)
        .mit(mit)
        .id(id)
      , "cannot send claim master");
  }

  private void waitForNewMaster() {
    // someone has requested the cache so lets cancel our timer and start another one
    actionTimer.cancel();
    setCurrentAction(CacheAction.WAITING_FOR_NEW_MASTER);
    startTimer(cacheCompleteTimeout);
  }

  private void delayAndRequestCompleteCache() {
    actionTimer.cancel();
    masterTimer.cancel();
    if (!internalCache.cacheComplete()) {
      setCurrentAction(CacheAction.WAITING_FOR_COMPLETE_SOURCE);
      startTimer(timeout * 2); // backoff a bit
    }
  }

  @Override
  public void onMessage(Message message) throws InterruptedException {
    try {
      CacheManagementMessage resp = CacheJsonMapper.readFromZipBytes(message.getData(), CacheManagementMessage.class);

      // ignore messages not directed at us or our own messages
      if (resp.getDestId() != null && !id.equals(resp.getDestId()) || id.equals(resp.getId())) {
        log.debug("directed - ignoring");
        return;
      }

      if (internalCache.cacheComplete() && currentAction == CacheAction.AM_MASTER) {
        atRest();
      }

      if (id.equals(resp.getId()) && resp.getRequestType() == CacheRequestType.DUPLICATE_MIT) {
        mit = (long)(Math.random() * Long.MAX_VALUE);
        internalCache.clear();
        requestCompleteCache(false);
      }

      if (id.equals(resp.getId()) || resp.getMit() == null) {
        return; // not interested in our own messages
      }

      log.info("received message {}:{}", mit == 1L ? "<MR>" : id, resp.toString());

      // if someone is claiming master and we are also claiming master but they have a higher mit than us,
      // drop out
      if (resp.getRequestType() == CacheRequestType.CLAIMING_MASTER) {
        if (currentAction == CacheAction.ATTEMPTING_TO_BECOME_MASTER) {
          if (resp.getMit() > mit) {
            // someone else has a higher mit, so we drop out of the race
            delayAndRequestCompleteCache();
          }
        } else if (currentAction == CacheAction.WAITING_FOR_COMPLETE_SOURCE) {
          delayAndRequestCompleteCache();
        }
      }

      if (resp.getCacheState() == CacheState.COMPLETE) {
        // we always want to take the most recent refresh source that isn't
        if (idOfRefreshSource == null || (resp.getMit() != 1L && idOfRefreshSource != null)) {
          log.info("Identified a cache source which has a complete cache. {}", idOfRefreshSource);
          idOfRefreshSource = resp.getId();
        }
        if (resp.getMit() == 1L && !foundMR) {
          log.info("We ({}:{}) are waiting for cache source and received from MR who has id {}", id, mit, resp.getId());
          foundMR = true;
        }
      }

      // we are waiting for a complete cache and someone has one or is a master requesting one
      // and they are responding to us
      if (currentAction == CacheAction.WAITING_FOR_COMPLETE_SOURCE) {
        // the _only_ thing we are doing is waiting for a complete cache source
        if (resp.getCacheState() == CacheState.COMPLETE) {
          if (1L != resp.getMit()) {
            log.info("We ({}:{}) are waiting for cache source and received from: {}:{}", id, mit, resp.getId(), resp.getMit());
            requestOtherCachePublishes(resp);
          }
        } else if (resp.getCacheState() == CacheState.REQUESTED) {
          log.info("Found master cache busy requesting data from MR: {}", resp.getId());
          waitForNewMaster();
        }
      }

      // someone is looking for a cache. either we have one or we are requesting one or neither.
      if (resp.getRequestType() == CacheRequestType.SEEKING_COMPLETE_CACHE) {
        if (internalCache.cacheComplete()) {
          respondToCompleteCacheRequest(resp);
        } else if (currentAction == CacheAction.AM_MASTER) {
          responseToCompleteCacheRequestWithMaster(resp);
        }
      }

      // is someone asking for our cache?
      if (resp.getRequestType() == CacheRequestType.SEEKING_REFRESH) {
        if (internalCache.cacheComplete() && mit.equals(resp.getMit())) {
          publishCache(resp);
        } else if (currentAction == CacheAction.ATTEMPTING_TO_BECOME_MASTER) {
          delayAndRequestCompleteCache();
        }
      }

    } catch (IOException e) {
      log.error("received unreadable message: {}", new String(message.getData(), StandardCharsets.UTF_8), e);
    }
  }

  private void responseToCompleteCacheRequestWithMaster(CacheManagementMessage resp) {
    log.info("Cache wants complete cache and we have requested MR publish one but it is not ready: {}:{}", id, mit);
    config.publish(ChannelNames.managementChannel(config.name),
      new CacheManagementMessage()
        .cacheState(CacheState.REQUESTED)
        .requestType(CacheRequestType.CACHE_SOURCE)
        .mit(resp.getMit())
        .id(id)
      , "unable to let client know we are cache complete");
  }

  private void respondToCompleteCacheRequest(CacheManagementMessage resp) {
    log.info("Cache wants complete cache and we have one: {}:{}", id, mit);
    config.publish(ChannelNames.managementChannel(config.name),
      new CacheManagementMessage()
        .cacheState(CacheState.COMPLETE)
        .requestType(CacheRequestType.CACHE_SOURCE)
        .mit(mit)
        .id(id)
      , "unable to let client know we are cache complete");
  }

  // starts causing our cache to drop onto the normal environment + serviceaccount channels
  private void publishCache(CacheManagementMessage resp) {
    log.info("We ({}:{}) are publishing cache to everyone (requested by {})", id, mit, resp.getId());
    executor.submit((Runnable) this::publishToCacheServiceAccounts);
    executor.submit(this::PublishEnvironments);
  }

  private void publishToCacheServiceAccounts() {
    internalCache.serviceAccounts().forEach(sa -> {
      config.publish(ChannelNames.serviceAccountChannel(config.name), sa, "unable to publish service account");
    });
  }

  private void PublishEnvironments() {
    internalCache.environments().forEach(env -> {
      config.publish(ChannelNames.environmentChannel(config.name), env, "unable to publish environment");
    });
  }

  private void requestOtherCachePublishes(CacheManagementMessage resp) {
    actionTimer.cancel();
    currentAction = CacheAction.WAITING_FOR_COMPLETE_CACHE;

    config.publish(ChannelNames.managementChannel(config.name),
      new CacheManagementMessage()
        .cacheState(CacheState.NONE)
        .requestType(CacheRequestType.SEEKING_REFRESH)
        .mit(resp.getMit())
        .destId(idOfRefreshSource)
        .id(id)
      , "unable to initialize");

    startTimer();
  }

  public UUID getId() {
    return id;
  }

  @Override
  public boolean getHealthy() {
    return getCurrentAction() == CacheAction.AT_REST;
  }

  @NotNull
  @Override
  public String getSourceName() {
    return "Dacha Cache (healthy = ready)";
  }
}
