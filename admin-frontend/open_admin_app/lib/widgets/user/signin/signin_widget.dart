import 'dart:math';

import 'package:animator/animator.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_signin_button/flutter_signin_button.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_green.dart';
import 'package:openapi_dart_common/openapi.dart';

class SigninWidget extends StatefulWidget {
  const SigninWidget(this.bloc);

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
    widget.bloc.login(_email.text, _password.text).then((_) {
      setState(() {
        loggingIn = false;
      });
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
                children: <Widget>[const RotatingLogoWidget()],
              ),
            ),
            Container(
              height: 48.0,
              child: Text(
                'Sign in to FeatureHub\n\n',
                style: Theme.of(context).textTheme.headline5,
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
                  FHPageDivider(),
                  Padding(
                      padding: const EdgeInsets.fromLTRB(0, 24, 0, 16),
                      child: Text('or sign in with a username and password',
                          style: Theme.of(context).textTheme.caption)),
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
                              .bodyText2!
                              .copyWith(color: Theme.of(context).errorColor),
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
                      child: FHFlatButtonGreen(
                          title: 'Sign in',
                          onPressed: () {
                            _handleSubmitted();
                          }),
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
          Container(
            height: 48,
            child: Padding(
              padding: const EdgeInsets.only(top: 12.0),
              child: SignInButton(
                  provider == 'oauth2-google'
                      ? Buttons.GoogleDark
                      : provider == 'oauth2-github'
                          ? Buttons.GitHub
                          : Buttons.Microsoft, onPressed: () {
                selectedExternalProviderFunc(provider);
              }),
            ),
          ),
      ],
    );
  }
}
