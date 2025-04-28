import 'package:flutter/material.dart';
import 'package:openapi_dart_common/openapi.dart';

class FHLoadingError extends StatelessWidget {
  final VoidCallback? onRetry;
  final String? errorMessage;
  final dynamic error;

  const FHLoadingError({
    Key? key,
    this.onRetry,
    this.errorMessage,
    this.error,
  }) : super(key: key);

  String _getErrorMessage() {
    if (errorMessage != null) {
      return errorMessage!;
    }

    if (error is ApiException) {
      final apiError = error as ApiException;
      if (apiError.code == 404) {
        return 'The requested resource was not found';
      } else if (apiError.code == 403) {
        return 'You do not have permission to access this resource';
      } else if (apiError.code == 500) {
        return 'An internal server error occurred';
      } else if (apiError.message != null) {
        return apiError.message!;
      }
    }

    return 'An error occurred while loading the data';
  }

  @override
  Widget build(BuildContext context) {
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
            _getErrorMessage(),
            style: Theme.of(context).textTheme.titleMedium,
            textAlign: TextAlign.center,
          ),
          if (onRetry != null) ...[
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          ],
        ],
      ),
    );
  }
}
