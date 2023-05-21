import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/widgets/apps/group_permissions_widget.dart';
import 'package:open_admin_app/widgets/apps/manage_app_bloc.dart';
import 'package:open_admin_app/widgets/apps/service_account_permissions_widget.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_panel_widget.dart';
import 'package:open_admin_app/widgets/common/application_drop_down.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/link_to_applications_page.dart';
import 'package:open_admin_app/widgets/environments/env_list_widget.dart';

class ManageAppRoute extends StatefulWidget {
  final bool createEnvironment;
  const ManageAppRoute(this.createEnvironment, {Key? key}) : super(key: key);

  @override
  _ManageAppRouteState createState() => _ManageAppRouteState();
}

class _ManageAppRouteState extends State<ManageAppRoute> {
  ManageAppBloc? bloc;

  @override
  void initState() {
    super.initState();

    bloc = BlocProvider.of<ManageAppBloc>(context);
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManageAppBloc>(context);
    FHAnalytics.sendScreenView("app-editing");
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const FHHeader(
          title: 'Application settings',
        ),
        const SizedBox(height: 16.0),
        StreamBuilder<List<Application>>(
            stream: bloc
                .mrClient.streamValley.currentPortfolioApplicationsStream,
            builder: (context, snapshot) {
              if (snapshot.hasData && snapshot.data!.isNotEmpty) {
                return Container(
                    padding: const EdgeInsets.only(left: 8, bottom: 8),
                    child: ApplicationDropDown(
                        applications: snapshot.data!, bloc: bloc));
              } else {
                bloc.setApplicationId(bloc.mrClient.currentAid);
                return Container(
                    padding: const EdgeInsets.only(left: 8, top: 15),
                    child: Row(
                      children: [
                        SelectableText('There are no applications in this portfolio',
                            style: Theme.of(context).textTheme.bodySmall),
                        const Padding(
                          padding: EdgeInsets.only(left: 8.0),
                          child: LinkToApplicationsPage(),
                        ),
                      ],
                    ));
              }
            }),
        const SizedBox(height: 8.0),
        const FHPageDivider(),
        StreamBuilder(
            stream: bloc.pageStateStream,
            builder: (context, envSnapshot) {
              //check we have all the initial data like Application and Environments
              if (envSnapshot.data == ManageAppPageState.initialState) {
                return const Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: <Widget>[
                    ManageAppWidget(),
                  ],
                );
              } else if (envSnapshot.data ==
                  ManageAppPageState.loadingState) {
                return Container();
              }
              return Container();
            }),
      ],
    );
  }

  @override
  void didUpdateWidget(ManageAppRoute oldWidget) {
    super.didUpdateWidget(oldWidget);
    _createEnvironmentCheck();
  }

  void _createEnvironmentCheck() {
    if (widget.createEnvironment && bloc != null) {
      WidgetsBinding.instance.addPostFrameCallback((timeStamp) {
        _createEnvironment(bloc!);
      });
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _createEnvironmentCheck();
  }

  _createEnvironment(ManageAppBloc bloc) {
    bloc.mrClient.addOverlay((BuildContext context) {
      return EnvUpdateDialogWidget(
        bloc: bloc,
      );
    });
  }
}

class ManageAppWidget extends StatefulWidget {
  const ManageAppWidget({Key? key}) : super(key: key);

  @override
  _ManageAppWidgetState createState() => _ManageAppWidgetState();
}

class _ManageAppWidgetState extends State<ManageAppWidget>
    with SingleTickerProviderStateMixin {
  StreamSubscription<RouteChange?>? _routeChange;
  TabController? _controller;
  ManagementRepositoryClientBloc? bloc;

  @override
  void initState() {
    super.initState();

    _controller = TabController(vsync: this, length: 4);
    _controller?.addListener(tabChangeListener);
  }

  void tabChangeListener() {
    // tab has changed, notify external route
    final rc = RouteChange('/app-settings');
    if (_controller?.index == 0) {
      rc.params = {
        'tab': ['environments']
      };
    } else if (_controller?.index == 1) {
      rc.params = {
        'tab': ['group-permissions']
      };
    } else if (_controller?.index == 2) {
      rc.params = {
        'tab': ['service-accounts']
      };
    } else if (_controller?.index == 3) {
      // this is gonna get awkward once we have multiple optional server flags
      rc.params = {
        'tab': ['webhooks']
      };
    }
    bloc?.notifyExternalRouteChange(rc);
  }

  @override
  void dispose() {
    super.dispose();
    _controller?.dispose();
    if (_routeChange != null) {
      _routeChange!.cancel();
      _routeChange = null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManageAppBloc>(context);
    this.bloc = bloc.mrClient;
    final ScrollController controllerTab1 = ScrollController();
    final ScrollController controllerTab2 = ScrollController();
    final ScrollController controllerTab3 = ScrollController();
    final ScrollController controllerTab4 = ScrollController(); // webhooks

    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Column(
        children: <Widget>[
          ScrollConfiguration(
            behavior: CustomScrollBehavior(),
            child: TabBar(
              indicatorSize: TabBarIndicatorSize.label,
              controller: _controller,
              labelStyle: MediaQuery.of(context).size.width > 400
                  ? const TextStyle(fontSize: 14.0, fontWeight: FontWeight.bold) : const TextStyle(fontSize: 12.0, fontWeight: FontWeight.bold),
              tabs: [
                const Tab(
                  child: Text("Environments"),
                ),
                const Tab(
                  child: Text("Group Permissions"),
                ),
                Tab(
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(50),
                    ),
                    child: const Align(
                      alignment: Alignment.center,
                      child: Text("Service Account Permissions"),
                    ),
                  ),
                ),
                if (bloc.mrClient.identityProviders.capabilityWebhooks)
                  const Tab(
                    child: Text("Webhooks")
                  )
              ],
            ),
          ),
          SizedBox(
            height: MediaQuery.of(context).size.height - 265,
            child: TabBarView(
              physics: const NeverScrollableScrollPhysics(),
              controller: _controller,
              children: [
                //Environments
                ScrollConfiguration(
                  behavior: CustomScrollBehavior(),
                  child: SingleChildScrollView(
                    physics: const ClampingScrollPhysics(),
                    controller: controllerTab1,
                    child: Column(
                      children: <Widget>[
                        Padding(
                          padding: const EdgeInsets.only(top: 12.0),
                          child: addEnvWidget(context, bloc),
                        ),
                        const EnvListWidget()
                      ],
                    ),
                  ),
                ),
                // Groups permissions
                ScrollConfiguration(
                  behavior: CustomScrollBehavior(),
                  child: SingleChildScrollView(
                    physics: const ClampingScrollPhysics(),
                    controller: controllerTab2,
                    child: const Column(
                      children: <Widget>[
                        GroupPermissionsWidget(),
                      ],
                    ),
                  ),
                ),
                // Service accounts
                ScrollConfiguration(
                  behavior: CustomScrollBehavior(),
                  child: SingleChildScrollView(
                    physics: const ClampingScrollPhysics(),
                    controller: controllerTab3,
                    child: const Column(
                      children: <Widget>[
                        ServiceAccountPermissionsWidget(),
                      ],
                    ),
                  ),
                ),
                if (bloc.mrClient.identityProviders.capabilityWebhooks)
                  ScrollConfiguration(
                    behavior: CustomScrollBehavior(),
                    child: SingleChildScrollView(
                      physics: const ClampingScrollPhysics(),
                      controller: controllerTab4,
                      child: const Column(
                        children: <Widget>[
                          WebhooksPanelWidget(),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
          )
        ],
      ),
    );
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    bloc = BlocProvider.of(context);

    if (_routeChange != null) {
      _routeChange!.cancel();
    }

    var mrClient = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    _routeChange = mrClient
        .routeChangedStream
        .listen((routeChange) {
      if (routeChange?.route == '/app-settings') {
        switch (routeChange!.params['tab']![0]) {
          case 'environments':
            _controller!.animateTo(0);
            break;
          case 'group-permissions':
            _controller!.animateTo(1);
            break;
          case 'service-accounts':
            _controller!.animateTo(2);
            break;
          case 'webhooks':
            if (mrClient.identityProviders.capabilityWebhooks) {
              _controller!.animateTo(3);
            } else {
              _controller!.animateTo(0);
            }
            break;
          default:
            _controller!.animateTo(0);
            break;
        }
      }
    });
  }
}
