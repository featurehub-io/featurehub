import 'package:app_singleapp/widgets/features/feature_dashboard_constants.dart';
import 'package:app_singleapp/widgets/features/percentage_utils.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';
import 'package:super_tooltip/super_tooltip.dart';


class StrategyRulesSuperTooltip extends StatefulWidget {

  const StrategyRulesSuperTooltip({Key key, @ required this.child, @required this.rolloutStrategy}) : super(key: key);

  final Widget child;
  final RolloutStrategy rolloutStrategy;

  @override
  _StrategyRulesSuperTooltipState createState() =>  _StrategyRulesSuperTooltipState();
}

class _StrategyRulesSuperTooltipState extends State<StrategyRulesSuperTooltip> {
  SuperTooltip tooltip;

  void onTap() {
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
        if(widget.rolloutStrategy.percentage != null)
          Text('${widget.rolloutStrategy.percentageText}%', style: Theme.of(context).textTheme.caption.copyWith(color: Theme.of(context).buttonColor)),
        if(widget.rolloutStrategy.attributes.any((rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.userkey.name))
          Icon(Icons.person_outline, size: 16.0,),
        if(widget.rolloutStrategy.attributes.any((rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.country.name))
          Icon(Icons.tour_outlined, size: 16.0,),
        if(widget.rolloutStrategy.attributes.any((rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.platform.name))
          Icon(Icons.desktop_windows, size: 16.0,),
        if(widget.rolloutStrategy.attributes.any((rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.device.name))
          Icon(Icons.devices, size: 16.0,),
        if(widget.rolloutStrategy.attributes.any((rsa) => rsa.fieldName == StrategyAttributeWellKnownNames.version.name))
          Icon(Icons.looks_one_outlined, size: 16.0,),
        if(widget.rolloutStrategy.attributes.any((rsa) => StrategyAttributeWellKnownNames.values.every((value) => rsa.fieldName != value.name)))
          Icon(Icons.construction_outlined, size: 16.0,),
      ],),
    );

    tooltip.show(context);
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      mouseCursor: SystemMouseCursors.click,
      onTap: () => onTap(),
      child: widget.child,
    );
  }
}
