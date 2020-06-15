import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/common/fh_circle_icon_button.dart';
import 'package:app_singleapp/widgets/stepper/progress_stepper_bloc.dart';
import 'package:app_singleapp/widgets/stepper/progress_stepper_widget.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';

class StepperContainer extends StatelessWidget {
  final int headerPadding;
  final ManagementRepositoryClientBloc mrBloc;

  const StepperContainer({
    Key key,
    this.headerPadding,
    this.mrBloc,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
      initialData: false,
      stream: mrBloc.stepperOpened,
      builder: (context, snapshot) {
        if (snapshot.data) {
            return SingleChildScrollView(
              child: BlocProvider(
                  creator: (_context, _bag) => StepperBloc(mrBloc),
                  child: FHSetupProgressStepper()),
            );
          } else {
        return Padding(
          padding: const EdgeInsets.only(top: 16.0, right: 16.0, left: 0),
              child: Container(
                  child: CircleIconButton(
                icon: Icon(
                  MaterialCommunityIcons.rocket,
                  size: 24.0,
                ),
                onTap: () => mrBloc.stepperOpened = true,
              )),
            );
        }
      }
    );
  }
}
