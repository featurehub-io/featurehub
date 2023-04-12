import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/ga_id.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/config/route_names.dart';
import 'package:open_admin_app/widgets/apps/app_delete_dialog_widget.dart';
import 'package:open_admin_app/widgets/apps/app_update_dialog_widget.dart';
import 'package:open_admin_app/widgets/apps/apps_bloc.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';
import 'package:open_admin_app/widgets/common/fh_loading_error.dart';
import 'package:open_admin_app/widgets/common/fh_loading_indicator.dart';

class AppsRoute extends StatefulWidget {
  final bool createApp;

  const AppsRoute({Key? key, required this.createApp}) : super(key: key);

  @override
  _AppsRouteState createState() => _AppsRouteState();
}

class _AppsRouteState extends State<AppsRoute> {
  AppsBloc? bloc;

  @override
  void initState() {
    super.initState();
    bloc = BlocProvider.of<AppsBloc>(context);
  }

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<AppsBloc>(context);
    FHAnalytics.sendScreenView("apps-dashboard");
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const SizedBox(height: 8.0),
        Wrap(
          children: [
            const FHHeader(
              title: 'Applications',
            ),
            StreamBuilder<ReleasedPortfolio?>(
                stream: bloc.mrClient.streamValley.currentPortfolioStream,
                builder: (context, snapshot) {
                  if (snapshot.data != null &&
                      (snapshot.data!.currentPortfolioOrSuperAdmin ==
                          true)) {
                    return FilledButton.icon(
                      icon: const Icon(Icons.add),
                      label: const Text('Create new application'),
                      onPressed: () => _createApp(bloc),
                    );
                  } else {
                    return const SizedBox.shrink();
                  }
                }),
          ],
        ),
        const SizedBox(height: 8.0),
        const FHPageDivider(),
        const SizedBox(height: 8.0),
        _ApplicationsCardsList(
          bloc: bloc,
        )
      ],
    );
  }

  @override
  void didUpdateWidget(AppsRoute oldWidget) {
    super.didUpdateWidget(oldWidget);
    _createAppCheck();
  }

  void _createAppCheck() {
    if (widget.createApp && bloc != null) {
      WidgetsBinding.instance.addPostFrameCallback((timeStamp) {
        _createApp(bloc!);
      });
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _createAppCheck();
  }

  _createApp(AppsBloc bloc) {
    bloc.mrClient.addOverlay((BuildContext context) {
      return AppUpdateDialogWidget(
        bloc: bloc,
      );
    });
  }
}

class _ApplicationsCardsList extends StatelessWidget {
  final AppsBloc bloc;

  const _ApplicationsCardsList({Key? key, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<List<Application>>(
        stream: bloc.currentApplicationsStream,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const FHLoadingIndicator();
          } else if (snapshot.connectionState == ConnectionState.active ||
              snapshot.connectionState == ConnectionState.done) {
            if (snapshot.hasError) {
              return const FHLoadingError();
            } else if (snapshot.hasData) {
              return Wrap(
                direction: Axis.horizontal,
                crossAxisAlignment: WrapCrossAlignment.start,
                children: snapshot.data!
                    .map(
                        (app) => _ApplicationCard(application: app, bloc: bloc))
                    .toList(),
              );
            }
          }
          return const SizedBox.shrink();
        });
  }
}

class _ApplicationCard extends StatelessWidget {
  final Application application;
  final AppsBloc bloc;

  const _ApplicationCard(
      {Key? key, required this.application, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Card(
        elevation: 4.0,
        color: Theme.of(context).brightness == Brightness.light
            ? Theme.of(context).colorScheme.background
            : null,
        child: InkWell(
          mouseCursor: SystemMouseCursors.click,
          borderRadius: BorderRadius.circular(8.0),
          onTap: () {
            bloc.mrClient.setCurrentAid(application.id);
            ManagementRepositoryClientBloc.router.navigateTo(
              context,
              routeNameFeatureDashboard,
            );
          },
          child: SizedBox(
            width: 240,
            height: 150,
            child: Padding(
              padding: const EdgeInsets.only(left: 16.0, bottom: 16, top: 8.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Container(
                        constraints: const BoxConstraints(maxWidth: 150),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.start,
                          children: [
                            Text(application.name,
                                maxLines: 2,
                                style: Theme.of(context)
                                    .textTheme
                                    .bodyMedium!
                                    .copyWith(
                                        color: Theme.of(context).brightness ==
                                                Brightness.light
                                            ? Theme.of(context).primaryColor
                                            : null)),
                            const SizedBox(height: 4.0),
                            Text(application.description!,
                                maxLines: 2,
//                              overflow: TextOverflow.ellipsis,
                                style: Theme.of(context).textTheme.bodySmall),
                          ],
                        ),
                      ),
                      StreamBuilder<ReleasedPortfolio?>(
                          stream:
                              bloc.mrClient.streamValley.currentPortfolioStream,
                          builder: (context, snapshot) {
                            if (snapshot.data != null &&
                                (snapshot.data!.currentPortfolioOrSuperAdmin ==
                                    true)) {
                              return _PopUpAdminMenu(
                                bloc: bloc,
                                application: application,
                              );
                            } else {
                              return const SizedBox();
                            }
                          })
                    ],
                  ),
                  Column(
                    children: [
                      const SizedBox(height: 4.0),
                      _AppTotals(application: application),
                    ],
                  )
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _AppTotals extends StatelessWidget {
  const _AppTotals({
    Key? key,
    required this.application,
  }) : super(key: key);

  final Application application;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 16.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          if (application.environments.isNotEmpty)
            _NumberAndIcon(
              tooltipText: 'Environments',
              text: application.environments.length.toString(),
              icon: const Icon(AntDesign.bars,
                  size: 16.0, color: Colors.deepPurpleAccent),
            ),
          if (application.features.isNotEmpty)
            _NumberAndIcon(
              tooltipText: 'Feature flags',
              text: application.features
                  .length
                  .toString(),
              icon: const Icon(Icons.flag, size: 16.0, color: Colors.green),
            ),
        ],
      ),
    );
  }
}

class _NumberAndIcon extends StatelessWidget {
  final String tooltipText;
  final String text;
  final Icon icon;

  const _NumberAndIcon({
    Key? key,
    required this.text,
    required this.tooltipText,
    required this.icon,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      message: tooltipText,
      child: Column(
        children: [
          _NumberContainer(
            child: Text(text, style: Theme.of(context).textTheme.titleMedium),
          ),
          const SizedBox(height: 2.0),
          icon,
        ],
      ),
    );
  }
}

class _NumberContainer extends StatelessWidget {
  final Widget child;

  const _NumberContainer({Key? key, required this.child}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
        padding: const EdgeInsets.symmetric(vertical: 4.0, horizontal: 16.0),
        decoration: BoxDecoration(
          borderRadius: const BorderRadius.all(Radius.circular(12.0)),
          color: Theme.of(context).cardColor,
        ),
        child: child);
  }
}

class _PopUpAdminMenu extends StatelessWidget {
  final AppsBloc bloc;
  final Application application;

  const _PopUpAdminMenu(
      {Key? key, required this.bloc, required this.application})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton(
      splashRadius: 20,
      tooltip: 'Show more',
      icon: const Icon(
        Icons.more_vert,
        size: 22.0,
      ),
      onSelected: (value) {
        if (value == 'edit') {
          bloc.mrClient
              .addOverlay((BuildContext context) => AppUpdateDialogWidget(
                    bloc: bloc,
                    application: application,
                  ));
        }
        if (value == 'delete') {
          bloc.mrClient.addOverlay((BuildContext context) {
            return AppDeleteDialogWidget(
              bloc: bloc,
              application: application,
            );
          });
        }
        if (value == 'features') {
          bloc.mrClient.setCurrentAid(application.id);
          ManagementRepositoryClientBloc.router.navigateTo(
            context,
            routeNameFeatureDashboard,
          );
        }
      },
      itemBuilder: (BuildContext context) {
        return [
          PopupMenuItem(
              value: 'features',
              child: Text('Features',
                  style: Theme.of(context).textTheme.bodyMedium)),
          PopupMenuItem(
              value: 'edit',
              child:
                  Text('Edit', style: Theme.of(context).textTheme.bodyMedium)),
          PopupMenuItem(
              value: 'delete',
              child: Text('Delete',
                  style: Theme.of(context).textTheme.bodyMedium))
        ];
      },
    );
  }
}
