import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/maintenance/maintenance_info.dart';

/// The yellow banner announcing an upcoming maintenance window. It appears as soon as a response carries the
/// x-maintenance-* headers and disappears when they stop arriving.
class MaintenanceBannerWidget extends StatelessWidget {
  final ManagementRepositoryClientBloc mrBloc;

  const MaintenanceBannerWidget({super.key, required this.mrBloc});

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<MaintenanceInfo?>(
        stream: mrBloc.pendingMaintenanceStream,
        builder: (BuildContext context, AsyncSnapshot<MaintenanceInfo?> snapshot) {
          final message = snapshot.data?.message;
          if (message == null) return const SizedBox.shrink();

          return Container(
            color: Colors.yellow,
            padding: const EdgeInsets.symmetric(vertical: 2.0),
            child: Center(
              child: Text(
                message,
                style: const TextStyle(
                  color: Colors.black,
                  fontSize: 16.0,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          );
        });
  }
}
