package io.featurehub.loadtest;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.featurehub.client.AnalyticsCollector;
import io.featurehub.client.ClientContext;
import io.featurehub.client.ClientContextRepository;
import io.featurehub.client.Feature;
import io.featurehub.client.FeatureRepository;
import io.featurehub.client.FeatureStateHolder;
import io.featurehub.client.FeatureValueInterceptor;
import io.featurehub.client.Readyness;
import io.featurehub.client.ReadynessListener;
import io.featurehub.client.jersey.JerseyClient;
import io.featurehub.sse.model.FeatureState;
import io.featurehub.sse.model.SSEResultState;
import org.codehaus.groovy.runtime.metaclass.ConcurrentReaderHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

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
  private final ClientContext clientContext = new ClientContextRepository(new Executor() {
    @Override
    public void execute(Runnable command) {
      command.run();
    }
  });

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
  public FeatureStateHolder getFeatureState(Feature feature) {
    return null;
  }

  @Override
  public boolean getFlag(String key) {
    return false;
  }

  @Override
  public boolean getFlag(Feature feature) {
    return false;
  }

  @Override
  public String getString(String key) {
    return null;
  }

  @Override
  public String getString(Feature feature) {
    return null;
  }

  @Override
  public BigDecimal getNumber(String key) {
    return null;
  }

  @Override
  public BigDecimal getNumber(Feature feature) {
    return null;
  }

  @Override
  public <T> T getJson(String key, Class<T> type) {
    return null;
  }

  @Override
  public <T> T getJson(Feature feature, Class<T> type) {
    return null;
  }

  @Override
  public String getRawJson(String key) {
    return null;
  }

  @Override
  public String getRawJson(Feature feature) {
    return null;
  }

  @Override
  public boolean isSet(String key) {
    return false;
  }

  @Override
  public boolean isSet(Feature feature) {
    return false;
  }

  @Override
  public boolean exists(String key) {
    return false;
  }

  @Override
  public boolean exists(Feature feature) {
    return false;
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

  @Override
  public Readyness getReadyness() {
    return null;
  }

  @Override
  public void setJsonConfigObjectMapper(ObjectMapper jsonConfigObjectMapper) {

  }

  @Override
  public ClientContext clientContext() {
    return clientContext;
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
  Integer maxConnections = 10;
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
    log.info("Connecting to {}", url);

    for(int count = 0; count < maxConnections; count ++) {
      log.info("kicking off connection {}", count);
      holders.add(new ClientHolder(url, repo));
//      try {
//        Thread.sleep(100);
//      } catch (InterruptedException e) {
//      }
    }

    // now waiting
    Thread.currentThread().join();
  }

  public static void main(String[] args) throws InterruptedException {
    new LoadTest().run();
  }
}
