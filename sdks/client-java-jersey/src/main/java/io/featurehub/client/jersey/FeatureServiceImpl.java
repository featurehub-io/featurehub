package io.featurehub.client.jersey;

import cd.connect.openapi.support.ApiClient;
import cd.connect.openapi.support.Pair;
import io.featurehub.sse.api.FeatureService;
import io.featurehub.sse.model.FeatureStateUpdate;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.GenericType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureServiceImpl implements FeatureService {
  private final ApiClient apiClient;

  public FeatureServiceImpl(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public Object setFeatureState(String sdkUrl, String featureKey,
                                            FeatureStateUpdate featureStateUpdate) {
    // verify the required parameter 'sdkUrl' is set
    if (sdkUrl == null) {
      throw new BadRequestException("Missing the required parameter 'sdkUrl' when calling setFeatureState");
    }

    // verify the required parameter 'featureKey' is set
    if (featureKey == null) {
      throw new BadRequestException("Missing the required parameter 'featureKey' when calling setFeatureState");
    }

    // create path and map variables /{sdkUrl}/{featureKey}
    String localVarPath = "/{sdkUrl}/{featureKey}"
      .replaceAll("\\{" + "sdkUrl" + "\\}", sdkUrl.toString())
      .replaceAll("\\{" + "featureKey" + "\\}", featureKey.toString());

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();


    final String[] localVarAccepts = {
      "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {
      "application/json"
    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[]{};

    GenericType<Object> localVarReturnType = new GenericType<>() {};

    return apiClient.invokeAPI(localVarPath, "PUT", localVarQueryParams, featureStateUpdate, localVarHeaderParams,
      localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType).getData();
  }
}
