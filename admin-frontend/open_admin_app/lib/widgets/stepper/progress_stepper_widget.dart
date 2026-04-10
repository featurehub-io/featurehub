import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/config/route_names.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_underline_button.dart';
import 'package:open_admin_app/widgets/stepper/custom_stepper.dart';
import 'package:open_admin_app/widgets/stepper/fh_stepper.dart';
import 'package:open_admin_app/widgets/stepper/progress_stepper_bloc.dart';


class FHSetupProgressStepper extends StatefulWidget {
  const FHSetupProgressStepper({super.key});

  @override
  StepperState createState() => StepperState();
}

class StepperState extends State<FHSetupProgressStepper> {
  var _index = 0;

  @override
  Widget build(BuildContext context) {
    var bloc = BlocProvider.of<StepperBloc>(context);
    final l10n = AppLocalizations.of(context)!;
    final captionStyle = Theme.of(context).textTheme.bodySmall;
    final cardWidgetTextPart =
        Column(crossAxisAlignment: CrossAxisAlignment.start, children: <Widget>[
      const SizedBox(height: 16),
      Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            l10n.stepperTitle,
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
                                    title: Text(l10n.stepCreateApplication),
                                    state: snapshot.data!.application == true
                                        ? CustomStepState.complete
                                        : CustomStepState.indexed,
                                    content: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: <Widget>[
                                        applicationDropDown(bloc, l10n),
                                        FHUnderlineButton(
                                          title: l10n.goToApplications,
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
                                    title: Text(l10n.stepCreateTeamGroup),
                                    state: snapshot.data!.group == true
                                        ? CustomStepState.complete
                                        : CustomStepState.indexed,
                                    content: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: <Widget>[
                                        Text(
                                          l10n.stepCreateTeamGroupHint,
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title: l10n.goToGroups,
                                          onPressed: () => {
                                            ManagementRepositoryClientBloc
                                                .router
                                                .navigateTo(context, '/groups')
                                          },
                                        ),
                                      ],
                                    )),
                                CustomStep(
                                    title: Text(l10n.stepCreateServiceAccount),
                                    state: snapshot.data!.serviceAccount == true
                                        ? CustomStepState.complete
                                        : CustomStepState.indexed,
                                    content: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: <Widget>[
                                        Text(
                                          l10n.stepCreateServiceAccountHint,
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title: l10n.goToServiceAccounts,
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
                                    title: Text(l10n.stepCreateEnvironment),
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
                                          l10n.stepCreateEnvironmentHint,
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title: l10n.goToEnvironments,
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
                                    title: Text(l10n.stepGiveAccessToGroups),
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
                                          l10n.stepGiveAccessToGroupsHint,
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title: l10n.goToGroupPermissions,
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
                                    title: Text(l10n.stepGiveAccessToServiceAccount),
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
                                          l10n.stepGiveAccessToServiceAccountHint,
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title: l10n.goToSAPermissions,
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
                                    title: Text(l10n.stepCreateFeature),
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
                                          l10n.stepCreateFeatureHint,
                                          style: captionStyle,
                                        ),
                                        const SizedBox(height: 4.0),
                                        FHUnderlineButton(
                                          title: l10n.goToFeatures,
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

  Widget applicationDropDown(StepperBloc bloc, AppLocalizations l10n) {
    return StreamBuilder<List<Application>>(
        stream: bloc.appsList,
        builder: (context, snapshot) {
          if (snapshot.hasData && snapshot.data!.isNotEmpty) {
            return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Text(l10n.stepSelectApplicationHint,
                      style: Theme.of(context).textTheme.bodySmall),
                  Container(
                      constraints: const BoxConstraints(maxWidth: 200),
                      child: applicationsDropdown(snapshot.data!, bloc, l10n))
                ]);
          }
          return Container();
        });
  }

  Widget applicationsDropdown(
      List<Application> applications, StepperBloc bloc, AppLocalizations l10n) {
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
        hint: Text(l10n.selectApplication,
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
