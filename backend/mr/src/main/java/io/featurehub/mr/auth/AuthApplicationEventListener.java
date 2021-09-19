package io.featurehub.mr.auth;

import io.featurehub.mr.api.AllowedDuringPasswordReset;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * This class intercepts all incoming method calls and checks to see if the method being invoked has
 * been declared in the OpenAPI as needing bearer authentication. If it finds that it has, then it checks the
 * header and sees if there is an auth token. If the token is there, and it is in the token repository, then
 * it sets it into the request's security context so the Resources can pick it up to determine who the user is.
 */

public class AuthApplicationEventListener implements ApplicationEventListener {
  private static final Logger log = LoggerFactory.getLogger(AuthApplicationEventListener.class);
  private final AuthenticationRepository authRepo;

  @Inject
  public AuthApplicationEventListener(AuthenticationRepository authRepo) {
    this.authRepo = authRepo;
  }

  @Override
  public void onEvent(ApplicationEvent event) {
    // ignore
  }

  @Override
  public RequestEventListener onRequest(RequestEvent requestEvent) {
    // do not return an interceptor when running as the party server otherwise it slows the features API down
    if (requestEvent.getUriInfo().getPath() != null &&
        !requestEvent.getUriInfo().getPath().startsWith("/features")
    ) return new AuthRequestListener();

    return null;
  }

  static Map<Method, AuthInfo> methodRequiresAuth = new ConcurrentHashMap<>();

  static class AuthInfo {
    boolean isAllowedDuringPasswordReset;
    boolean requiresAuth;

    public AuthInfo(Method op) {
      isAllowedDuringPasswordReset = op.isAnnotationPresent(AllowedDuringPasswordReset.class);
      requiresAuth = Arrays.stream(op.getParameterTypes()).anyMatch(p -> p == SecurityContext.class);
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

    // WARNING: this does NOT deal with more than a single level of class with interfaces, it will not cope with
    // class inheritance
    private void authCheck(RequestEvent event) {
      AuthInfo requiresAuth = methodRequiresAuth.computeIfAbsent(getMethod(event),
        (AuthInfo::new));

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
      log.trace("request method: {}", event.getUriInfo().getPath());
      return event.getUriInfo().getMatchedResourceMethod().getInvocable().getHandlingMethod();
    }

    void abort(RequestEvent event, Response response) {
      event.getContainerRequest().abortWith(response);
    }

  }
}
