import 'package:flutter/material.dart';

import 'package:flutter_signin_button/flutter_signin_button.dart';
import 'package:mrapi/api.dart';

class SignInProviderButton extends StatelessWidget {
  const SignInProviderButton(
      {Key? key,
      required this.func,
      required this.provider,
      required this.providedIcon})
      : super(key: key);
  final Function func;
  final String provider;
  final IdentityProviderInfo? providedIcon;

  @override
  Widget build(BuildContext context) {
    switch (provider) {
      case 'oauth2-google':
        return SignInButton(Buttons.GoogleDark, onPressed: func);
      case 'oauth2-github':
        return SignInButton(Buttons.GitHub, onPressed: func);
      case 'oauth2-azure':
        return SignInButton(Buttons.Microsoft, onPressed: func);
      case 'oauth2-keycloak':
        return SignInButtonBuilder(
          backgroundColor: Colors.blue,
          onPressed: func,
          text: 'Sign in with Keycloak',
          padding: const EdgeInsets.symmetric(vertical: 16),
          icon: Icons.key,
        );
      default:
        if (providedIcon != null) {
          return SignInButtonBuilder(
            height: 28,
            backgroundColor:
                Color(int.parse(providedIcon!.buttonBackgroundColor)),
            onPressed: func,
            text: providedIcon!.buttonText,
            innerPadding: const EdgeInsets.all(4),
            image: Container(
                // height: 30,
                // width: 48,
                margin: const EdgeInsets.fromLTRB(0.0, 0.0, 10.0, 0.0),
                child: ClipRRect(
                    child: Image.network(
                  providedIcon!.buttonIcon,
                  height: 28,
                  // width: 36,
                ))),
          );
        }
        return const SizedBox.shrink();
    }
  }
}
