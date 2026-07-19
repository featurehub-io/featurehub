import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:open_admin_app/api/client_api.dart';

import 'maintenance_info.dart';

class MaintenanceModeWidget extends StatelessWidget {
  final ManagementRepositoryClientBloc mrBloc;

  const MaintenanceModeWidget({super.key, required this.mrBloc});

  static final _dateFmt = DateFormat('dd MMM yyyy HH:mm');

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Material(
      color: Colors.black54,
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 480),
          child: Card(
            color: theme.colorScheme.errorContainer,
            child: Padding(
              padding: const EdgeInsets.all(32.0),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.build_rounded,
                    size: 48,
                    color: theme.colorScheme.onErrorContainer,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Scheduled Maintenance',
                    style: theme.textTheme.headlineSmall?.copyWith(
                      color: theme.colorScheme.onErrorContainer,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 12),
                  StreamBuilder(stream: mrBloc.activeMaintenanceStream, builder: (BuildContext context, AsyncSnapshot<MaintenanceInfo?> snapshot) {
                    if (!snapshot.hasData) return SizedBox.shrink();
                    final message =
                    (snapshot.data!.message != null && snapshot.data!.message!.isNotEmpty)
                        ? snapshot.data!.message!
                        : 'The system is undergoing scheduled maintenance.';

                    return Text(
                      message,
                      textAlign: TextAlign.center,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.colorScheme.onErrorContainer,
                      ),
                    );
                  }),
                  const SizedBox(height: 8),
                  StreamBuilder(stream: mrBloc.activeMaintenanceStream, builder: (BuildContext context, AsyncSnapshot<MaintenanceInfo?> snapshot) {
                    if (!snapshot.hasData) return SizedBox.shrink();
                    final endStr = snapshot.data!.end != null
                        ? '${_dateFmt.format(snapshot.data!.end!.toUtc())} UTC'
                        : null;

                    if (endStr == null) return SizedBox.shrink();
                    return Text(
                      'Expected to end at $endStr.',
                      textAlign: TextAlign.center,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onErrorContainer,
                      ),
                    );
                  }),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
