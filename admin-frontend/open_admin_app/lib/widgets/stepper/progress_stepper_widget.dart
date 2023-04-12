import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/config/route_names.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_underline_button.dart';
import 'package:open_admin_app/widgets/stepper/custom_stepper.dart';
import 'package:open_admin_app/widgets/stepper/fh_stepper.dart';
import 'package:open_admin_app/widgets/stepper/progress_stepper_bloc.dart';


class FHSetupProgressStepper extends StatefulWidget {
  const FHSetupProgressStepper({Key? key}) : super(key: key);

  @override
  _StepperState createState() => _StepperState();
}

class _StepperState extends State<FHSetupProgressStepper> {
  var _index = 0;

  @override
  Widget build(BuildContext context) {
    var bloc = BlocProvider.of<StepperBloc>(context);
    final captionStyle = Theme.of(context).textTheme.bodySmall;
    final cardWidgetTextPart =
        Column(crossAxisAlignment: CrossAxisAlignment.start, children: <Widget>[
      const SizedBox(height: 16),
      Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          const Text(
            'Application setup progress',
          ),
          IconButton(
              splashRadius: 20,
              icon: const Icon(
                Icons.close,
                size: 16.0,
              ),
              onPressed: () => bloc.mrClient.stepperOpened = false)
        ],
      ),
      const SizedBox(height: 8.0),
      const FHPageDivider(),
    ]);

    final ScrollController controller = ScrollController();
    return Material(
      elevation: 16.0,
      child: Container(
          constraints: const BoxConstraints(maxWidth: 260),
          height: MediaQuery.of(context).size.height - kToolbarHeight,
          padding: const EdgeInsets.only(bottom: 16.0, right: 16.0, left: 16.0),
          child: ScrollConfiguration(
            behavior: CustomScrollBehavior(),
            child: SingleChildScrollView(
              controller: controller,
              child: Column(
                children: [
                  cardWidgetTextPart,
                  StreamBuilder<FHStepper>(
                      stream: bloc.stepper,
                      builder: (context, snapshot) {
                        if (snapshot.hasData) {
                          return CustomStepper(
                              physics: const ClampingScrollPhysics(),
                              steps: [
                                CustomStep(
                                    title: const Text('Create application'),
                                    state: snapshot.data!.application == true
                                        ? CustomStepState.complete
                                        : CustomStepState.indexed,
                                    content: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: <Widget>[
                                        applicationDropDown(bloc),
                                        FHUnderlineButton(
                                          title: 'Go to Applications',
                                          onPressed: () => {
                                            ManagementRepositoryClientBloc
                                                .router
                                                .navigateTo(
                                                    context, '/applications')
                                          },
                                        ),
                                      ],
                                    )),
                                CustomStep(
                                    title: const Text('Create team group'),
//                            isActive: _index == 1,
                                    state: snapshot.data!.group == true
                                        ? CustomStepState.complete
                                        : CustomStepState.indexed,
                                    content: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: <Widget>[
                                        Text(
                                          'Groups are portfolio-wide, we recommend creating application specific groups eg “MyApp developers”',
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title: 'Go to Groups',
                                          onPressed: () => {
                                            ManagementRepositoryClientBloc
                                                .router
                                                .navigateTo(context, '/groups')
                                          },
                                        ),
                                      ],
                                    )),
                                CustomStep(
                                    title: const Text('Create service account'),
                                    state: snapshot.data!.serviceAccount == true
                                        ? CustomStepState.complete
                                        : CustomStepState.indexed,
                                    content: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: <Widget>[
                                        Text(
                                          'Service accounts are portfolio-wide, we recommend creating at least two service accounts specific to an application, e.g. “SA-MyApp-Prod” and "SA-MyApp-Non-Prod"',
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title: 'Go to Service Accounts',
                                          onPressed: () => {
                                            ManagementRepositoryClientBloc
                                                .router
                                                .navigateTo(context,
                                                    '/service-accounts')
                                          },
                                        ),
                                      ],
                                    )),
                                CustomStep(
                                    title: const Text('Create environment'),
                                    state: snapshot.data!.application
                                        ? (snapshot.data!.environment == true
                                            ? CustomStepState.complete
                                            : CustomStepState.indexed)
                                        : CustomStepState.disabled,
                                    content: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: <Widget>[
                                        Text(
                                          'Create an environment for selected application, e.g. "test", "dev", "prod"',
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title: 'Go to Environments',
                                          onPressed: () => {
                                            ManagementRepositoryClientBloc
                                                .router
                                                .navigateTo(
                                              context,
                                              '/app-settings',
                                              params: {
                                                'id': [bloc.applicationId!],
                                                'tab': ['environments']
                                              },
                                            )
                                          },
                                        ),
                                      ],
                                    )),
                                CustomStep(
                                    title: const Text('Give access to groups'),
                                    state: snapshot.data!.environment
                                        ? (snapshot.data!.groupPermission ==
                                                true
                                            ? CustomStepState.complete
                                            : CustomStepState.indexed)
                                        : CustomStepState.disabled,
                                    content: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: <Widget>[
                                        Text(
                                          'Assign an application environment level permissions to a group of users',
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title: 'Go to Group Permissions',
                                          onPressed: () => {
                                            ManagementRepositoryClientBloc
                                                .router
                                                .navigateTo(
                                              context,
                                              '/app-settings',
                                              params: {
                                                'id': [bloc.applicationId!],
                                                'tab': ['group-permissions']
                                              },
                                            )
                                          },
                                        ),
                                      ],
                                    )),
                                CustomStep(
                                    title: const Text(
                                        ' Give access to service\n account'),
                                    state: snapshot.data!.environment &&
                                            snapshot.data!.serviceAccount
                                        ? (snapshot.data!
                                                    .serviceAccountPermission ==
                                                true
                                            ? CustomStepState.complete
                                            : CustomStepState.indexed)
                                        : CustomStepState.disabled,
                                    content: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: <Widget>[
                                        Text(
                                          'Assign an application environment level permissions to a service account',
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title:
                                              'Go to SA Permissions',
                                          onPressed: () => {
                                            ManagementRepositoryClientBloc
                                                .router
                                                .navigateTo(
                                              context,
                                              '/app-settings',
                                              params: {
                                                'id': [bloc.applicationId!],
                                                'tab': ['service-accounts']
                                              },
                                            )
                                          },
                                        ),
                                      ],
                                    )),
                                CustomStep(
                                    title: const Text('Create a feature'),
                                    state: snapshot.data!.application
                                        ? (snapshot.data!.feature == true
                                            ? CustomStepState.complete
                                            : CustomStepState.indexed)
                                        : CustomStepState.disabled,
                                    content: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: <Widget>[
                                        Text(
                                          'Create a feature for an application',
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title: 'Go to Features',
                                          onPressed: () => {
                                            ManagementRepositoryClientBloc
                                                .router
                                                .navigateTo(context,
                                                    routeNameFeatureDashboard,
                                               )
                                          },
                                        ),
                                      ],
                                    )),
                              ],
                              controlsBuilder:
                                  (BuildContext context, controlDetails) =>
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
                ],
              ),
            ),
          )),
    );
  }

  Widget applicationDropDown(StepperBloc bloc) {
    return StreamBuilder<List<Application>>(
        stream: bloc.appsList,
        builder: (context, snapshot) {
          if (snapshot.hasData && snapshot.data!.isNotEmpty) {
            return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Text('Select application or create a new one by following the link below',
                      style: Theme.of(context).textTheme.bodySmall),
                  Container(
                      constraints: const BoxConstraints(maxWidth: 200),
                      child: applicationsDropdown(snapshot.data!, bloc))
                ]);
          }
          return Container();
        });
  }

  Widget applicationsDropdown(
      List<Application> applications, StepperBloc bloc) {
    return InkWell(
      mouseCursor: SystemMouseCursors.click,
      child: DropdownButton(
        icon: const Padding(
          padding: EdgeInsets.only(left: 8.0),
          child: Icon(
            Icons.keyboard_arrow_down,
            size: 18,
          ),
        ),
        isExpanded: true,
        style: Theme.of(context).textTheme.bodyLarge,
        items: applications.map((Application application) {
          return DropdownMenuItem<String>(
              value: application.id,
              child: Text(application.name,
                  style: Theme.of(context).textTheme.bodyMedium));
        }).toList(),
        hint: Text('Select application',
            style: Theme.of(context).textTheme.titleSmall),
        onChanged: (String? value) {
          setState(() {
            bloc.mrClient.setCurrentAid(value);
          });
        },
        value: bloc.applicationId,
      ),
    );
  }
}
