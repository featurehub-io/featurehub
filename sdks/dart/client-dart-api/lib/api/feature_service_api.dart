part of featurehub_client_api.api;

class FeatureServiceApi {
  final FeatureServiceApiDelegate apiDelegate;
  FeatureServiceApi(ApiClient apiClient)
      : apiDelegate = FeatureServiceApiDelegate(apiClient);

  ///
  ///
  /// Requests all features for this sdkurl and disconnects
  Future<List<Environment>> getFeatureStates(List<String> sdkUrl,
      {Options? options}) async {
    final response = await apiDelegate.getFeatureStates(
      sdkUrl,
      options: options,
    );

    if (![200, 400].contains(response.statusCode)) {
      throw ApiException(500,
          'Invalid response code ${response.statusCode} returned from API');
    }

    final body = response.body;
    if (response.statusCode >= 400) {
      throw ApiException(response.statusCode,
          body == null ? null : await decodeBodyBytes(body));
    }

    if (body == null) {
      throw ApiException(500, 'Received an empty body');
    }

    return await apiDelegate.getFeatureStates_decode(body);
  }

  ///
  ///
  /// Requests all features for this sdkurl and disconnects
  ///
  ///
  /// Updates the feature state if allowed.
  Future<dynamic> setFeatureState(
      String sdkUrl, String featureKey, FeatureStateUpdate featureStateUpdate,
      {Options? options}) async {
    final response = await apiDelegate.setFeatureState(
      sdkUrl,
      featureKey,
      featureStateUpdate,
      options: options,
    );

    if (![200, 201, 400, 403, 404, 412].contains(response.statusCode)) {
      throw ApiException(500,
          'Invalid response code ${response.statusCode} returned from API');
    }

    final body = response.body;
    if (response.statusCode >= 400) {
      throw ApiException(response.statusCode,
          body == null ? null : await decodeBodyBytes(body));
    }

    if (body == null) {
      throw ApiException(500, 'Received an empty body');
    }

    return await apiDelegate.setFeatureState_decode(body);
  }

  ///
  ///
  /// Updates the feature state if allowed.
}

class FeatureServiceApiDelegate {
  final ApiClient apiClient;

  FeatureServiceApiDelegate(this.apiClient);

  Future<ApiResponse> getFeatureStates(List<String> sdkUrl,
      {Options? options}) async {
    Object? postBody;

    // create path and map variables
    final __path = '/features/';

    // query params
    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{}
      ..addAll(options?.headers?.cast<String, String>() ?? {});
    if (headerParams['Accept'] == null) {
      // we only want to accept this format as we can parse it
      headerParams['Accept'] = 'application/json';
    }

    queryParams.addAll(convertParametersForCollectionFormat(
        LocalApiClient.parameterToString, 'multi', 'sdkUrl', sdkUrl));

    final authNames = <String>[];
    final opt = options ?? Options();

    final contentTypes = [];

    if (contentTypes.isNotEmpty && headerParams['Content-Type'] == null) {
      headerParams['Content-Type'] = contentTypes[0];
    }
    postBody = LocalApiClient.serialize(postBody);

    opt.headers = headerParams;
    opt.method = 'GET';

    return await apiClient.invokeAPI(
        __path, queryParams, postBody, authNames, opt);
  }

  Future<List<Environment>> getFeatureStates_decode(
      Stream<List<int>> body) async {
    return (LocalApiClient.deserializeFromString(
            await decodeBodyBytes(body), 'List<Environment>') as List)
        .map((item) => item as Environment)
        .toList();
  }

  Future<ApiResponse> setFeatureState(
      String sdkUrl, String featureKey, FeatureStateUpdate featureStateUpdate,
      {Options? options}) async {
    Object postBody = featureStateUpdate;

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
    postBody = LocalApiClient.serialize(postBody);

    opt.headers = headerParams;
    opt.method = 'PUT';

    return await apiClient.invokeAPI(
        __path, queryParams, postBody, authNames, opt);
  }

  Future<dynamic> setFeatureState_decode(Stream<List<int>> body) async {
    return LocalApiClient.deserializeFromString(
        await decodeBodyBytes(body), 'dynamic') as dynamic;
  }
}
