import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:flutter_signin_button/flutter_signin_button.dart';


class SignInProviderButton extends StatelessWidget {
  const SignInProviderButton(
      {Key? key, required this.func, required this.provider})
      : super(key: key);
  final Function func;
  final String provider;

  @override
  Widget build(BuildContext context) {
    switch (provider) {
      case 'oauth2-google':
        return SignInButton(Buttons.GoogleDark, onPressed: () {
          func;
        });
      case 'oauth2-github':
        return SignInButton(Buttons.GitHub, onPressed: () {
          func;
        });
      case 'oauth2-azure':
        return SignInButton(Buttons.Microsoft, onPressed: () {
          func;
        });
      case 'oauth2-keycloak':
        return SignInButtonBuilder(
          backgroundColor: Colors.blue,
          onPressed: () {
            func;
          },
          text: 'Sign in with Keycloak',
          padding: const EdgeInsets.symmetric(vertical: 16),
          icon: FontAwesome.key,
        );
      default:
        return const SizedBox.shrink();
    }
  }
}
