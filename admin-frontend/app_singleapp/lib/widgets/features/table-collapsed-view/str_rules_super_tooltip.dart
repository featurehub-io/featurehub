import 'package:app_singleapp/widgets/features/feature_dashboard_constants.dart';
import 'package:app_singleapp/widgets/features/percentage_utils.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';
import 'package:super_tooltip/super_tooltip.dart';

class StrategyRulesSuperTooltip extends StatefulWidget {
  const StrategyRulesSuperTooltip(
      {Key key, @required this.child, @required this.rolloutStrategy})
      : super(key: key);

  final Widget child;
  final RolloutStrategy rolloutStrategy;

  @override
  _StrategyRulesSuperTooltipState createState() =>
      _StrategyRulesSuperTooltipState();
}

class _StrategyRulesSuperTooltipState extends State<StrategyRulesSuperTooltip> {
  SuperTooltip tooltip;
  bool _show = false;
  bool _oldShow = null;

  @override
  void didUpdateWidget(StrategyRulesSuperTooltip oldWidget) {
    if ((widget != oldWidget || tooltip == null) &&
        widget.rolloutStrategy != null) {
      tooltip = SuperTooltip(
        maxWidth: 180,
        minWidth: 50,
        maxHeight: 60,
        popupDirection: TooltipDirection.up,
        arrowLength: 0.0,
        borderColor: strategyTextColor,
        borderWidth: 1.5,
        hasShadow: false,
        content: Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            if (widget.rolloutStrategy.percentage != null)
              Text('${widget.rolloutStrategy.percentageText}%',
                  style: Theme.of(context)
                      .textTheme
                      .caption
                      .copyWith(color: Theme.of(context).buttonColor)),
            if (widget.rolloutStrategy.attributes.any((rsa) =>
                rsa.fieldName == StrategyAttributeWellKnownNames.userkey.name))
              Icon(
                Icons.person_outline,
                size: 16.0,
              ),
            if (widget.rolloutStrategy.attributes.any((rsa) =>
                rsa.fieldName == StrategyAttributeWellKnownNames.country.name))
              Icon(
                Octicons.globe,
                size: 16.0,
              ),
            if (widget.rolloutStrategy.attributes.any((rsa) =>
                rsa.fieldName == StrategyAttributeWellKnownNames.platform.name))
              Icon(
                Icons.desktop_windows,
                size: 16.0,
              ),
            if (widget.rolloutStrategy.attributes.any((rsa) =>
                rsa.fieldName == StrategyAttributeWellKnownNames.device.name))
              Icon(
                MaterialCommunityIcons.devices,
                size: 16.0,
              ),
            if (widget.rolloutStrategy.attributes.any((rsa) =>
                rsa.fieldName == StrategyAttributeWellKnownNames.version.name))
              Icon(
                MaterialCommunityIcons.tag_multiple,
                size: 16.0,
              ),
            if (widget.rolloutStrategy.attributes.any((rsa) =>
                StrategyAttributeWellKnownNames.values
                    .every((value) => rsa.fieldName != value.name)))
              Icon(
                Icons.construction_outlined,
                size: 16.0,
              ),
          ],
        ),
      );
    }
  }

  void onHover() {
    if (tooltip != null) {
      _show ? tooltip.show(context) : tooltip.close();
      _oldShow = _show;
    }
  }

  @override
  void dispose() {
    super.dispose();

    if (tooltip != null) {
      tooltip.close();
      tooltip = null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onHover: (hover) {
        if (mounted) {
          setState(() {
            if (!_show && hover) {
              _show = hover;
              onHover();
            }
          });
        }
      },
      onTap: () {},
      child: widget.child,
    );
  }
}
