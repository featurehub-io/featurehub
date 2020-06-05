package io.featurehub.db.utils;

import io.ebean.config.IdGenerator;

import java.util.UUID;

public class UUIDIdGenerator implements IdGenerator  {
  @Override
  public Object nextValue() {
    return UUID.randomUUID().toString();
  }

  @Override
  public String getName() {
    return "uuidStr";
  }
}
