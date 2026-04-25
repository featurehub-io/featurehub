import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';

/// Shows a dismissible warning banner at the top of every page when the
/// system is in a maintenance window.  Listens to [ManagementRepositoryClientBloc.maintenanceStream]
/// and rebuilds automatically when the state changes.
class MaintenanceBannerWidget extends StatelessWidget {
  const MaintenanceBannerWidget({super.key});

  @override
  Widget build(BuildContext context) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    return StreamBuilder<MaintenanceInfo?>(
      stream: mrBloc.maintenanceStream,
      builder: (context, snapshot) {
        final info = snapshot.data;
        if (info == null || !info.active) return const SizedBox.shrink();

        final message = (info.message != null && info.message!.isNotEmpty)
            ? info.message!
            : 'The system is undergoing maintenance. Some features may be unavailable.';

        return Material(
          color: Colors.transparent,
          child: Container(
            width: double.infinity,
            color: Theme.of(context).colorScheme.errorContainer,
            padding:
                const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Icon(
                  Icons.warning_amber_rounded,
                  color: Theme.of(context).colorScheme.onErrorContainer,
                  size: 20,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    message,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color:
                              Theme.of(context).colorScheme.onErrorContainer,
                        ),
                  ),
                ),
                IconButton(
                  icon: Icon(
                    Icons.close,
                    size: 18,
                    color: Theme.of(context).colorScheme.onErrorContainer,
                  ),
                  tooltip: 'Dismiss',
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(),
                  onPressed: () => mrBloc.dismissMaintenanceBanner(),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
