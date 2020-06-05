import 'package:app_singleapp/widgets/apps/app_delete_dialog_widget.dart';
import 'package:app_singleapp/widgets/apps/app_update_dialog_widget.dart';
import 'package:app_singleapp/widgets/apps/group_permissions_widget.dart';
import 'package:app_singleapp/widgets/apps/manage_app_bloc.dart';
import 'package:app_singleapp/widgets/apps/service_account_permissions_widget.dart';
import 'package:app_singleapp/widgets/common/fh_card.dart';
import 'package:app_singleapp/widgets/common/fh_header.dart';
import 'package:app_singleapp/widgets/common/fh_icon_button.dart';
import 'package:app_singleapp/widgets/common/fh_icon_text_button.dart';
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
    ManageAppBloc bloc = BlocProvider.of(context);
    return Container(
        padding: const EdgeInsets.fromLTRB(0, 8, 0, 0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Container(
              padding: EdgeInsets.only(bottom: 10),
              child: FHHeader(
                title: "Application settings",
                children: <Widget>[],
              ),
            ),
            Row(
              children: <Widget>[
                StreamBuilder<List<Application>>(
                    stream: bloc.mrClient.streamValley
                        .currentPortfolioApplicationsStream,
                    builder: (context, snapshot) {
                      if (snapshot.hasData && snapshot.data.isNotEmpty) {
                        return Container(
                            padding: EdgeInsets.only(left: 8, bottom: 8),
                            child: applicationsDropdown(
                                snapshot.data, bloc, context));
                      } else {
                        bloc.setApplicationId(bloc.mrClient.currentAid);
                        return Container(
                            padding: EdgeInsets.only(left: 8, top: 15),
                            child: Text("No applications found!"));
                      }
                    }),
                bloc.mrClient.isPortfolioOrSuperAdmin(bloc.mrClient.currentPid)
                    ? _getAdminActions(bloc)
                    : Container()
              ],
            ),
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

  Widget _getAdminActions(ManageAppBloc bloc) {
    return Container(
      padding: EdgeInsets.only(left: 10, top: 12),
      child: Row(children: <Widget>[
        Container(
            child: FHIconTextButton(
          iconData: Icons.add,
          keepCase: true,
          label: 'Create new application',
          onPressed: () => bloc.mrClient.addOverlay((BuildContext context) {
            return AppUpdateDialogWidget(
              bloc: bloc,
            );
          }),
        )),
        StreamBuilder<String>(
            stream: bloc.mrClient.streamValley.currentAppIdStream,
            builder: (context, snapshot) {
              if (snapshot.hasData) {
                return Row(children: <Widget>[
                  FHIconButton(
                      icon: Icon(Icons.edit,
                          color: Theme.of(context).buttonColor),
                      onPressed: () => bloc.mrClient.addOverlay(
                          (BuildContext context) => AppUpdateDialogWidget(
                                bloc: bloc,
                                application: bloc.application,
                              ))),
                  FHIconButton(
                      icon: Icon(Icons.delete,
                          color: Theme.of(context).buttonColor),
                      onPressed: () =>
                          bloc.mrClient.addOverlay((BuildContext context) {
                            return AppDeleteDialogWidget(
                              bloc: bloc,
                              application: bloc.application,
                            );
                          }))
                ]);
              } else {
                return Container();
              }
            })
      ]),
    );
  }

  Widget applicationsDropdown(
      List<Application> applications, ManageAppBloc bloc, context) {
    bloc.setApplicationId(bloc.mrClient.currentAid);
    return Container(
      padding: EdgeInsets.only(top: 15),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            "Portfolio application",
            style: Theme.of(context).textTheme.caption,
          ),
          DropdownButton(
            items: applications != null && applications.isNotEmpty
                ? applications.map((Application application) {
                    return DropdownMenuItem<String>(
                        value: application.id,
                        child: Text(application.name,
                            style: Theme.of(context).textTheme.bodyText2));
                  }).toList()
                : null,
            hint: Text('Select application',
                style: Theme.of(context).textTheme.subtitle2),
            onChanged: (value) {
              setState(() {
                bloc.setApplicationId(value);
                bloc.mrClient.setCurrentAid(value);
              });
            },
            value: bloc.mrClient.currentAid,
          ),
        ],
      ),
    );
  }
}

class ManageAppWidget extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    ManageAppBloc bloc = BlocProvider.of(context);
    int findTabIndex() {
      switch (bloc.tabName) {
        case "environments":
          {
            return 0;
          }
          break;

        case "group-permissions":
          {
            return 1;
          }
          break;

        case "service-accounts":
          {
            return 2;
          }
          break;

        default:
          {
            return 0;
          }
          break;
      }
    }

    return DefaultTabController(
      initialIndex: findTabIndex(),
      // The number of tabs / content sections to display.
      length: 3,
      child: Column(
        children: <Widget>[
          TabBar(
            labelStyle: Theme.of(context).textTheme.bodyText1,
            labelColor: Theme.of(context).textTheme.subtitle2.color,
            unselectedLabelColor: Theme.of(context).textTheme.bodyText2.color,
            tabs: [
              Tab(text: "Environments"),
              Tab(text: "Group permissions"),
              Tab(text: "Service account permissions"),
            ],
          ),
          SizedBox(
            height: MediaQuery.of(context).size.height - 265,
            child: TabBarView(
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
      ),
    );
  }
}
