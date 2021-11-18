import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/widgets/apps/group_permissions_widget.dart';
import 'package:open_admin_app/widgets/apps/manage_app_bloc.dart';
import 'package:open_admin_app/widgets/apps/service_account_permissions_widget.dart';
import 'package:open_admin_app/widgets/common/application_drop_down.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/link_to_applications_page.dart';
import 'package:open_admin_app/widgets/environments/env_list_widget.dart';

class ManageAppRoute extends StatefulWidget {
  const ManageAppRoute({Key? key}) : super(key: key);

  @override
  _ManageAppRouteState createState() => _ManageAppRouteState();
}

class _ManageAppRouteState extends State<ManageAppRoute> {
  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManageAppBloc>(context);
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 8, 0, 0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
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
                            Text('There are no applications in this portfolio',
                                style: Theme.of(context).textTheme.caption),
                            const Padding(
                              padding: EdgeInsets.only(left: 8.0),
                              child: LinkToApplicationsPage(),
                            ),
                          ],
                        ));
                  }
                }),
            Container(
              padding: const EdgeInsets.only(bottom: 10),
              child: const FHHeader(
                title: 'Application settings',
              ),
            ),
            const FHPageDivider(),
            StreamBuilder(
                stream: bloc.pageStateStream,
                builder: (context, envSnapshot) {
                  //check we have all the initial data like Application and Environments
                  if (envSnapshot.data == ManageAppPageState.initialState) {
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: const <Widget>[
                        FHCardWidget(child: ManageAppWidget()),
                      ],
                    );
                  } else if (envSnapshot.data ==
                      ManageAppPageState.loadingState) {
                    return Container();
                  }
                  return Container();
                }),
          ],
        ));
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

    _controller = TabController(vsync: this, length: 3);
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

    return Column(
      children: <Widget>[
        ScrollConfiguration(
          behavior: CustomScrollBehavior(),
          child: TabBar(
            // indicatorPadding: const EdgeInsets.all(5),
            indicatorSize: TabBarIndicatorSize.label,
            indicator: BoxDecoration(
              shape: BoxShape.rectangle,
              border: Border.all(
                  color:
                      Theme.of(context).colorScheme.secondary.withOpacity(0.2),
                  width: 2),
              borderRadius: BorderRadius.circular(50),
              color: Theme.of(context).colorScheme.secondary.withOpacity(0.2),
            ),
            controller: _controller,
            labelStyle: Theme.of(context).textTheme.bodyText1,
            labelColor: Theme.of(context).textTheme.subtitle2!.color,
            unselectedLabelColor: Theme.of(context).colorScheme.secondary,
            overlayColor:
                MaterialStateColor.resolveWith((Set<MaterialState> states) {
              if (states.contains(MaterialState.pressed)) {
                return Theme.of(context).colorScheme.secondaryVariant;
              }
              if (states.contains(MaterialState.focused)) {
                return Theme.of(context).colorScheme.secondaryVariant;
              } else if (states.contains(MaterialState.hovered)) {
                return Theme.of(context).cardColor;
              }

              return Colors.transparent;
            }),
            tabs: [
              Tab(
                child: Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(50),
                  ),
                  child: const Align(
                    alignment: Alignment.center,
                    child: Text("ENVIRONMENTS"),
                  ),
                ),
              ),
              Tab(
                child: Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(50),
                  ),
                  child: const Align(
                    alignment: Alignment.center,
                    child: Text("GROUP PERMISSIONS"),
                  ),
                ),
              ),
              Tab(
                child: Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(50),
                  ),
                  child: const Align(
                    alignment: Alignment.center,
                    child: Text("SERVICE ACCOUNT PERMISSIONS"),
                  ),
                ),
              ),
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
                  controller: controllerTab2,
                  child: Column(
                    children: const <Widget>[
                      GroupPermissionsWidget(),
                    ],
                  ),
                ),
              ),
              // Service accounts
              ScrollConfiguration(
                behavior: CustomScrollBehavior(),
                child: SingleChildScrollView(
                  controller: controllerTab3,
                  child: Column(
                    children: const <Widget>[
                      ServiceAccountPermissionsWidget(),
                    ],
                  ),
                ),
              ),
            ],
          ),
        )
      ],
    );
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    bloc = BlocProvider.of(context);

    if (_routeChange != null) {
      _routeChange!.cancel();
    }

    _routeChange = BlocProvider.of<ManagementRepositoryClientBloc>(context)
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
          default:
            _controller!.animateTo(0);
            break;
        }
      }
    });
  }
}
