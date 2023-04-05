import 'dart:core';

import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

import 'host.dart';

class Identity {
  String? _bearerToken;
  String? username;
  String? password;
  AuthServiceApi _authServiceApi;
  final PortfolioServiceApi portfolioServiceApi;
  final FeatureServiceApi featureServiceApi;
  final Host host;

  String? get bearerToken => _bearerToken;

  Identity(this.host, {this.username, this.password}):
    _authServiceApi = AuthServiceApi(host.apiClient),
    featureServiceApi = FeatureServiceApi(host.apiClient),
    portfolioServiceApi = PortfolioServiceApi(host.apiClient);


  Future<void> login() async {
    final token = await _authServiceApi.login(UserCredentials(email: username ?? '', password: password ?? ''));

    _bearerToken = token.accessToken;
    host.apiClient
        .setAuthentication('bearerAuth', OAuth(accessToken: _bearerToken));
  }

  Future<Portfolio?> findPortfolio(String portfolioName) async {
    PortfolioServiceApi _pService = PortfolioServiceApi(host.apiClient);

    final portfolios = await _pService.findPortfolios(
        filter: portfolioName, includeGroups: false);

    return portfolios.firstWhereOrNull(
        (p) => p.name.toUpperCase() == portfolioName.toUpperCase());
  }

  Future<Application?> findApplication(
      String portfolioId, String applicationName) async {
    ApplicationServiceApi _appService = ApplicationServiceApi(host.apiClient);

    final apps = await _appService.findApplications(portfolioId,
        filter: applicationName);

    return apps.firstWhereOrNull(
        (a) => a.name.toUpperCase() == applicationName.toUpperCase());
  }

  Future<List<Feature>> findFeatures(String applicationId) async {
    final features = await featureServiceApi.getAllFeaturesForApplication(applicationId);
    return features;
  }
}
