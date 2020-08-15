package io.featurehub.loadtest;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.featurehub.client.AnalyticsCollector;
import io.featurehub.client.FeatureRepository;
import io.featurehub.client.FeatureStateHolder;
import io.featurehub.client.FeatureValueInterceptor;
import io.featurehub.client.ReadynessListener;
import io.featurehub.client.jersey.JerseyClient;
import io.featurehub.sse.model.FeatureState;
import io.featurehub.sse.model.SSEResultState;
import org.codehaus.groovy.runtime.metaclass.ConcurrentReaderHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class FeatureVersionHolder {
  private static final Logger log = LoggerFactory.getLogger(FeatureVersionHolder.class);
  private int counter = 0;
  private long start = System.currentTimeMillis();


  synchronized int inc() {
    counter ++;
    return counter;
  }

  void finished(Long version) {
    log.info("version {} updated in {}ms", version, System.currentTimeMillis() - start);
  }

  @Override
  public String toString() {
    return "FeatureVersionHolder{" +
      "counter=" + counter +
      '}';
  }
}

class InternetFeatureTrackerRepository implements FeatureRepository {
  private static final Logger log = LoggerFactory.getLogger(InternetFeatureTrackerRepository.class);
  public static int fullDataCount = 0;
  Map<Long, FeatureVersionHolder> versionMap = new ConcurrentHashMap<>();
  static ObjectMapper mapper;
  private final int maxConnections;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  synchronized void fdInc() {
    fullDataCount ++;
  }

  InternetFeatureTrackerRepository(int maxConnections) {
    this.maxConnections = maxConnections;
  }

  @Override
  public void notify(SSEResultState state, String data) {
    if (state == SSEResultState.FEATURES) {
      if (fullDataCount < maxConnections) {
        fdInc();
        if (fullDataCount == maxConnections) {
          log.info("All servers have their data");
        }
      }
    } else if (state == SSEResultState.FEATURE) {
      final FeatureState featureState;
      try {
        featureState = mapper.readValue(data, FeatureState.class);
      } catch (JsonProcessingException e) {
        return;
      }
      versionMap.putIfAbsent(featureState.getVersion(), new FeatureVersionHolder());
      final FeatureVersionHolder fvh = versionMap.get(featureState.getVersion());
      int val = fvh.inc();
      log.debug("version: {} -> {}", featureState.getVersion(), val);
      if (val == maxConnections) {
        fvh.finished(featureState.getVersion());
      }
    }
  }

  @Override
  public void notify(List<FeatureState> states) {
  }

  @Override
  public void notify(List<FeatureState> states, boolean force) {
  }

  @Override
  public FeatureRepository addReadynessListener(ReadynessListener readynessListener) {
    return null;
  }

  @Override
  public FeatureStateHolder getFeatureState(String key) {
    return null;
  }

  @Override
  public FeatureRepository logAnalyticsEvent(String action, Map<String, String> other) {
    return null;
  }

  @Override
  public FeatureRepository addAnalyticCollector(AnalyticsCollector collector) {
    return null;
  }

  @Override
  public FeatureRepository registerValueInterceptor(boolean allowLockOverride, FeatureValueInterceptor interceptor) {
    return null;
  }
}

class ClientHolder {
  JerseyClient client;

  public ClientHolder(String url, FeatureRepository repo) {
    client = new JerseyClient(url, true, repo);
  }
}

public class LoadTest {
  private static final Logger log = LoggerFactory.getLogger(LoadTest.class);
  @ConfigKey("max-connections")
  Integer maxConnections = 500;
  @ConfigKey("ramp-up-seconds")
  Integer rampUpSeconds = 5;
  @ConfigKey("start-connections")
  Integer startConnections = 5;
  @ConfigKey("server.url")
  String url;
  private final InternetFeatureTrackerRepository repo = new InternetFeatureTrackerRepository(maxConnections);
  private List<ClientHolder> holders = new ArrayList<>();

  public LoadTest() {
    DeclaredConfigResolver.resolve(this);
  }

  public void run() throws InterruptedException {
    for(int count = 0; count < maxConnections; count ++) {
      log.info("kicking off connection {}", count);
      holders.add(new ClientHolder(url, repo));
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
    }

    // now waiting
    Thread.currentThread().join();
  }

  public static void main(String[] args) throws InterruptedException {
    new LoadTest().run();
  }
}
