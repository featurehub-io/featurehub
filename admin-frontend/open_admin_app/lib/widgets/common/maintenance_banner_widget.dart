import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';

/// Shows a warning banner at the top of every page when the system is in or
/// approaching a maintenance window. Cannot be dismissed — it clears
/// automatically when maintenance ends. Listens to
/// [ManagementRepositoryClientBloc.maintenanceStream] and rebuilds
/// automatically when the state changes.
class MaintenanceBannerWidget extends StatelessWidget {
  const MaintenanceBannerWidget({super.key});

  static final _dateFmt = DateFormat('dd MMM yyyy HH:mm');

  static String _formatUtc(DateTime dt) =>
      '${_dateFmt.format(dt.toUtc())} UTC';

  @override
  Widget build(BuildContext context) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return StreamBuilder<MaintenanceInfo?>(
      stream: mrBloc.maintenanceStream,
      builder: (context, snapshot) {
        final info = snapshot.data;
        if (info == null) return const SizedBox.shrink();

        final String message;
        final Color bannerColor;
        final Color contentColor;

        if (info.active) {
          // System is actively in maintenance — users are locked out
          final baseMsg = (info.message != null && info.message!.isNotEmpty)
              ? info.message!
              : 'The system is undergoing maintenance. Some features may be unavailable.';
          final endPart = info.endTime != null
              ? ' Maintenance is expected to end at ${_formatUtc(info.endTime!)}.'
              : '';
          message = '$baseMsg$endPart';
          bannerColor = Theme.of(context).colorScheme.errorContainer;
          contentColor = Theme.of(context).colorScheme.onErrorContainer;
        } else {
          // Upcoming maintenance — warn users but don't lock them out
          final startPart = info.startTime != null
              ? 'Scheduled maintenance will begin at ${_formatUtc(info.startTime!)}.'
              : 'Scheduled maintenance is coming soon.';
          final endPart = info.endTime != null
              ? ' Expected to end at ${_formatUtc(info.endTime!)}.'
              : '';
          final customMsg =
              (info.message != null && info.message!.isNotEmpty)
                  ? ' ${info.message}'
                  : '';
          message = '$startPart$endPart$customMsg';
          bannerColor = Theme.of(context).colorScheme.tertiaryContainer;
          contentColor = Theme.of(context).colorScheme.onTertiaryContainer;
        }

        return Material(
          color: Colors.transparent,
          child: Container(
            width: double.infinity,
            color: bannerColor,
            padding:
                const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Icon(
                  info.active
                      ? Icons.warning_amber_rounded
                      : Icons.schedule_rounded,
                  color: contentColor,
                  size: 20,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    message,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: contentColor,
                        ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
