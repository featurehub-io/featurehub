import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/routes/apps_route.dart';
import 'package:app_singleapp/routes/create_user_route.dart';
import 'package:app_singleapp/routes/edit_user_route.dart';
import 'package:app_singleapp/routes/features_overview_route.dart';
import 'package:app_singleapp/routes/landing_route.dart';
import 'package:app_singleapp/routes/manage_app_route.dart';
import 'package:app_singleapp/routes/manage_group_route.dart';
import 'package:app_singleapp/routes/manage_portfolios_route.dart';
import 'package:app_singleapp/routes/manage_service_accounts_route.dart';
import 'package:app_singleapp/routes/manage_users_route.dart';
import 'package:app_singleapp/routes/register_url_route.dart';
import 'package:app_singleapp/routes/service_env_route.dart';
import 'package:app_singleapp/widgets/apps/apps_bloc.dart';
import 'package:app_singleapp/widgets/apps/manage_app_bloc.dart';
import 'package:app_singleapp/widgets/apps/manage_service_accounts_bloc.dart';
import 'package:app_singleapp/widgets/features/per_application_features_bloc.dart';
import 'package:app_singleapp/widgets/group/group_bloc.dart';
import 'package:app_singleapp/widgets/portfolio/portfolio_bloc.dart';
import 'package:app_singleapp/widgets/service-accounts/service_accounts_env_bloc.dart';
import 'package:app_singleapp/widgets/simple_widget.dart';
import 'package:app_singleapp/widgets/user/common/select_portfolio_group_bloc.dart';
import 'package:app_singleapp/widgets/user/create/create_user_bloc.dart';
import 'package:app_singleapp/widgets/user/edit/edit_user_bloc.dart';
import 'package:app_singleapp/widgets/user/list/list_users_bloc.dart';
import 'package:app_singleapp/widgets/user/register/register_url_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

Handler handleRouteChangeRequest(builder) {
  return Handler(
      handlerFunc: (BuildContext context, Map<String, dynamic> params) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return builder(mrBloc, params: params);
  });
}

Widget root(mrBloc, {params}) {
  return LandingRoute(title: 'FeatureHub');
}

Widget portfolios(mrBloc, {params}) {
  return BlocProvider<PortfolioBloc>(
      creator: (_context, _bag) =>
          PortfolioBloc(params['search']?.elementAt(0), mrBloc),
      child: PortfolioRoute());
}

Widget users(mrBloc, {params}) {
  return BlocProvider<ListUsersBloc>(
      creator: (_context, _bag) =>
          ListUsersBloc(params['search']?.elementAt(0), mrBloc),
      child: ManageUsersRoute());
}

Widget group(mrBloc, {params}) {
  return BlocProvider<GroupBloc>(
      creator: (_context, _bag) =>
          GroupBloc(params['id']?.elementAt(0), mrBloc),
      child: ManageGroupRoute());
}

Widget forgotPassword(mrBloc, {params}) {
  return SimpleWidget(
    message: 'forgot-password, contact you system administrator.',
  );
}

Widget registerUrl(mrBloc, {params}) {
  return BlocProvider<RegisterBloc>(
      creator: (_context, _bag) =>
          RegisterBloc(mrBloc)..getDetails(params['token']?.elementAt(0)),
      child: RegisterURLRoute(params['token']?.elementAt(0)));
}

Widget createUser(mrBloc, {params}) {
  // TODO: fix this construction, bloc should not be created outside of provider
  final select = SelectPortfolioGroupBloc(mrBloc);
  return BlocProvider<SelectPortfolioGroupBloc>(
      creator: (_context, _bag) => select,
      child: BlocProvider<CreateUserBloc>(
          creator: (_context, _bag) =>
              CreateUserBloc(mrBloc, selectGroupBloc: select),
          child: CreateUserRoute(title: 'Create User')));
}

Widget manageUser(mrBloc, {params}) {
  final select = SelectPortfolioGroupBloc(mrBloc);
  return BlocProvider<SelectPortfolioGroupBloc>(
      creator: (_context, _bag) => select,
      child: BlocProvider<EditUserBloc>(
          creator: (_context, _bag) => EditUserBloc(
              mrBloc, params['id']?.elementAt(0),
              selectGroupBloc: select),
          child: EditUserRoute()));
}

Widget serviceAccount(mrBloc, {params}) {
  return BlocProvider<ManageServiceAccountsBloc>(
      creator: (_context, _bag) =>
          ManageServiceAccountsBloc(params['pid']?.elementAt(0), mrBloc),
      child: ManageServiceAccountsRoute());
}

Widget featureStatus(ManagementRepositoryClientBloc mrBloc, {params}) {
  return BlocProvider<PerApplicationFeaturesBloc>(
      creator: (_c, _b) => PerApplicationFeaturesBloc(mrBloc),
      child: Builder(builder: (context) => FeatureStatusRoute()));
}

Widget apps(mrBloc, {params}) {
  return BlocProvider<AppsBloc>(
      creator: (_context, _bag) => AppsBloc(mrBloc), child: AppsRoute());
}

Widget serviceEnvsHandler(ManagementRepositoryClientBloc mrBloc,
    {Map<String, List<String>>? params}) {
  return BlocProvider<ServiceAccountEnvBloc>(
    creator: (_c, _b) => ServiceAccountEnvBloc(mrBloc),
    child: ServiceAccountEnvRoute(),
  );
}

Widget manageApp(mrBloc, {params}) {
  return BlocProvider<ManageAppBloc>(
      creator: (_context, _bag) => ManageAppBloc(mrBloc),
      child: ManageAppRoute());
}

Widget featureValues(mrBloc, {params}) {
  return Container();
}
