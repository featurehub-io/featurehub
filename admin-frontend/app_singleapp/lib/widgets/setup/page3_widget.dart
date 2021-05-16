import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_card.dart';
import 'package:app_singleapp/widgets/setup/setup_bloc.dart';
import 'package:flutter/material.dart';

class SetupPage3Widget extends StatelessWidget {
  final SetupBloc bloc;

  const SetupPage3Widget({Key? key, required this.bloc}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
      stream: bloc.setupState,
      builder: (BuildContext context, AsyncSnapshot<bool> snap) {
        if (snap.hasError) {
          bloc.mrClient.dialogError(snap.error, null);
        } else if (snap.hasData && snap.data!) {
          return allRegistered(context);
        }
        return Container();
      },
    );
  }

  Widget allRegistered(BuildContext context) {
    return FHCardWidget(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.only(bottom: 26.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Image.asset('assets/logo/FeatureHub-icon.png',
                    width: 40, height: 40),
              ],
            ),
          ),
          Text('All set!', style: Theme.of(context).textTheme.headline6),
          Padding(
            padding: const EdgeInsets.only(top: 8.0, bottom: 8.0),
            child: Text(
                'Ok, next step is to create your first application, an environment and add some features. You can follow the progress stepper by clicking "rocket" icon on the right of the app bar.',
                style: Theme.of(context).textTheme.bodyText1),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: <Widget>[
              FHFlatButton(
                onPressed: () {
                  bloc.reinitialize();
                },
                title: 'Next',
                keepCase: false,
              )
            ],
          )
        ],
      ),
    );
  }
}
