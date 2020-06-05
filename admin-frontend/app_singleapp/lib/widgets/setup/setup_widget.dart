import 'package:app_singleapp/widgets/setup/page1_widget.dart';
import 'package:app_singleapp/widgets/setup/page2_widget.dart';
import 'package:app_singleapp/widgets/setup/page3_widget.dart';
import 'package:app_singleapp/widgets/setup/setup_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

class SetupPageWidget extends StatelessWidget {

  @override
  Widget build(BuildContext context) {
    SetupBloc setupBloc = BlocProvider.of(context);

    return StreamBuilder<SetupPage>(
        stream: setupBloc.pageState,
        builder: (BuildContext context, AsyncSnapshot<SetupPage> snapshot) {
          Widget child;

          if (snapshot.hasError) {
            setupBloc.mrClient.dialogError(snapshot.error,null);
            child = Container();
          }
          else if (snapshot.hasData) {
            if (snapshot.data == SetupPage.page1) {
              child = SetupPage1Widget(bloc: setupBloc,);
            } else if (snapshot.data == SetupPage.page2) {
              child = SetupPage2Widget(
                bloc: setupBloc,
              );
            } else {
              child = SetupPage3Widget(bloc: setupBloc,);
            }
          } else {
            print("no data no error");
            child = Container();
          }

          return AnimatedSwitcher(duration: Duration(milliseconds: 300), child: child,);
        });
  }
}
