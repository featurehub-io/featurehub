package io.featurehub.client;

import java.math.BigDecimal;

abstract public class AbstractFeatureRepository implements FeatureRepository {

  @Override
  public FeatureStateHolder getFeatureState(Feature feature) {
    return this.getFeatureState(feature.name());
  }

  @Override
  public FeatureStateHolder getFeatureState(String key, ClientContext ctx) {
    return this.getFeatureState(key).withContext(ctx);
  }

  @Override
  public FeatureStateHolder getFeatureState(Feature feature, ClientContext ctx) {
    return this.getFeatureState(feature.name()).withContext(ctx);
  }

  @Override
  public boolean getFlag(Feature feature) {
    return getFlag(feature.name());
  }

  @Override
  public boolean getFlag(String key, ClientContext ctx) {
    return getFeatureState(key, ctx).getBoolean() == Boolean.TRUE;
  }

  @Override
  public boolean getFlag(Feature feature, ClientContext ctx) {
    return getFeatureState(feature.name(), ctx).getBoolean() == Boolean.TRUE;
  }

  @Override
  public boolean getFlag(String key) {
    return getFeatureState(key).getBoolean() == Boolean.TRUE;
  }

  @Override
  public String getString(Feature feature) {
    return getString(feature.name());
  }

  @Override
  public String getString(String key, ClientContext ctx) {
    return getFeatureState(key, ctx).getString();
  }

  @Override
  public String getString(Feature feature, ClientContext ctx) {
    return getFeatureState(feature, ctx).getString();
  }

  @Override
  public String getString(String key) {
    return getFeatureState(key).getString();
  }

  @Override
  public BigDecimal getNumber(String key) {
    return getFeatureState(key).getNumber();
  }

  @Override
  public BigDecimal getNumber(Feature feature) {
    return getNumber(feature.name());
  }

  @Override
  public BigDecimal getNumber(String key, ClientContext ctx) {
    return getFeatureState(key, ctx).getNumber();
  }

  @Override
  public BigDecimal getNumber(Feature feature, ClientContext ctx) {
    return getFeatureState(feature, ctx).getNumber();
  }

  @Override
  public <T> T getJson(String key, Class<T> type) {
    return getFeatureState(key).getJson(type);
  }

  @Override
  public <T> T getJson(Feature feature, Class<T> type) {
    return getJson(feature.name(), type);
  }

  @Override
  public <T> T getJson(String key, Class<T> type, ClientContext ctx) {
    return getFeatureState(key, ctx).getJson(type);
  }

  @Override
  public <T> T getJson(Feature feature, Class<T> type, ClientContext ctx) {
    return getFeatureState(feature, ctx).getJson(type);
  }

  @Override
  public String getRawJson(String key) {
    return getFeatureState(key).getRawJson();
  }

  @Override
  public String getRawJson(Feature feature) {
    return getRawJson(feature.name());
  }

  @Override
  public String getRawJson(String key, ClientContext ctx) {
    return getFeatureState(key, ctx).getRawJson();
  }

  @Override
  public String getRawJson(Feature feature, ClientContext ctx) {
    return getFeatureState(feature, ctx).getRawJson();
  }

  @Override
  public boolean isSet(String key) {
    return getFeatureState(key).isSet();
  }

  @Override
  public boolean isSet(Feature feature) {
    return isSet(feature.name());
  }

  @Override
  public boolean isSet(String key, ClientContext ctx) {
    return getFeatureState(key, ctx).isSet();
  }

  @Override
  public boolean isSet(Feature feature, ClientContext ctx) {
    return getFeatureState(feature, ctx).isSet();
  }

  @Override
  public boolean exists(String key) {
    return ((FeatureStateBaseHolder)getFeatureState(key)).exists();
  }

  @Override
  public boolean exists(Feature feature) {
    return exists(feature.name());
  }
}
