import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/stepper/stepper_container.dart';
import 'package:flutter/widgets.dart';

class WidgetCreator {
  Widget createStepper(ManagementRepositoryClientBloc bloc) =>
      StepperContainer(mrBloc: bloc);
}

WidgetCreator widgetCreator = WidgetCreator();
