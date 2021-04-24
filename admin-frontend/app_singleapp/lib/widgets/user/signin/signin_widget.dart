import 'dart:math';

import 'package:animator/animator.dart';
import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:openapi_dart_common/openapi.dart';

class SigninWidget extends StatefulWidget {
  SigninWidget(this.bloc);

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
        child: Container(
          padding: const EdgeInsets.fromLTRB(40, 8, 40, 40),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.max,
            children: <Widget>[
              Padding(
                padding: const EdgeInsets.only(bottom: 26.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    Animator<double>(
                        tween: Tween<double>(begin: 0, end: 2 * pi),
                        duration: Duration(seconds: 2),
                        repeats: 0,
                        builder: (context, anim, other) => Transform.rotate(
                              angle: anim.value,
                              child: Image.asset(
                                  'assets/logo/FeatureHub-icon.png',
                                  width: 40,
                                  height: 40),
                            ))
                  ],
                ),
              ),
              Text(
                'Sign in to FeatureHub\n\n',
                style: Theme.of(context).textTheme.headline5,
              ),
              if (widget.bloc.identityProviders.has3rdParty)
                _SetupPage1ThirdPartyProviders(
                  bloc: widget.bloc,
                  selectedExternalProviderFunc: _loginViaProvider,
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
                            InputDecoration(labelText: 'Email address')),
                    TextFormField(
                        controller: _password,
                        obscureText: true,
                        textInputAction: TextInputAction.next,
                        onFieldSubmitted: (_) => _handleSubmitted(),
                        validator: (v) => v == null || v.isEmpty
                            ? 'Please enter your password'
                            : null,
                        decoration: InputDecoration(labelText: 'Password')),
                  ],
                ),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: <Widget>[],
              ),
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
              Container(
                padding: EdgeInsets.only(top: 20),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: <Widget>[
//                    FHFlatButtonTransparent(
//                      onPressed: () =>
//                          Navigator.pushNamed(context, '/forgot-password'),
//                      title: 'Forgot password?',
//                      keepCase: true,
//                    ),
                    FHFlatButton(
                        title: 'Sign In',
                        onPressed: () {
                          _handleSubmitted();
                        })
                  ],
                ),
              )
            ],
          ),
        ),
      ),
    );
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
      : assert(bloc != null),
        assert(selectedExternalProviderFunc != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    final external = bloc.identityProviders.has3rdParty;

    final children = <Widget>[];

    if (external) {
      bloc.identityProviders.externalProviders.forEach((provider) {
        children.add(InkWell(
          mouseCursor: SystemMouseCursors.click,
          child: Image.asset(
              bloc.identityProviders.externalProviderAssets[provider]!),
          onTap: () {
            selectedExternalProviderFunc(provider);
          },
        ));
        children.add(Padding(
          padding: const EdgeInsets.fromLTRB(0, 16, 0, 10),
          child: Text('or login with a username and password',
              style: Theme.of(context).textTheme.caption),
        ));
      });
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: children,
    );
  }
}
