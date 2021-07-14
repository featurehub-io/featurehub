import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/routes/apps_route.dart';
import 'package:open_admin_app/routes/create_user_route.dart';
import 'package:open_admin_app/routes/edit_user_route.dart';
import 'package:open_admin_app/routes/features_overview_route.dart';
import 'package:open_admin_app/routes/home_route.dart';
import 'package:open_admin_app/routes/manage_app_route.dart';
import 'package:open_admin_app/routes/manage_group_route.dart';
import 'package:open_admin_app/routes/manage_portfolios_route.dart';
import 'package:open_admin_app/routes/manage_service_accounts_route.dart';
import 'package:open_admin_app/routes/manage_users_route.dart';
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
      handlerFunc: (BuildContext context, Map<String, dynamic> params) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return builder(mrBloc, params: params);
  });
}

class RouteCreator {
  Widget loading(mrBloc, {params}) {
    return HomeRoute(title: 'Loading MotherFlagga!');
    // return LandingRoute(title: 'FeatureHub');
  }

  Widget notFound(mrBloc, {params}) {
    return HomeRoute(title: 'Link not found');
  }

  Widget root(mrBloc, {params}) {
    return HomeRoute(title: 'FeatureHub');
    // return LandingRoute(title: 'FeatureHub');
  }

  Widget login(mrBloc, {params}) {
    return const SigninWrapperWidget();
  }

  Widget setup(mrBloc, {params}) {
    return const SetupWrapperWidget();
    // return LandingRoute(title: 'FeatureHub');
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
    return const SimpleWidget(
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
            child: const CreateUserRoute(title: 'Create User')));
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
}

var routeCreator = RouteCreator();
