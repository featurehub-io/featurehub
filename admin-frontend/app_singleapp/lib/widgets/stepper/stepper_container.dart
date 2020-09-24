import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/common/stream_valley.dart';
import 'package:app_singleapp/widgets/stepper/progress_stepper_bloc.dart';
import 'package:app_singleapp/widgets/stepper/progress_stepper_widget.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';

class StepperContainer extends StatelessWidget {
  final ManagementRepositoryClientBloc mrBloc;

  const StepperContainer({
    Key key,
    this.mrBloc,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
      initialData: false,
      stream: mrBloc.stepperOpened,
      builder: (context, snapshot) {
        if (snapshot.hasData && snapshot.data == true) {
            return BlocProvider(
                creator: (_context, _bag) => StepperBloc(mrBloc),
                child: FHSetupProgressStepper());
          } else {
            return SizedBox.shrink();
          }
        });
  }
}

class StepperRocketButton extends StatelessWidget {
  final int headerPadding;
  final ManagementRepositoryClientBloc mrBloc;

  const StepperRocketButton({
    Key key,
    this.headerPadding,
    @required this.mrBloc,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<ReleasedPortfolio>(
        stream: mrBloc.personState.isCurrentPortfolioOrSuperAdmin,
        builder: (context, snapshot) {
          if (snapshot.data != null &&
              (snapshot.data.currentPortfolioOrSuperAdmin == true)) {
            return IconButton(
              tooltip: 'Open setup helper',
              icon: Icon(
                MaterialCommunityIcons.rocket,
//                color: Theme.of(context).primaryColor,
                size: 24.0,
              ),
              onPressed: () => mrBloc.stepperOpened = true,
            );
          } else {
            return SizedBox.shrink();
          }
        });
  }
}
