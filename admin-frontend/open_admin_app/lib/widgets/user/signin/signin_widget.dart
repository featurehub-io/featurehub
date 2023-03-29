import 'dart:math';

import 'package:animator/animator.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/user/signin/signin_provider_button.dart';
import 'package:open_admin_app/widgets/user/update/password_reset_widget.dart';
import 'package:openapi_dart_common/openapi.dart';

class SigninWidget extends StatefulWidget {
  const SigninWidget(this.bloc, {Key? key}) : super(key: key);

  final ManagementRepositoryClientBloc bloc;

  @override
  State<StatefulWidget> createState() {
    return _SigninState();
  }
}

class _SigninState extends State<SigninWidget> {
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _formKey = GlobalKey<FormState>(debugLabel: 'signin_widget');
  bool displayError = false;
  String? personIdForResetWidget;
  bool loggingIn = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    // when we are here we know that there are either (a) multiple sign-in providers or (b) at least a local option

    widget.bloc.lastUsername().then((lu) {
      if (lu != null && _email.text.isEmpty) {
        setState(() {
          _email.text = lu;
        });
      }
    });
  }

  void _handleSubmitted() {
    if (!loggingIn) {
      if (_formKey.currentState!.validate()) {
        _login();
      }
    }
  }

  void _loginViaProvider(String provider) {
    widget.bloc.identityProviders.authenticateViaProvider(provider);
  }

  void _login() {
    setState(() {
      loggingIn = true;
    });

    // when logging in, we could have received a temporary password and
    // thus need to reset the password before being allowed to login.
    // this all has to happen under the facade of the login process, we can't
    // "let them out" of this route (login) and we can't expose it as a route
    // as that makes no sense (to navigate to the reset password route)

    widget.bloc.login(_email.text, _password.text).then((tp) {
      if (tp.person?.passwordRequiresReset == true) {
        setState(() {
          personIdForResetWidget = tp.person?.id?.id;
          loggingIn = false;
        });
      } else if (tp.accessToken != null) {
        // we shouldn't even get here
        setState(() {
          loggingIn = false;
          personIdForResetWidget = null;
        });
      }
    }).catchError((e, s) {
      if (e is ApiException && e.code == 404) {
        setState(() {
          displayError = true;
          loggingIn = false;
        });
      } else {
        widget.bloc.dialogError(e, s);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    if (personIdForResetWidget != null) {
      return ResetPasswordWidget(personIdForResetWidget!);
    }

    return Form(
      key: _formKey,
      child: FHCardWidget(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.only(bottom: 26.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children:  [Image.asset('assets/logo/FeatureHub-icon.png',
                    width: 40, height: 40)],
              ),
            ),
            SizedBox(
              height: 48.0,
              child: Text(
                'Sign in to FeatureHub\n\n',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
            ),
            if (widget.bloc.identityProviders.has3rdParty)
              _SetupPage1ThirdPartyProviders(
                bloc: widget.bloc,
                selectedExternalProviderFunc: _loginViaProvider,
              ),
            if (widget.bloc.identityProviders.has3rdParty &&
                widget.bloc.identityProviders.hasLocal)
              Column(
                children: [
                  const SizedBox(height: 24.0),
                  const FHPageDivider(),
                  Padding(
                      padding: const EdgeInsets.fromLTRB(0, 24, 0, 16),
                      child: Text('or sign in with a username and password',
                          style: Theme.of(context).textTheme.bodySmall)),
                ],
              ),
            if (widget.bloc.identityProviders.hasLocal)
              Column(
                children: <Widget>[
                  TextFormField(
                      controller: _email,
                      autofocus: true,
                      textInputAction: TextInputAction.next,
                      onFieldSubmitted: (_) => _handleSubmitted,
                      validator: (v) => v == null || v.isEmpty
                          ? 'Please enter your email'
                          : null,
                      decoration:
                          const InputDecoration(labelText: 'Email address')),
                  TextFormField(
                      controller: _password,
                      obscureText: true,
                      textInputAction: TextInputAction.next,
                      onFieldSubmitted: (_) => _handleSubmitted(),
                      validator: (v) => v == null || v.isEmpty
                          ? 'Please enter your password'
                          : null,
                      decoration: const InputDecoration(labelText: 'Password')),
                ],
              ),
            if (widget.bloc.identityProviders.hasLocal)
              Container(
                  child: displayError
                      ? Text(
                          'Incorrect email address or password',
                          style: Theme.of(context)
                              .textTheme
                              .bodyMedium!
                              .copyWith(color: Theme.of(context).colorScheme.error),
                        )
                      : Container()),
            if (widget.bloc.identityProviders.hasLocal)
              Container(
                padding: const EdgeInsets.only(top: 20),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
//                    FHFlatButtonTransparent(
//                      onPressed: () =>
//                          Navigator.pushNamed(context, '/forgot-password'),
//                      title: 'Forgot password?',
//                      keepCase: true,
//                    ),
                    Expanded(
                      child: FilledButton(
                          onPressed: () {
                            _handleSubmitted();
                          }, child: const Text('Sign in'),),
                    )
                  ],
                ),
              )
          ],
        ),
      ),
    );
  }
}

class RotatingLogoWidget extends StatelessWidget {
  const RotatingLogoWidget({
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Animator<double>(
        tween: Tween<double>(begin: 0, end: 2 * pi),
        duration: const Duration(seconds: 2),
        repeats: 0,
        builder: (context, anim, other) => Transform.rotate(
              angle: anim.value,
              child: Image.asset('assets/logo/FeatureHub-icon.png',
                  width: 40, height: 40),
            ));
  }
}

typedef _SelectedExternalFunction = void Function(String provider);

class _SetupPage1ThirdPartyProviders extends StatelessWidget {
  final ManagementRepositoryClientBloc bloc;
  final _SelectedExternalFunction selectedExternalProviderFunc;

  const _SetupPage1ThirdPartyProviders(
      {Key? key,
      required this.bloc,
      required this.selectedExternalProviderFunc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        for (dynamic provider in bloc.identityProviders.externalProviders)
          Padding(
              padding: const EdgeInsets.only(top: 12.0),
              child: SignInProviderButton(
                  provider: provider,
                  providedIcon: bloc.identityProviders.identityInfo[provider],
                  func: () => selectedExternalProviderFunc(provider))),
      ],
    );
  }
}
