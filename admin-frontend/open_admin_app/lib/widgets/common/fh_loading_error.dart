import 'package:flutter/material.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:openapi_dart_common/openapi.dart';

class FHLoadingError extends StatelessWidget {
  final VoidCallback? onRetry;
  final String? errorMessage;
  final dynamic error;

  const FHLoadingError({
    super.key,
    this.onRetry,
    this.errorMessage,
    this.error,
  });

  String _getErrorMessage(AppLocalizations l10n) {
    if (errorMessage != null) {
      return errorMessage!;
    }

    if (error is ApiException) {
      final apiError = error as ApiException;
      if (apiError.code == 404) {
        return l10n.errorNotFound;
      } else if (apiError.code == 403) {
        return l10n.errorForbidden;
      } else if (apiError.code == 500) {
        return l10n.errorInternalServer;
      } else if (apiError.message != null) {
        return apiError.message!;
      }
    }

    return l10n.errorLoadingData;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.error_outline,
            color: Colors.red,
            size: 48,
          ),
          const SizedBox(height: 16),
          Text(
            _getErrorMessage(l10n),
            style: Theme.of(context).textTheme.titleMedium,
            textAlign: TextAlign.center,
          ),
          if (onRetry != null) ...[
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: Text(l10n.retry),
            ),
          ],
        ],
      ),
    );
  }
}
