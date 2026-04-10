import 'package:flutter/material.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';

class Oauth2FailRoute extends StatelessWidget {
  const Oauth2FailRoute({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Image.asset('assets/logo/FeatureHub-icon.png',
                width: 40, height: 40),
            const SizedBox(height: 24.0),
            Text(
              l10n.oauth2NotAuthorized,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 30,
              ),
            ),
            const SizedBox(height: 16.0),
            Text(
              l10n.oauth2ContactAdmin,
              textAlign: TextAlign.center,
              style: const TextStyle(
              ),
            ),
          ],
        ),
      ),
    );
  }
}
