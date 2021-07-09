package io.featurehub.mr.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.jersey.config.JacksonObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.util.HashMap;
import java.util.Map;

public class ConstraintExceptionHandler implements ExceptionMapper<ConstraintViolationException> {
  private static final Logger log = LoggerFactory.getLogger(ConstraintExceptionHandler.class);

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    return Response.status(Response.Status.BAD_REQUEST)
      .entity(prepareMessage(exception))
      .type("application/json")
      .build();
  }

  private String prepareMessage(ConstraintViolationException exception) {
    Map<String, String> fields = new HashMap<>();
    for (ConstraintViolation<?> cv : exception.getConstraintViolations()) {
      fields.put(cv.getPropertyPath().toString(), cv.getMessage());
    }

    try {
      return JacksonObjectProvider.mapper.writeValueAsString(fields);
    } catch (JsonProcessingException e) {
      log.error("totally failed", e);
      return "{}";
    }
  }
}

