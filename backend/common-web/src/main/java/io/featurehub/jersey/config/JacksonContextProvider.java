package io.featurehub.jersey.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JacksonContextProvider implements ContextResolver<ObjectMapper> {
  public JacksonContextProvider() {
  }

  public ObjectMapper getContext(Class<?> aClass) {
    return JacksonObjectProvider.mapper;
  }
}
