import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/percentage_utils.dart';
import 'package:open_admin_app/widgets/strategyeditor/transform_strategy_conditions.dart';
import 'package:open_admin_app/widgets/strategyeditor/transform_strategy_type_field.dart';

const _maxPercentage = 1000000.0;
const _maxValueChips = 8;
const _cardWidth = 340.0;

enum StrategyTooltipKind { custom, application, portfolio }

/// The rollout details of a strategy as shown in the dashboard hover card.
class StrategyTooltipDetails {
  final int? percentage;

  /// true when the feature value overrides the shared strategy's percentage
  final bool percentageOverridden;
  final List<RolloutStrategyAttribute> attributes;

  StrategyTooltipDetails(
      {this.percentage,
      this.percentageOverridden = false,
      List<RolloutStrategyAttribute>? attributes})
      : attributes = attributes ?? [];
}

/// Wraps [child] so that hovering it shows a card describing the strategy:
/// its name and kind, the percentage rollout and each targeting rule.
class StrategyTooltip extends StatelessWidget {
  final String name;
  final StrategyTooltipKind kind;
  final Color accent;

  /// null when the (shared) strategy definition is not available
  final StrategyTooltipDetails? details;
  final Widget child;

  const StrategyTooltip(
      {super.key,
      required this.name,
      required this.kind,
      required this.accent,
      required this.details,
      required this.child});

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      richMessage: WidgetSpan(
          child: StrategyTooltipCard(
              name: name, kind: kind, accent: accent, details: details)),
      decoration: const BoxDecoration(),
      padding: EdgeInsets.zero,
      waitDuration: const Duration(milliseconds: 250),
      child: child,
    );
  }
}

class StrategyTooltipCard extends StatelessWidget {
  final String name;
  final StrategyTooltipKind kind;
  final Color accent;
  final StrategyTooltipDetails? details;

  const StrategyTooltipCard(
      {super.key,
      required this.name,
      required this.kind,
      required this.accent,
      required this.details});

  String _kindLabel(AppLocalizations l10n) {
    switch (kind) {
      case StrategyTooltipKind.custom:
        return l10n.tooltipCustomStrategy;
      case StrategyTooltipKind.application:
        return l10n.applicationStrategyTooltip;
      case StrategyTooltipKind.portfolio:
        return l10n.portfolioStrategyTooltip;
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final muted = theme.colorScheme.onSurface.withAlpha(150);
    final d = details;

    final sections = <Widget>[];
    if (d == null) {
      sections.add(Text(l10n.tooltipRulesUnavailable,
          style: theme.textTheme.bodySmall?.copyWith(color: muted)));
    } else {
      if (d.percentage != null) {
        sections.add(_PercentageSection(
            percentage: d.percentage!,
            overridden: d.percentageOverridden,
            accent: accent,
            muted: muted));
      }
      if (d.attributes.isNotEmpty) {
        sections.add(Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.tooltipRulesAllMatch,
                style: theme.textTheme.labelMedium?.copyWith(color: muted)),
            const SizedBox(height: 4),
            for (final attr in d.attributes)
              _RuleRow(attribute: attr, accent: accent, muted: muted),
          ],
        ));
      } else if (d.percentage == null) {
        sections.add(Text(l10n.tooltipNoRules,
            style: theme.textTheme.bodySmall?.copyWith(color: muted)));
      }
    }

    return SizedBox(
      width: _cardWidth,
      child: Material(
        elevation: 8,
        color: theme.colorScheme.surface,
        borderRadius: BorderRadius.circular(8),
        clipBehavior: Clip.antiAlias,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _Header(name: name, kindLabel: _kindLabel(l10n), accent: accent),
            Padding(
              padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  for (var i = 0; i < sections.length; i++) ...[
                    if (i > 0) const Divider(height: 20),
                    sections[i],
                  ]
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Header extends StatelessWidget {
  final String name;
  final String kindLabel;
  final Color accent;

  const _Header(
      {required this.name, required this.kindLabel, required this.accent});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final onAccent =
        ThemeData.estimateBrightnessForColor(accent) == Brightness.dark
            ? Colors.white
            : Colors.black87;
    return Container(
      decoration: BoxDecoration(
        color: accent.withAlpha(30),
        border: Border(left: BorderSide(color: accent, width: 4)),
      ),
      padding: const EdgeInsets.fromLTRB(10, 10, 12, 10),
      child: Row(
        children: [
          Expanded(
            child: Text(name,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleSmall
                    ?.copyWith(fontWeight: FontWeight.bold)),
          ),
          const SizedBox(width: 8),
          Flexible(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
              decoration: BoxDecoration(
                  color: accent, borderRadius: BorderRadius.circular(10)),
              child: Text(kindLabel,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelSmall
                      ?.copyWith(color: onAccent, fontWeight: FontWeight.w600)),
            ),
          ),
        ],
      ),
    );
  }
}

class _PercentageSection extends StatelessWidget {
  final int percentage;
  final bool overridden;
  final Color accent;
  final Color muted;

  const _PercentageSection(
      {required this.percentage,
      required this.overridden,
      required this.accent,
      required this.muted});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(l10n.tooltipRollout,
                  style: theme.textTheme.labelMedium?.copyWith(color: muted)),
            ),
            Text('${percentageText(percentage)}%',
                style: theme.textTheme.titleSmall
                    ?.copyWith(fontWeight: FontWeight.bold)),
          ],
        ),
        const SizedBox(height: 6),
        ClipRRect(
          borderRadius: BorderRadius.circular(3),
          child: LinearProgressIndicator(
            value: (percentage / _maxPercentage).clamp(0.0, 1.0),
            minHeight: 6,
            color: accent,
            backgroundColor: accent.withAlpha(40),
          ),
        ),
        if (overridden) ...[
          const SizedBox(height: 4),
          Row(
            children: [
              Icon(Icons.tune, size: 12, color: muted),
              const SizedBox(width: 4),
              Expanded(
                child: Text(l10n.tooltipPercentageOverridden,
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: muted, fontStyle: FontStyle.italic)),
              ),
            ],
          ),
        ],
      ],
    );
  }
}

class _RuleRow extends StatelessWidget {
  final RolloutStrategyAttribute attribute;
  final Color accent;
  final Color muted;

  const _RuleRow(
      {required this.attribute, required this.accent, required this.muted});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final theme = Theme.of(context);
    final wellKnown = StrategyAttributeWellKnownNames.values
        .firstWhereOrNull((e) => e.name == attribute.fieldName);

    final IconData icon;
    final String label;
    switch (wellKnown) {
      case StrategyAttributeWellKnownNames.userkey:
        icon = Icons.person_outline;
        label = l10n.tooltipUserKey;
      case StrategyAttributeWellKnownNames.country:
        icon = Icons.public;
        label = l10n.tooltipCountry;
      case StrategyAttributeWellKnownNames.platform:
        icon = Icons.computer;
        label = l10n.tooltipPlatform;
      case StrategyAttributeWellKnownNames.device:
        icon = Icons.smartphone;
        label = l10n.tooltipDevice;
      case StrategyAttributeWellKnownNames.version:
        icon = Icons.new_releases_outlined;
        label = l10n.tooltipVersion;
      case StrategyAttributeWellKnownNames.session:
      case null:
        icon = wellKnown == null ? Icons.tune : Icons.timelapse;
        label = attribute.fieldName;
    }

    // condition and value names are SDK values and stay untranslated
    final condition = transformStrategyAttributeConditionalValueToString(
        attribute.conditional, wellKnown);
    final values = attribute.values
        .map((v) => wellKnown == StrategyAttributeWellKnownNames.country
            ? v.toString().replaceAll('_', ' ')
            : v.toString())
        .toList();
    final chipStyle = theme.textTheme.bodySmall;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(top: 1),
            child: Icon(icon, size: 16, color: accent),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text.rich(TextSpan(children: [
                  TextSpan(
                      text: label,
                      style: theme.textTheme.bodyMedium
                          ?.copyWith(fontWeight: FontWeight.w600)),
                  TextSpan(
                      text: '  $condition',
                      style: theme.textTheme.bodySmall?.copyWith(color: muted)),
                  if (wellKnown == null)
                    TextSpan(
                        text:
                            '  (${transformRolloutStrategyTypeFieldToString(attribute.type)})',
                        style: theme.textTheme.bodySmall?.copyWith(
                            color: muted, fontStyle: FontStyle.italic)),
                ])),
                if (values.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Wrap(
                    spacing: 4,
                    runSpacing: 4,
                    children: [
                      for (final v in values.take(_maxValueChips))
                        _ValueChip(text: v, style: chipStyle),
                      if (values.length > _maxValueChips)
                        _ValueChip(
                            text: '+${values.length - _maxValueChips}',
                            style: chipStyle?.copyWith(color: muted)),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ValueChip extends StatelessWidget {
  final String text;
  final TextStyle? style;

  const _ValueChip({required this.text, this.style});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.onSurface.withAlpha(18),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(text, style: style),
    );
  }
}
