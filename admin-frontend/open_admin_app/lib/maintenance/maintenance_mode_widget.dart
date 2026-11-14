import 'dart:async';
import 'dart:math' as math;
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:open_admin_app/api/client_api.dart';

import 'maintenance_info.dart';

/// Shown both as a full page (/maintenance) and as an overlay on top of the scaffold
/// while the server reports it is in maintenance.
class MaintenanceModeWidget extends StatefulWidget {
  final ManagementRepositoryClientBloc mrBloc;

  const MaintenanceModeWidget({super.key, required this.mrBloc});

  @override
  State<MaintenanceModeWidget> createState() => _MaintenanceModeWidgetState();
}

class _MaintenanceModeWidgetState extends State<MaintenanceModeWidget>
    with SingleTickerProviderStateMixin {
  late final AnimationController _spin;

  // re-renders the "in about x minutes" text
  Timer? _clock;

  static final _timeFmt = DateFormat.jm();
  static final _dateFmt = DateFormat('EEE d MMM');

  @override
  void initState() {
    super.initState();
    _spin = AnimationController(
        vsync: this, duration: const Duration(seconds: 12))
      ..repeat();
    _clock = Timer.periodic(
        const Duration(seconds: 30), (_) => mounted ? setState(() {}) : null);
  }

  @override
  void dispose() {
    _spin.dispose();
    _clock?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;

    return Material(
      type: MaterialType.transparency,
      child: Stack(
        fit: StackFit.expand,
        children: [
          // frosted backdrop over whatever was on screen
          BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 8, sigmaY: 8),
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    scheme.primary.withValues(alpha: 0.55),
                    scheme.surface.withValues(alpha: 0.75),
                  ],
                ),
              ),
            ),
          ),
          Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 460),
                child: Card(
                  elevation: 12,
                  shadowColor: Colors.black38,
                  clipBehavior: Clip.antiAlias,
                  color: scheme.surface,
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(24)),
                  child: StreamBuilder<MaintenanceInfo?>(
                    stream: widget.mrBloc.activeMaintenanceStream,
                    builder: (context, snapshot) =>
                        _content(theme, snapshot.data),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _content(ThemeData theme, MaintenanceInfo? info) {
    final scheme = theme.colorScheme;
    final message = (info?.message?.trim().isNotEmpty ?? false)
        ? info!.message!.trim()
        : 'FeatureHub is undergoing scheduled maintenance. Your features are '
            'still being served to your applications. Only the admin console '
            'is paused.';

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // thin animated bar across the top of the card
        LinearProgressIndicator(
          minHeight: 3,
          backgroundColor: scheme.primaryContainer,
          color: scheme.primary,
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(32, 32, 32, 28),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _gears(scheme),
              const SizedBox(height: 24),
              Text(
                'We\'ll be right back',
                textAlign: TextAlign.center,
                style: theme.textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: scheme.onSurface,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                'SCHEDULED MAINTENANCE',
                textAlign: TextAlign.center,
                style: theme.textTheme.labelMedium?.copyWith(
                  letterSpacing: 1.6,
                  color: scheme.primary,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 20),
              Text(
                message,
                textAlign: TextAlign.center,
                style: theme.textTheme.bodyMedium?.copyWith(
                  height: 1.5,
                  color: scheme.onSurfaceVariant,
                ),
              ),
              if (info?.end != null) ...[
                const SizedBox(height: 24),
                _expectedBack(theme, info!.end!),
              ],
              const SizedBox(height: 28),
              _checkNow(),
              const SizedBox(height: 12),
              Text(
                'We check automatically every '
                '${ManagementRepositoryClientBloc.maintenanceRecoveryInterval.inMinutes} '
                'minutes and will take you back in when it\'s done.',
                textAlign: TextAlign.center,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: scheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
      ],
    );
  }

  /// a large gear and a small one turning against each other
  Widget _gears(ColorScheme scheme) {
    return Container(
      width: 104,
      height: 104,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [scheme.primaryContainer, scheme.secondaryContainer],
        ),
      ),
      child: AnimatedBuilder(
        animation: _spin,
        builder: (context, _) {
          final angle = _spin.value * 2 * math.pi;
          return Stack(
            children: [
              Positioned(
                left: 18,
                top: 20,
                child: Transform.rotate(
                  angle: angle,
                  child: Icon(Icons.settings,
                      size: 52, color: scheme.onPrimaryContainer),
                ),
              ),
              Positioned(
                right: 16,
                bottom: 18,
                child: Transform.rotate(
                  // smaller gear turns the other way, and faster
                  angle: -angle * 1.6,
                  child: Icon(Icons.settings,
                      size: 32,
                      color: scheme.primary.withValues(alpha: 0.85)),
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _expectedBack(ThemeData theme, DateTime end) {
    final scheme = theme.colorScheme;
    final local = end.toLocal();
    final now = DateTime.now();
    final sameDay = local.year == now.year &&
        local.month == now.month &&
        local.day == now.day;
    final when = sameDay
        ? _timeFmt.format(local)
        : '${_dateFmt.format(local)}, ${_timeFmt.format(local)}';

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.schedule_rounded, color: scheme.primary),
          const SizedBox(width: 12),
          Flexible(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Expected back',
                    style: theme.textTheme.labelSmall
                        ?.copyWith(color: scheme.onSurfaceVariant)),
                const SizedBox(height: 2),
                Text.rich(
                  TextSpan(children: [
                    TextSpan(
                      text: when,
                      style: theme.textTheme.titleMedium
                          ?.copyWith(fontWeight: FontWeight.w600),
                    ),
                    TextSpan(
                      text: '  ${_relative(local.difference(now))}',
                      style: theme.textTheme.bodySmall
                          ?.copyWith(color: scheme.onSurfaceVariant),
                    ),
                  ]),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  static String _relative(Duration d) {
    if (d.isNegative || d.inMinutes < 1) return '(any moment now)';
    if (d.inMinutes < 60) {
      return '(in about ${d.inMinutes} min${d.inMinutes == 1 ? '' : 's'})';
    }
    final hours = d.inHours;
    final mins = d.inMinutes % 60;
    return mins == 0
        ? '(in about $hours h)'
        : '(in about $hours h $mins min)';
  }

  Widget _checkNow() {
    return StreamBuilder<bool>(
      stream: widget.mrBloc.maintenanceProbeInFlightStream,
      builder: (context, snapshot) {
        final checking = snapshot.data == true;
        return SizedBox(
          height: 44,
          child: FilledButton.icon(
            style: FilledButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              shape: const StadiumBorder(),
            ),
            onPressed:
                checking ? null : () => widget.mrBloc.checkMaintenanceRecovery(),
            icon: checking
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.refresh_rounded),
            label: Text(checking ? 'Checking...' : 'Check now'),
          ),
        );
      },
    );
  }
}
