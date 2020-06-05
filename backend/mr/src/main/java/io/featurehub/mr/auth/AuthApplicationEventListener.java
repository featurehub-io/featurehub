package io.featurehub.mr.auth;

import io.swagger.annotations.ApiOperation;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/**
 * This class intercepts all incoming method calls and checks to see if the method being invoked has
 * been declared in the OpenAPI as needing bearer authentication. If it finds that it has, then it checks the
 * header and sees if there is an auth token. If the token is there, and it is in the token repository, then
 * it sets it into the request's security context so the Resources can pick it up to determine who the user is.
 */

public class AuthApplicationEventListener implements ApplicationEventListener {
  private static final Logger log = LoggerFactory.getLogger(AuthApplicationEventListener.class);
  private final SecurityContext securityContext;
  private final AuthenticationRepository authRepo;

  @Inject
  public AuthApplicationEventListener(SecurityContext securityContext, AuthenticationRepository authRepo) {
    this.securityContext = securityContext;
    this.authRepo = authRepo;
  }

  @Override
  public void onEvent(ApplicationEvent event) {
    // ignore
  }

  @Override
  public RequestEventListener onRequest(RequestEvent requestEvent) {
    return new AuthRequestListener();
  }

  static Map<Method, AuthInfo> methodRequiresAuth = new ConcurrentHashMap<>();

  static class AuthInfo {
    boolean isAllowedDuringPasswordReset;
    boolean requiresAuth;

    public AuthInfo(ApiOperation op) {
      if (op != null) {
        isAllowedDuringPasswordReset = Arrays.asList(op.tags()).contains("AllowedDuringPasswordReset");
        requiresAuth = Arrays.stream(op.authorizations()).anyMatch(a -> "bearerAuth".equals(a.value()));
      }
    }
  }

  class AuthRequestListener implements RequestEventListener {
    @Override
    public void onEvent(RequestEvent event) {
      if (event.getType() == RequestEvent.Type.REQUEST_MATCHED) {
        authCheck(event);
      }
    }

    Response unauthorized() {
      return Response
        .status(Response.Status.UNAUTHORIZED)
        .entity("Please login.")
        .build();
    }

    Response forbidden() {
      return Response
        .status(Response.Status.FORBIDDEN)
        .entity("Access denied.")
        .build();
    }

    // WARNING: this does NOT deal with more than a single level of class with interfaces, it will not cope with
    // class inheritance
    private void authCheck(RequestEvent event) {
      AuthInfo requiresAuth = methodRequiresAuth.computeIfAbsent(getMethod(event),
        (method -> new AuthInfo(findAnnotationInInterfaces(method))));

      if (requiresAuth.requiresAuth) {
        String authHeader = getHttpAuthHeader(event);
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
          String token = authHeader.substring("bearer ".length());

          SessionToken value = authRepo.get(token);

          // if they have logged in and either this API is ok during password reset or the person doesn't require it
          if (value != null && (requiresAuth.isAllowedDuringPasswordReset || !Boolean.TRUE.equals(value.person.getPasswordRequiresReset()) )) {
            event.getContainerRequest().setSecurityContext(new AuthHolder(value));
          } else {
            abort(event, unauthorized());
          }
        } else {
          abort(event, unauthorized());
        }
      }
    }

    String getHttpAuthHeader(RequestEvent event) {
      return event.getContainerRequest().getHeaderString(HttpHeaders.AUTHORIZATION);
    }

    Method getMethod(RequestEvent event) {
      log.info("request method: {}", event.getUriInfo().getPath());
      return event.getUriInfo().getMatchedResourceMethod().getInvocable().getHandlingMethod();
    }

    void abort(RequestEvent event, Response response) {
      event.getContainerRequest().abortWith(response);
    }

  }

  private ApiOperation findAnnotationInClass(Method method, Class c) {
    Method found = null;
    ApiOperation op = null;
    try {
      found = c.getMethod(method.getName(), method.getParameterTypes());
    } catch (NoSuchMethodException e) {
      return null;
    }
    if (found != null) {
      op = found.getAnnotation(ApiOperation.class);
      if (op != null) {
        return op;
      }
    } else {
      return null;
    }

    return null;
  }

  private ApiOperation findAnnotationInInterfaces(Method method) {
    ApiOperation op = method.getAnnotation(ApiOperation.class);
    if (op != null) {
      return op;
    }

    for (Class<?> anInterface : method.getDeclaringClass().getInterfaces()) {
      op = findAnnotationInClass(method, anInterface);
      if (op != null) {
        return op;
      }
    }

    return null;
  }
}
