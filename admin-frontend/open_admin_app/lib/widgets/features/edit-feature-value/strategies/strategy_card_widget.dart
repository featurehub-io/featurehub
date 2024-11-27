import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/theme/custom_text_style.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/split_edit.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';

class StrategyCardWidget extends StatelessWidget {
  final bool editable;
  final Widget editableHolderWidget;
  final RolloutStrategy? rolloutStrategy;
  final ThinGroupRolloutStrategy? groupRolloutStrategy;
  final RolloutStrategyInstance? applicationRolloutStrategy;
  final EditingFeatureValueBloc strBloc;

  const StrategyCardWidget(
      {Key? key,
      required this.editable,
      required this.editableHolderWidget,
      this.rolloutStrategy,
      required this.strBloc,
      this.groupRolloutStrategy,
      this.applicationRolloutStrategy})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 50,
      child: InkWell(
        mouseCursor:
            rolloutStrategy != null || applicationRolloutStrategy != null
                ? SystemMouseCursors.grab
                : null,
        child: Card(
          elevation: 0.0, // if this is not set, then colors are screwed up
          color: rolloutStrategy != null
              ? strategyTextColor.withOpacity(0.15)
              : groupRolloutStrategy != null
                  ? groupStrategyTextColor.withOpacity(0.15)
                  : applicationRolloutStrategy != null
                      ? applicationStrategyTextColor.withOpacity(0.15)
                      : defaultTextColor.withOpacity(0.15),
          child: Padding(
            padding: const EdgeInsets.only(left: 8.0, right: 2.0),
            child: Row(
                mainAxisAlignment: MainAxisAlignment.start,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[
                  Expanded(
                      flex: 4,
                      child: groupRolloutStrategy != null
                          ? Text(groupRolloutStrategy!.name,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: Theme.of(context).textTheme.labelLarge)
                          : applicationRolloutStrategy != null
                              ? Text(applicationRolloutStrategy!.name!,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  style: Theme.of(context).textTheme.labelLarge)
                              : (rolloutStrategy?.name == null
                                  ? Text('default',
                                      style: Theme.of(context)
                                          .textTheme
                                          .bodySmall!
                                          .copyWith(color: defaultTextColor))
                                  : Text(rolloutStrategy!.name,
                                      maxLines: 2,
                                      overflow: TextOverflow.ellipsis,
                                      style: Theme.of(context)
                                          .textTheme
                                          .labelLarge))),
                  Expanded(
                      flex: 1,
                      child: Text("serve",
                          style: CustomTextStyle.bodySmallLight(context))),
                  Expanded(flex: 4, child: editableHolderWidget),
                  if (rolloutStrategy != null && groupRolloutStrategy == null)
                    Expanded(
                      flex: 3,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          EditValueStrategyLinkButton(
                            editable: editable,
                            rolloutStrategy: rolloutStrategy!,
                            fvBloc: strBloc,
                          ),
                          if (editable)
                            IconButton(
                              mouseCursor: SystemMouseCursors.click,
                              icon: const Icon(Icons.delete, size: 16),
                              onPressed: () =>
                                  strBloc.removeStrategy(rolloutStrategy!),
                            ),
                        ],
                      ),
                    ),
                  if (groupRolloutStrategy != null)
                    Expanded(
                      flex: 3,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          IconButton(
                            tooltip: 'Edit Feature Groups',
                            onPressed: () {
                              Navigator.pop(context);
                              ManagementRepositoryClientBloc.router
                                  .navigateTo(context, '/feature-groups');
                            },
                            icon: const Icon(Icons.arrow_forward, size: 18),
                          ),
                        ],
                      ),
                    ),
                  if (applicationRolloutStrategy != null)
                    Expanded(
                      flex: 3,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          if (editable)
                            IconButton(
                                mouseCursor: SystemMouseCursors.click,
                                icon: const Icon(Icons.edit, size: 16),
                                onPressed: () {
                                  Navigator.pop(context);
                                  ManagementRepositoryClientBloc.router
                                      .navigateTo(
                                          context, '/edit-application-strategy',
                                          params: {
                                        'id': [
                                          applicationRolloutStrategy!
                                              .strategyId!
                                        ]
                                      });
                                }),
                          if (editable)
                            IconButton(
                              mouseCursor: SystemMouseCursors.click,
                              icon: const Icon(Icons.delete, size: 16),
                              onPressed: () =>
                                  strBloc.removeApplicationStrategy(
                                      applicationRolloutStrategy!),
                            ),
                        ],
                      ),
                    ),
                ]),
          ),
        ),
      ),
    );
  }
}
