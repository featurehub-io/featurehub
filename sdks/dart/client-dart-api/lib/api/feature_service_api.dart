part of featurehub_client_api.api;

class FeatureServiceApi {
  final FeatureServiceApiDelegate apiDelegate;
  FeatureServiceApi(ApiClient apiClient)
      : assert(apiClient != null),
        apiDelegate = FeatureServiceApiDelegate(apiClient);

  ///
  ///
  /// Updates the feature state if allowed.
  Future<dynamic> setFeatureState(String sdkUrl, String featureKey,
      {Options options, FeatureStateUpdate featureStateUpdate}) async {
    final response = await apiDelegate.setFeatureState(sdkUrl, featureKey,
        options: options, featureStateUpdate: featureStateUpdate);

    if (response.statusCode >= 400) {
      throw ApiException(response.statusCode, await decodeBodyBytes(response));
    } else {
      return await apiDelegate.setFeatureState_decode(response);
    }
  }

  ///
  ///
  /// Updates the feature state if allowed.
}

class FeatureServiceApiDelegate {
  final ApiClient apiClient;

  FeatureServiceApiDelegate(this.apiClient) : assert(apiClient != null);

  Future<ApiResponse> setFeatureState(String sdkUrl, String featureKey,
      {Options options, FeatureStateUpdate featureStateUpdate}) async {
    Object postBody = featureStateUpdate;

    // verify required params are set
    if (sdkUrl == null) {
      throw ApiException(400, 'Missing required param: sdkUrl');
    }
    if (featureKey == null) {
      throw ApiException(400, 'Missing required param: featureKey');
    }

    // create path and map variables
    final __path = '/features/{sdkUrl}/{featureKey}'
        .replaceAll('{' + 'sdkUrl' + '}', sdkUrl.toString())
        .replaceAll('{' + 'featureKey' + '}', featureKey.toString());

    // query params
    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{}
      ..addAll(options?.headers?.cast<String, String>() ?? {});
    if (headerParams['Accept'] == null) {
      // we only want to accept this format as we can parse it
      headerParams['Accept'] = 'application/json';
    }

    final authNames = <String>[];
    final opt = options ?? Options();

    final contentTypes = ['application/json'];

    if (contentTypes.isNotEmpty && headerParams['Content-Type'] == null) {
      headerParams['Content-Type'] = contentTypes[0];
    }
    if (postBody != null) {
      postBody = LocalApiClient.serialize(postBody);
    }

    opt.headers = headerParams;
    opt.method = 'PUT';

    return await apiClient.invokeAPI(
        __path, queryParams, postBody, authNames, opt);
  }

  Future<dynamic> setFeatureState_decode(ApiResponse response) async {
    if (response.body != null) {
      return LocalApiClient.deserializeFromString(
          await decodeBodyBytes(response), 'dynamic') as dynamic;
    }

    return null;
  }
}
