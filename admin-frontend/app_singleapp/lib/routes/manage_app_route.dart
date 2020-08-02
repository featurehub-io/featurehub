import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/widgets/apps/group_permissions_widget.dart';
import 'package:app_singleapp/widgets/apps/manage_app_bloc.dart';
import 'package:app_singleapp/widgets/apps/service_account_permissions_widget.dart';
import 'package:app_singleapp/widgets/common/application_drop_down.dart';
import 'package:app_singleapp/widgets/common/decorations/fh_page_divider.dart';
import 'package:app_singleapp/widgets/common/fh_card.dart';
import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/environments/env_list_widget.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class ManageAppRoute extends StatefulWidget {
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
                  if (snapshot.hasData && snapshot.data.isNotEmpty) {
                    return Container(
                        padding: EdgeInsets.only(left: 8, bottom: 8),
                        child: ApplicationDropDown(
                            applications: snapshot.data, bloc: bloc));
                  } else {
                    bloc.setApplicationId(bloc.mrClient.currentAid);
                    return Container(
                        padding: EdgeInsets.only(left: 8, top: 15),
                        child: Text('No applications found!'));
                  }
                }),
            Container(
              padding: EdgeInsets.only(bottom: 10),
              child: FHHeader(
                title: 'Application settings',
                children: <Widget>[],
              ),
            ),
            FHPageDivider(),
            StreamBuilder(
                stream: bloc.pageStateStream,
                builder: (context, envSnapshot) {
                  //check we have all the initial data like Application and Environments
                  if (envSnapshot.data == ManageAppPageState.initialState) {
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: <Widget>[
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
  @override
  _ManageAppWidgetState createState() => _ManageAppWidgetState();
}

class _ManageAppWidgetState extends State<ManageAppWidget>
    with SingleTickerProviderStateMixin {
  StreamSubscription<RouteChange> _routeChange;
  TabController _controller;
  ManagementRepositoryClientBloc bloc;

  @override
  void initState() {
    super.initState();

    _controller = TabController(vsync: this, length: 3);
    _controller.addListener(tabChangeListener);
  }

  void tabChangeListener() {
    // tab has changed, notify external route
    final rc = RouteChange()..route = '/manage-app';
    if (_controller.index == 0) {
      rc.params = {
        'tab-name': ['environments']
      };
    } else if (_controller.index == 1) {
      rc.params = {
        'tab-name': ['group-permissions']
      };
    } else if (_controller.index == 2) {
      rc.params = {
        'tab-name': ['service-accounts']
      };
    }
    if (rc.params != null) {
      bloc.notifyExternalRouteChange(rc);
    }
  }

  @override
  void dispose() {
    super.dispose();
    _controller.dispose();
    if (_routeChange != null) {
      _routeChange.cancel();
      _routeChange = null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<ManageAppBloc>(context);

    // maybe should be a Column?
    return Column(
      children: <Widget>[
        TabBar(
          controller: _controller,
          labelStyle: Theme.of(context).textTheme.bodyText1,
          labelColor: Theme.of(context).textTheme.subtitle2.color,
          unselectedLabelColor: Theme.of(context).textTheme.bodyText2.color,
          tabs: [
            Tab(text: 'Environments'),
            Tab(text: 'Group permissions'),
            Tab(text: 'Service account permissions'),
          ],
        ),
        SizedBox(
          height: MediaQuery.of(context).size.height - 265,
          child: TabBarView(
            controller: _controller,
            children: [
              //Environments
              SingleChildScrollView(
                child: Column(
                  children: <Widget>[
                    Padding(
                      padding: const EdgeInsets.only(top: 12.0),
                      child: AddEnvWidget(context, bloc),
                    ),
                    EnvListWidget()
                  ],
                ),
              ),
              // Groups permissions
              SingleChildScrollView(
                child: Column(
                  children: <Widget>[
                    GroupPermissionsWidget(),
                  ],
                ),
              ),
              // Service accounts
              SingleChildScrollView(
                child: Column(
                  children: <Widget>[
                    ServiceAccountPermissionsWidget(),
                  ],
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
      _routeChange.cancel();
    }

    _routeChange = BlocProvider.of<ManagementRepositoryClientBloc>(context)
        .routeChangedStream
        .listen((routeChange) {
      if (routeChange.route == '/manage-app') {
        switch (routeChange.params['tab-name'][0]) {
          case 'environments':
            _controller.animateTo(0);
            break;
          case 'group-permissions':
            _controller.animateTo(1);
            break;
          case 'service-accounts':
            _controller.animateTo(2);
            break;
          default:
            _controller.animateTo(0);
            break;
        }
      }
    });
  }
}
