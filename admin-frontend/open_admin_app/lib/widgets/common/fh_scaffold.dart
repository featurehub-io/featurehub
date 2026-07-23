import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:logging/logging.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/maintenance/maintenance_info.dart';
import 'package:open_admin_app/maintenance/maintenance_mode_widget.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/fh_appbar.dart';
import 'package:open_admin_app/widgets/stepper/stepper_container.dart';

import 'fh_drawer.dart';
import 'fh_error.dart';

class FHScaffoldWidget extends StatefulWidget {
  final Widget body;
  final int scrollAtWidth;
  final MainAxisAlignment bodyMainAxisAlignment;

  const FHScaffoldWidget(
      {super.key,
      required this.body,
      this.scrollAtWidth = 320,
      required this.bodyMainAxisAlignment});

  @override
  State<StatefulWidget> createState() {
    return _FHScaffoldWidgetState();
  }
}

final _log = Logger("fh_scaffold");

class _FHScaffoldWidgetState extends State<FHScaffoldWidget> {
  final GlobalKey<_FHScaffoldWidgetState> scaffold =
      GlobalKey<_FHScaffoldWidgetState>();

  @override
  Widget build(BuildContext context) {
    return _InternalFHScaffoldWidgetWidgetState(
      scrollAtWidth: widget.scrollAtWidth,
      bodyMainAxisAlignment: widget.bodyMainAxisAlignment,
      child: widget.body,
    );
  }
}

class _InternalFHScaffoldWidgetWidgetState extends StatelessWidget {
  final Widget child;
  final int scrollAtWidth;
  final MainAxisAlignment bodyMainAxisAlignment;

  const _InternalFHScaffoldWidgetWidgetState(
      {required this.child,
      required this.scrollAtWidth,
      required this.bodyMainAxisAlignment});

  @override
  Widget build(BuildContext context) {
    var mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return Scaffold(
        appBar: const PreferredSize(
            preferredSize: Size(double.infinity, kToolbarHeight),
            child: FHappBar()),
        body: Stack(children: [
          _excludeFocusOnMainContent(mrBloc),
          StreamBuilder<Widget?>(
              stream: mrBloc.snackbarStream,
              builder: (BuildContext context, AsyncSnapshot<Widget?> snapshot) {
                if (snapshot.hasData) {
                  final snackBar = SnackBar(
                    backgroundColor: Colors.orange,
                    content: snapshot.data!,
                  );
                  // make async as calls another build
                  Timer(
                      const Duration(milliseconds: 1),
                      () =>
                          ScaffoldMessenger.of(context).showSnackBar(snackBar));
                  //after snackbar message shows up - need to make sure it doesn't show up again
                  mrBloc.addSnackbar(null);
                }
                return Container();
              }),
          StreamBuilder<WidgetBuilder?>(
              stream: mrBloc.overlayStream,
              builder: (BuildContext context,
                  AsyncSnapshot<WidgetBuilder?> snapshot) {
                if (snapshot.hasData) {
                  return BlocProvider.fromBloc(bloc: mrBloc, child:snapshot.data!(context));
                }
                return Container();
              }),
          StreamBuilder<FHError?>(
              stream: mrBloc.errorStream,
              builder:
                  (BuildContext context, AsyncSnapshot<FHError?> snapshot) {
                if (snapshot.hasData) {
                  return FHErrorWidget(error: snapshot.data!);
                }
                return Container();
              }),
          StreamBuilder<MaintenanceInfo?>(
              stream: mrBloc.activeMaintenanceStream,
              builder: (BuildContext context,
                  AsyncSnapshot<MaintenanceInfo?> snapshot) {
                if (!snapshot.hasData) return const SizedBox.shrink();
                final info = snapshot.data!;
                return MaintenanceModeWidget(mrBloc: mrBloc,);
              })
        ]));
  }

  // we tell the main content that its focus will be excluded
  Widget _excludeFocusOnMainContent(ManagementRepositoryClientBloc mrBloc) {
    return StreamBuilder<WidgetBuilder?>(
        stream: mrBloc.overlayStream,
        builder:
            (BuildContext context, AsyncSnapshot<WidgetBuilder?> snapshot) {
          return ExcludeFocus(
            excluding: snapshot.hasData,
            child: Column(
                mainAxisAlignment: MainAxisAlignment.start,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _mainContent(context),
                ]),
          );
        });
  }


  Widget maintenanceWidget(ManagementRepositoryClientBloc mrBloc) {
    return StreamBuilder(stream: mrBloc.pendingMaintenanceStream,
        builder: (BuildContext context, AsyncSnapshot<MaintenanceInfo?> snapshot) {
          if (snapshot.hasData) {
            final message = snapshot.data!.message!;
            return Container(
              color: Colors.yellow,
              padding: const EdgeInsets.symmetric(vertical: 2.0),
              child:
              Center(
                child: Text(
                  message,
                  style: TextStyle(
                    color: Colors.black,
                    fontSize: 16.0,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            );
          }
          return SizedBox.shrink();
        });
  }


  Widget _mainContent(BuildContext context) {
    final ScrollController controller = ScrollController();
    var mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return Expanded(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: bodyMainAxisAlignment,
        children: <Widget>[
          StreamBuilder<Person>(
              stream: mrBloc.personStream,
              builder: (BuildContext context, AsyncSnapshot<Person> snapshot) {
                if (snapshot.hasData && mrBloc.isLoggedIn) {
                  return const DrawerViewWidget();
                }
                return Container();
              }),
          Expanded(
            child: LayoutBuilder(builder: (context, constraints) {
              if (constraints.maxWidth > scrollAtWidth) {
                //parent that constraints this widget is the page width (without a menu)
                return Container(
                    height: MediaQuery.of(context).size.height - kToolbarHeight,
                    padding: const EdgeInsets.symmetric(horizontal: 16.0),
                    child: Column(children: [
                      Expanded(
                          child: ScrollConfiguration(
                        behavior: CustomScrollBehavior(),
                        child: SingleChildScrollView(
                            physics: const ClampingScrollPhysics(),
                            controller: controller,
                            child: Column(
                              children: <Widget>[maintenanceWidget(mrBloc), child],
                              // children: <Widget>[child],
                            )),
                      )),
                    ]));
              }
              return ScrollConfiguration(
                behavior: CustomScrollBehavior(),
                child: SingleChildScrollView(
                    controller: controller,
                    scrollDirection: Axis.horizontal,
                    child: Container(
                        height:
                            MediaQuery.of(context).size.height - kToolbarHeight,
                        padding: const EdgeInsets.fromLTRB(16, 0, 16, 0),
                        width: scrollAtWidth.toDouble(),
                        child: ListView(
                          shrinkWrap: true,
                          children: <Widget>[maintenanceWidget(mrBloc), child],
                        ))),
              );
            }),
          ),
          StreamBuilder<ReleasedPortfolio?>(
              stream: mrBloc.streamValley.currentPortfolioStream,
              builder: (context, snapshot) {
                if (snapshot.data != null &&
                    (snapshot.data!.currentPortfolioOrSuperAdmin == true)) {
                  return StepperContainer(
                    mrBloc: mrBloc,
                  );
                }

                return const SizedBox.shrink();
              }),
        ],
      ),
    );
  }
}
