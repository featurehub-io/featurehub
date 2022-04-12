import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/common/fh_alert_dialog.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/setup/setup_bloc.dart';

class SetupPage3Widget extends StatefulWidget {
  final SetupBloc bloc;

  const SetupPage3Widget({Key? key, required this.bloc}) : super(key: key);

  @override
  State createState() {
    return _SetupPage3WidgetState();
  }
}

class _SetupPage3WidgetState extends State<SetupPage3Widget> {
  @override
  Widget build(BuildContext context) {
    return Container();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    WidgetsBinding.instance?.addPostFrameCallback((timeStamp) {
      widget.bloc.mrClient.addOverlay(
          (context) => _FinalSetupPageOverlayWidget(bloc: widget.bloc));
    });
  }
}

class _FinalSetupPageOverlayWidget extends StatelessWidget {
  final SetupBloc bloc;

  const _FinalSetupPageOverlayWidget({Key? key, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
          stream: bloc.setupState,
          builder: (BuildContext context, AsyncSnapshot<bool> snap) {
            if (snap.hasError) {
              bloc.mrClient.dialogError(snap.error, null);
            } else if (snap.hasData && snap.data!) {
              return FHAlertDialog(
                title: title(context),
                content: Padding(
                  padding: const EdgeInsets.only(top: 8.0, bottom: 8.0),
                  child: Text(
                      'Ok, next step is to create your first application, an environment and add some features. You can follow the progress stepper by clicking the "rocket" icon on the right of the app bar.',
                      style: Theme.of(context).textTheme.bodyText1),
                ),
                  actions: <Widget>[
                    FHFlatButton(
                        title: 'Next',
                        onPressed: () {
                          bloc.mrClient.removeOverlay();
                          bloc.reinitialize();
                        })
                  ]
              );
            }
            else if (snap.connectionState == ConnectionState.waiting) {
            return const Center(child:CircularProgressIndicator());
          }
            return Container();
          },
        );
  }

  Widget title(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.only(right: 26.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Image.asset('assets/logo/FeatureHub-icon.png',
                  width: 40, height: 40),
            ],
          ),
        ),
        Text('All set!', style: Theme.of(context).textTheme.headline6),
      ],
    );
  }
}
