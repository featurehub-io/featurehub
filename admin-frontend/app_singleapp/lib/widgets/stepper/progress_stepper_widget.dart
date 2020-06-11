import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/widgets/common/fh_circle_icon_button.dart';
import 'package:app_singleapp/widgets/stepper/FHStepper.dart';
import 'package:app_singleapp/widgets/stepper/custom_stepper.dart';
import 'package:app_singleapp/widgets/stepper/progress_stepper_bloc.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import '../common/fh_flat_button_transparent.dart';

class FHSetupProgressStepper extends StatefulWidget {
  @override
  _StepperState createState() => _StepperState();
}

class _StepperState extends State<FHSetupProgressStepper> {
  var _index = 0;

  @override
  Widget build(BuildContext context) {
    var bloc = BlocProvider.of<StepperBloc>(context);
    final captionStyle = Theme.of(context).textTheme.caption;
    final cardWidgetTextPart =
        Column(crossAxisAlignment: CrossAxisAlignment.start, children: <Widget>[
      SizedBox(height: 16),
      Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            'Application setup progress',
          ),
          CircleIconButton(
              icon: Icon(
                Icons.close,
                size: 16.0,
              ),
              onTap: () => bloc.mrClient.stepperOpened = false)
        ],
      ),
      Divider(),
    ]);

    return Card(
      elevation: 3.0,
      child: Column(children: <Widget>[
        Container(
            padding:
                const EdgeInsets.only(bottom: 16.0, right: 16.0, left: 16.0),
            color: Theme.of(context).cardColor,
            child: cardWidgetTextPart),
        StreamBuilder<FHStepper>(
            stream: bloc.stepper,
            builder: (context, snapshot) {
              if (snapshot.hasData) {
                return CustomStepper(
                    steps: [
                      CustomStep(
                          title: Text('Create application'),
                          state: snapshot.data.application == true
                              ? CustomStepState.complete
                              : CustomStepState.indexed,
                          content: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              ApplicationDropDown(bloc),
                              FHFlatButtonTransparent(
                                title: 'Create application',
                                keepCase: true,
                                onPressed: () => {
                                  ManagementRepositoryClientBloc.router
                                      .navigateTo(context, '/manage-app',
                                          replace: true,
                                          transition: TransitionType.material)
                                },
                              ),
                            ],
                          )),
                      CustomStep(
                          title: Text('Create team group'),
//                            isActive: _index == 1,
                          state: snapshot.data.group == true
                              ? CustomStepState.complete
                              : CustomStepState.indexed,
                          content: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Text(
                                'Although groups are portofolio-wide, we recommend creating application specific groups eg “MyApp developers”',
                                style: captionStyle,
                              ),
                              FHFlatButtonTransparent(
                                title: 'Create group',
                                keepCase: true,
                                onPressed: () => {
                                  ManagementRepositoryClientBloc.router
                                      .navigateTo(context, '/manage-group',
                                          replace: true,
                                          transition: TransitionType.material)
                                },
                              ),
                            ],
                          )),
                      CustomStep(
                          title: Text('Create service account'),
                          state: snapshot.data.serviceAccount == true
                              ? CustomStepState.complete
                              : CustomStepState.indexed,
                          content: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Text(
                                'Service accounts are portfolio-wide, we recommend creating service accounts specific to an application, e.g. “SA-MyApp”',
                                style: captionStyle,
                              ),
                              FHFlatButtonTransparent(
                                title: 'Create service account',
                                keepCase: true,
                                onPressed: () => {
                                  ManagementRepositoryClientBloc.router
                                      .navigateTo(
                                          context, '/manage-service-accounts',
                                          replace: true,
                                          transition: TransitionType.material)
                                },
                              ),
                            ],
                          )),
                      CustomStep(
                          title: Text('Create environment'),
                          state: snapshot.data.application
                              ? (snapshot.data.environment == true
                                  ? CustomStepState.complete
                                  : CustomStepState.indexed)
                              : CustomStepState.disabled,
                          content: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Text(
                                'Create an environment for selected application, e.g. "test", "dev", "prod"',
                                style: captionStyle,
                              ),
                              FHFlatButtonTransparent(
                                title: 'Create environment',
                                keepCase: true,
                                onPressed: () => {
                                  ManagementRepositoryClientBloc.router
                                      .navigateTo(context, '/manage-app',
                                          replace: true,
                                          params: {
                                            'id': [bloc.applicationId],
                                            'tab-name': ['environments']
                                          },
                                          transition: TransitionType.material)
                                },
                              ),
                            ],
                          )),
                      CustomStep(
                          title: Text('Give access to groups'),
                          state: snapshot.data.environment
                              ? (snapshot.data.groupPermission == true
                                  ? CustomStepState.complete
                                  : CustomStepState.indexed)
                              : CustomStepState.disabled,
                          content: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Text(
                                'Assign an application environment level permissions to a group of users',
                                style: captionStyle,
                              ),
                              FHFlatButtonTransparent(
                                title: 'Set permissions',
                                keepCase: true,
                                onPressed: () => {
                                  ManagementRepositoryClientBloc.router
                                      .navigateTo(context, '/manage-app',
                                          params: {
                                            'id': [bloc.applicationId],
                                            'tab-name': ['group-permissions']
                                          },
                                          replace: true,
                                          transition: TransitionType.material)
                                },
                              ),
                            ],
                          )),
                      CustomStep(
                          title: Text(' Give access to service\n account'),
                          state: snapshot.data.environment &&
                                  snapshot.data.serviceAccount
                              ? (snapshot.data.serviceAccountPermission == true
                                  ? CustomStepState.complete
                                  : CustomStepState.indexed)
                              : CustomStepState.disabled,
                          content: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Text(
                                'Assign an application environment level permissions to a service account',
                                style: captionStyle,
                              ),
                              FHFlatButtonTransparent(
                                title: 'Set service account permissions',
                                keepCase: true,
                                onPressed: () => {
                                  ManagementRepositoryClientBloc.router
                                      .navigateTo(context, '/manage-app',
                                          replace: true,
                                          params: {
                                            'id': [bloc.applicationId],
                                            'tab-name': ['service-accounts']
                                          },
                                          transition: TransitionType.material)
                                },
                              ),
                            ],
                          )),
                      CustomStep(
                          title: Text('Create a feature'),
                          state: snapshot.data.application
                              ? (snapshot.data.feature == true
                                  ? CustomStepState.complete
                                  : CustomStepState.indexed)
                              : CustomStepState.disabled,
                          content: Column(
                            children: <Widget>[
                              Text(
                                'Create a feature for an application',
                                style: captionStyle,
                              ),
                              FHFlatButtonTransparent(
                                title: 'Create feature',
                                keepCase: true,
                                onPressed: () => {
                                  ManagementRepositoryClientBloc.router
                                      .navigateTo(context, '/feature-status',
                                          replace: true,
                                          transition: TransitionType.material)
                                },
                              ),
                            ],
                          )),
                    ],
                    controlsBuilder: (BuildContext context,
                            {VoidCallback onStepContinue,
                            VoidCallback onStepCancel}) =>
                        Container(),
                    currentStep: _index,
                    onStepTapped: (index) {
                      setState(() {
                        _index = index;
                      });
                    });
              } else {
                return Container();
              }
            })
      ]),
    );
  }

  Widget ApplicationDropDown(StepperBloc bloc) {
    return StreamBuilder<List<Application>>(
        stream: bloc.appsList,
        builder: (context, snapshot) {
          if (snapshot.hasData && snapshot.data.isNotEmpty) {
            return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Container(
                      child: Text('Select application or create a new one',
                          style: Theme.of(context).textTheme.caption)),
                  Container(child: applicationsDropdown(snapshot.data, bloc))
                ]);
          }
          return Container();
        });
  }

  Widget applicationsDropdown(
      List<Application> applications, StepperBloc bloc) {
    return Flexible(
      fit: FlexFit.loose,
      child: DropdownButton(
        isExpanded: true,
        style: Theme.of(context).textTheme.bodyText1,
        items: applications.map((Application application) {
          return DropdownMenuItem<String>(
              value: application.id,
              child: Text(application.name,
                  style: Theme.of(context).textTheme.bodyText2));
        }).toList(),
        hint: Text('Select application',
            style: Theme.of(context).textTheme.subtitle2),
        onChanged: (value) {
          setState(() {
            bloc.mrClient.setCurrentAid(value);
          });
        },
        value: bloc.applicationId,
      ),
    );
  }
}
