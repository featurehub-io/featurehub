package io.featurehub.mr.filters;

import cd.connect.jackson.JacksonObjectProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
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

