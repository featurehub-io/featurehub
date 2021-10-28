package io.featurehub.jersey.config;

import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

// from: https://stackoverflow.com/questions/32525699/listing-all-deployed-rest-endpoints-spring-boot-jersey

public class EndpointLoggingListener implements ApplicationEventListener {
  private static final Logger log = LoggerFactory.getLogger(EndpointLoggingListener.class);
  private static final TypeResolver TYPE_RESOLVER = new TypeResolver();

  private final String applicationPath;

  private boolean withOptions = false;
  private boolean withWadl = false;

  public EndpointLoggingListener() {
    this.applicationPath = "/";
  }

  @Override
  public void onEvent(ApplicationEvent event) {
    if (log.isDebugEnabled()) {
      if (event.getType() == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
        final ResourceModel resourceModel = event.getResourceModel();
        final ResourceLogDetails logDetails = new ResourceLogDetails();
        resourceModel.getResources().stream().forEach((resource) -> {
          logDetails.addEndpointLogLines(getLinesFromResource(resource));
        });
        logDetails.log();
      } else if (event.getType() == ApplicationEvent.Type.INITIALIZATION_FINISHED) {
        if (!ApplicationLifecycleManager.isReady()) {
          ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);
        }
      } else if (event.getType() == ApplicationEvent.Type.DESTROY_FINISHED) {
        if (ApplicationLifecycleManager.getStatus() != LifecycleStatus.TERMINATED) {
          ApplicationLifecycleManager.updateStatus(LifecycleStatus.TERMINATED);
        }
      }
    }
  }

  @Override
  public RequestEventListener onRequest(RequestEvent requestEvent) {
    return null;
  }

  public EndpointLoggingListener withOptions() {
    this.withOptions = true;
    return this;
  }

  public EndpointLoggingListener withWadl() {
    this.withWadl = true;
    return this;
  }

  private Set<EndpointLogLine> getLinesFromResource(Resource resource) {
    Set<EndpointLogLine> logLines = new HashSet<>();
    populate(this.applicationPath, false, resource, logLines);
    return logLines;
  }

  private void populate(String basePath, Class<?> klass, boolean isLocator,
                        Set<EndpointLogLine> endpointLogLines) {
    populate(basePath, isLocator, Resource.from(klass), endpointLogLines);
  }

  private void populate(String basePath, boolean isLocator, Resource resource,
                        Set<EndpointLogLine> endpointLogLines) {
    if (!isLocator) {
      basePath = normalizePath(basePath, resource.getPath());
    }

    for (ResourceMethod method : resource.getResourceMethods()) {
      if (!withOptions && method.getHttpMethod().equalsIgnoreCase("OPTIONS")) {
        continue;
      }
      if (!withWadl && basePath.contains(".wadl")) {
        continue;
      }
      endpointLogLines.add(new EndpointLogLine(method.getHttpMethod(), basePath, null));
    }

    for (Resource childResource : resource.getChildResources()) {
      for (ResourceMethod method : childResource.getAllMethods()) {
        if (method.getType() == ResourceMethod.JaxrsType.RESOURCE_METHOD) {
          final String path = normalizePath(basePath, childResource.getPath());
          if (!withOptions && method.getHttpMethod().equalsIgnoreCase("OPTIONS")) {
            continue;
          }
          if (!withWadl && path.contains(".wadl")) {
            continue;
          }
          endpointLogLines.add(new EndpointLogLine(method.getHttpMethod(), path, null));
        } else if (method.getType() == ResourceMethod.JaxrsType.SUB_RESOURCE_LOCATOR) {
          final String path = normalizePath(basePath, childResource.getPath());
          final ResolvedType responseType = TYPE_RESOLVER
            .resolve(method.getInvocable().getResponseType());
          final Class<?> erasedType = !responseType.getTypeBindings().isEmpty()
            ? responseType.getTypeBindings().getBoundType(0).getErasedType()
            : responseType.getErasedType();
          populate(path, erasedType, true, endpointLogLines);
        }
      }
    }
  }

  private static String normalizePath(String basePath, String path) {
    if (path == null) {
      return basePath;
    }
    if (basePath.endsWith("/")) {
      return path.startsWith("/") ? basePath + path.substring(1) : basePath + path;
    }
    return path.startsWith("/") ? basePath + path : basePath + "/" + path;
  }

  private static class ResourceLogDetails {

    private static final Logger logger = LoggerFactory.getLogger(ResourceLogDetails.class);

    private static final Comparator<EndpointLogLine> COMPARATOR
      = Comparator.comparing((EndpointLogLine e) -> e.path)
      .thenComparing((EndpointLogLine e) -> e.httpMethod);

    private final Set<EndpointLogLine> logLines = new TreeSet<>(COMPARATOR);

    private void log() {
      StringBuilder sb = new StringBuilder("\nAll endpoints for Jersey application\n");
      logLines.stream().forEach((line) -> {
        sb.append(line).append("\n");
      });
      logger.info(sb.toString());
    }

    private void addEndpointLogLines(Set<EndpointLogLine> logLines) {
      this.logLines.addAll(logLines);
    }
  }

  private static class EndpointLogLine {

    private static final String DEFAULT_FORMAT = "   %-7s %s";
    final String httpMethod;
    final String path;
    final String format;

    private EndpointLogLine(String httpMethod, String path, String format) {
      this.httpMethod = httpMethod;
      this.path = path;
      this.format = format == null ? DEFAULT_FORMAT : format;
    }

    @Override
    public String toString() {
      return String.format(format, httpMethod, path);
    }
  }
}
