package io.featurehub.edge.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.jersey.config.CacheJsonMapper;

public class SSEStatusMessage {
  private String status;

  public SSEStatusMessage() {
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public SSEStatusMessage(String status) {
    this.status = status;
  }

  public static String status(String msg) {
    try {
      return CacheJsonMapper.mapper.writeValueAsString(new SSEStatusMessage(msg));
    } catch (JsonProcessingException e) {
      return "{\"status\":\"failed encoding\"}";
    }
  }
}
