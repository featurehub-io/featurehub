import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/routes/admin_api_keys_route.dart';
import 'package:open_admin_app/routes/apps_route.dart';
import 'package:open_admin_app/routes/create_admin_api_keys_route.dart';
import 'package:open_admin_app/routes/create_user_route.dart';
import 'package:open_admin_app/routes/edit_admin_api_key_route.dart';
import 'package:open_admin_app/routes/edit_user_route.dart';
import 'package:open_admin_app/routes/features_overview_route.dart';
import 'package:open_admin_app/routes/home_route.dart';
import 'package:open_admin_app/routes/loading_route.dart';
import 'package:open_admin_app/routes/manage_app_route.dart';
import 'package:open_admin_app/routes/manage_group_route.dart';
import 'package:open_admin_app/routes/manage_portfolios_route.dart';
import 'package:open_admin_app/routes/manage_service_accounts_route.dart';
import 'package:open_admin_app/routes/manage_users_route.dart';
import 'package:open_admin_app/routes/not_found_route.dart';
import 'package:open_admin_app/routes/oauth2_fail_route.dart';
import 'package:open_admin_app/routes/register_url_route.dart';
import 'package:open_admin_app/routes/service_env_route.dart';
import 'package:open_admin_app/routes/setup_route.dart';
import 'package:open_admin_app/routes/signin_route.dart';
import 'package:open_admin_app/widgets/apps/apps_bloc.dart';
import 'package:open_admin_app/widgets/apps/manage_app_bloc.dart';
import 'package:open_admin_app/widgets/apps/manage_service_accounts_bloc.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:open_admin_app/widgets/group/group_bloc.dart';
import 'package:open_admin_app/widgets/portfolio/portfolio_bloc.dart';
import 'package:open_admin_app/widgets/service-accounts/service_accounts_env_bloc.dart';
import 'package:open_admin_app/widgets/simple_widget.dart';
import 'package:open_admin_app/widgets/user/common/select_portfolio_group_bloc.dart';
import 'package:open_admin_app/widgets/user/create/create_user_bloc.dart';
import 'package:open_admin_app/widgets/user/edit/edit_user_bloc.dart';
import 'package:open_admin_app/widgets/user/list/list_users_bloc.dart';
import 'package:open_admin_app/widgets/user/register/register_url_bloc.dart';

Handler handleRouteChangeRequest(builder) {
  return Handler(
      handlerFunc: (BuildContext context, Map<String, List<String?>> params) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return builder(mrBloc, params: params);
  });
}

class RouteCreator {
  Widget loading(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return const LoadingRoute();
  }

  Widget notFound(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return const NotFoundRoute();
  }

  Widget root(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return const HomeRoute(title: 'FeatureHub');
  }

  Widget login(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return const SigninWrapperWidget();
  }

  Widget setup(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return const SetupWrapperWidget();
    // return LandingRoute(title: 'FeatureHub');
  }

  Widget oauth2Fail(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return const Oauth2FailRoute();
  }

  Widget portfolios(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return BlocProvider<PortfolioBloc>(
        creator: (_context, _bag) =>
            PortfolioBloc(params['search']?.elementAt(0), mrBloc),
        child: const PortfolioRoute());
  }

  Widget users(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return BlocProvider<ListUsersBloc>(
        creator: (_context, _bag) =>
            ListUsersBloc(params['search']?.elementAt(0), mrBloc, true),
        child: const ManageUsersRoute());
  }

  Widget group(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return BlocProvider<GroupBloc>(
        creator: (_context, _bag) =>
            GroupBloc(params['id']?.elementAt(0), mrBloc),
        child: ManageGroupRoute(
          createGroup: _actionCreate(params),
        ));
  }

  Widget forgotPassword(mrBloc,
      {Map<String, List<String?>> params = const {}}) {
    return const SimpleWidget(
      message: 'forgot-password, contact you system administrator.',
    );
  }

  Widget registerUrl(mrBloc, {Map<String, List<String?>> params = const {}}) {
    if (params['token'] == null || params['token']![0] == null) {
      return notFound(mrBloc);
    }

    return BlocProvider<RegisterBloc>(
        creator: (_context, _bag) =>
            RegisterBloc(mrBloc)..getDetails(params['token']![0]!),
        child: RegisterURLRoute(params['token']![0]!));
  }

  Widget createUser(mrBloc, {Map<String, List<String?>> params = const {}}) {
    // TODO: fix this construction, bloc should not be created outside of provider
    final select = SelectPortfolioGroupBloc(mrBloc);
    return BlocProvider<SelectPortfolioGroupBloc>(
        creator: (_context, _bag) => select,
        child: BlocProvider<CreateUserBloc>(
            creator: (_context, _bag) =>
                CreateUserBloc(mrBloc, selectGroupBloc: select),
            child: const CreateUserRoute(title: 'Create User')));
  }

  Widget createAdminApiKey(mrBloc, {Map<String, List<String?>> params = const {}}) {
    // TODO: fix this construction, bloc should not be created outside of provider
    final select = SelectPortfolioGroupBloc(mrBloc);
    return BlocProvider<SelectPortfolioGroupBloc>(
        creator: (_context, _bag) => select,
        child: BlocProvider<CreateUserBloc>(
            creator: (_context, _bag) =>
                CreateUserBloc(mrBloc, selectGroupBloc: select),
            child: const CreateAdminApiKeyRoute(title: 'Create Admin API Key')));
  }

  Widget manageUser(mrBloc, {Map<String, List<String?>> params = const {}}) {
    final select = SelectPortfolioGroupBloc(mrBloc);
    return BlocProvider<SelectPortfolioGroupBloc>(
        creator: (_context, _bag) => select,
        child: BlocProvider<EditUserBloc>(
            creator: (_context, _bag) => EditUserBloc(
                mrBloc, params['id']?.elementAt(0),
                selectGroupBloc: select),
            child: const EditUserRoute()));
  }

  Widget editAdminApiKey(mrBloc, {Map<String, List<String?>> params = const {}}) {
    final select = SelectPortfolioGroupBloc(mrBloc);
    return BlocProvider<SelectPortfolioGroupBloc>(
        creator: (_context, _bag) => select,
        child: BlocProvider<EditUserBloc>(
            creator: (_context, _bag) => EditUserBloc(
                mrBloc, params['id']?.elementAt(0),
                selectGroupBloc: select),
            child: const EditAdminApiKeyRoute()));
  }

  Widget adminAPIKeys(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return BlocProvider<ListUsersBloc>(
        creator: (_context, _bag) =>
            ListUsersBloc(params['search']?.elementAt(0), mrBloc, false),
        child: const ManageAdminApiKeysRoute());
  }

  Widget serviceAccount(mrBloc,
      {Map<String, List<String?>> params = const {}}) {
    return BlocProvider<ManageServiceAccountsBloc>(
        creator: (_context, _bag) =>
            ManageServiceAccountsBloc(params['pid']?.elementAt(0), mrBloc),
        child: ManageServiceAccountsRoute(
            createServiceAccount: _actionCreate(params)));
  }

  Widget featureStatus(ManagementRepositoryClientBloc mrBloc,
      {Map<String, List<String?>> params = const {}}) {
    return BlocProvider<PerApplicationFeaturesBloc>(
        creator: (_c, _b) => PerApplicationFeaturesBloc(mrBloc),
        child: Builder(
            builder: (context) => FeatureStatusRoute(
                  createFeature: _actionCreate(params),
                )));
  }

  bool _actionCreate(Map<String, List<String?>> params) {
    return _paramEquals(params, 'action', 'create');
  }

  bool _paramEquals(
      Map<String, List<String?>> params, String field, String value) {
    final action = params[field];

    if (action != null) {
      return action.isNotEmpty && action[0] == value;
    }

    return false;
  }

  Widget apps(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return BlocProvider<AppsBloc>(
        creator: (_context, _bag) => AppsBloc(mrBloc),
        child: AppsRoute(createApp: _actionCreate(params)));
  }

  Widget serviceEnvsHandler(ManagementRepositoryClientBloc mrBloc,
      {Map<String, List<String>>? params}) {
    return BlocProvider<ServiceAccountEnvBloc>(
      creator: (_c, _b) => ServiceAccountEnvBloc(mrBloc),
      child: const ServiceAccountEnvRoute(),
    );
  }

  Widget manageApp(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return BlocProvider<ManageAppBloc>(
        creator: (_context, _bag) => ManageAppBloc(mrBloc),
        child: ManageAppRoute(_actionCreate(params) &&
            _paramEquals(params, 'tab', 'environments')));
  }

  Widget featureValues(mrBloc, {Map<String, List<String?>> params = const {}}) {
    return Container();
  }
}

var routeCreator = RouteCreator();
