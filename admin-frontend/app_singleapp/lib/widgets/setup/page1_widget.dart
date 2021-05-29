import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_card.dart';
import 'package:app_singleapp/widgets/setup/setup_bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:zxcvbn/zxcvbn.dart';

class SetupPage1Widget extends StatefulWidget {
  final SetupBloc bloc;

  const SetupPage1Widget({Key? key, required this.bloc}) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _SetupPage1State();
  }
}

class _SetupPage1State extends State<SetupPage1Widget> {
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _pw1 = TextEditingController();
  final _pw2 = TextEditingController();
  final _PASSWORD_SCORE_THRESHOLD = 1;

  Text _passwordStrength = Text('');

  final _formKey = GlobalKey<FormState>(debugLabel: 'setup_page1');

  @override
  Widget build(BuildContext context) {
    return _dataEntry(context);
  }

  @override
  void initState() {
    super.initState();
    copyIn();
    _pw1.addListener(setPasswordStrength);
  }

  void setPasswordStrength() {
    final result = Zxcvbn().evaluate(_pw1.text);
    var state = 'Weak';
    if (result.score == 1) {
      state = 'Below average';
    } else if (result.score == 2) {
      state = 'Good';
    } else if (result.score == 3) {
      state = 'Strong';
    }
    Color stateColor =
        (result.score == null || result.score! < _PASSWORD_SCORE_THRESHOLD)
            ? Colors.red
            : Colors.green;
    if (result.score == 1) {
      stateColor = Colors.orange;
    }
    final stateText = Text(state,
        style:
            Theme.of(context).textTheme.caption!.copyWith(color: stateColor));
    setState(() {
      _passwordStrength = stateText;
    });
  }

  Widget _dataEntry(BuildContext context) {
    final external = widget.bloc.has3rdParty;
    final local = widget.bloc.hasLocal;

    return Form(
      key: _formKey,
      child: FHCardWidget(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.only(bottom: 26.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Image.asset('assets/logo/FeatureHub-icon.png',
                      width: 40, height: 40),
                ],
              ),
            ),
            Text(
              'Lets get this party started!',
              style: Theme.of(context).textTheme.headline6,
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(0, 10, 0, 10),
              child: Text(
                  "Well done, FeatureHub is up and running.  You\'ll be the first 'Site administrator' of your FeatureHub account.",
                  style: Theme.of(context).textTheme.bodyText1),
            ),
            if (external)
              _SetupPage1ThirdPartyProviders(
                bloc: widget.bloc,
                selectedExternalProviderFunc: _handleSelectedExternal,
              ),
            if (local)
              Column(
                  mainAxisAlignment: MainAxisAlignment.start,
                  children: <Widget>[
                    TextFormField(
                      controller: _name,
                      autofocus: true,
                      decoration: InputDecoration(labelText: 'Name'),
                      textInputAction: TextInputAction.next,
                      onFieldSubmitted: (_) => _handleSubmitted(),
                      validator: (v) => (v == null || v.isEmpty)
                          ? 'Please enter your name'
                          : null,
                    ),
                    TextFormField(
                        controller: _email,
                        decoration: InputDecoration(labelText: 'Email address'),
                        textInputAction: TextInputAction.next,
                        onFieldSubmitted: (_) => _handleSubmitted(),
                        validator: (v) {
                          if (v == null || v.isEmpty) {
                            return 'Please enter your email address';
                          }
                          if (!validateEmail(v)) {
                            return ('Please enter a valid email address');
                          }
                          return null;
                        }),
                    TextFormField(
                        controller: _pw1,
                        obscureText: true,
                        textInputAction: TextInputAction.next,
                        onFieldSubmitted: (_) => _handleSubmitted(),
                        decoration: InputDecoration(labelText: 'Password'),
                        validator: (v) {
                          if (v == null || v.isEmpty) {
                            return 'Please enter your password';
                          }
                          if (v.length < 7) {
                            return 'Password must be at least 7 characters';
                          }
                          //this is quite sensitive and annoying at the moment, commenting out
//                    Result result = Xcvbnm().estimate(v);
//                    if (result.score < _PASSWORD_SCORE_THRESHOLD) {
//                      return 'Password not strong enough, try adding numbers and symbols';
//                    }
                          return null;
                        }),
                    Align(
                      alignment: Alignment.topLeft,
                      child: Padding(
                        padding: const EdgeInsets.fromLTRB(0, 5, 0, 0),
                        child: _passwordStrength,
                      ),
                    ),
                    TextFormField(
                        controller: _pw2,
                        obscureText: true,
                        onFieldSubmitted: (_) => _handleSubmitted(),
                        decoration:
                            InputDecoration(labelText: 'Confirm Password'),
                        validator: (v) {
                          if (v != _pw1.text) {
                            return "Passwords don't match";
                          }
                          return null;
                        }),
                  ]),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: <Widget>[
                  Padding(
                  padding: const EdgeInsets.only(top: 16.0),
                  child: FHFlatButton(
                  onPressed: () => _handleSubmitted(),
                  title: 'Next',
                  ),)
              ],
            )
          ],
        ),
      ),
    );
  }

  void _handleSelectedExternal(String provider) {
    widget.bloc.provider = provider;
    widget.bloc.name = null;
    widget.bloc.email = null;
    widget.bloc.pw1 = null;
    widget.bloc.pw2 = null;
    widget.bloc.nextPage();
  }

  void _handleSubmitted() {
    if (_formKey.currentState!.validate()) {
      copyState();
      widget.bloc.nextPage();
    }
  }

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _pw1.dispose();
    _pw2.dispose();
    super.dispose();
  }

  void copyIn() {
    _name.text = widget.bloc.name ?? '';
    _email.text = widget.bloc.email ?? '';
    _pw1.text = widget.bloc.pw1 ?? '';
    _pw2.text = widget.bloc.pw2 ?? '';
  }

  void copyState() {
    widget.bloc.name = _name.text;
    widget.bloc.email = _email.text;
    widget.bloc.pw1 = _pw1.text;
    widget.bloc.pw2 = _pw2.text;
  }
}

typedef _SelectedExternalFunction = void Function(String provider);

class _SetupPage1ThirdPartyProviders extends StatelessWidget {
  final SetupBloc bloc;
  final _SelectedExternalFunction selectedExternalProviderFunc;

  const _SetupPage1ThirdPartyProviders(
      {Key? key,
      required this.bloc,
      required this.selectedExternalProviderFunc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    final external = bloc.has3rdParty;

    final children = <Widget>[];

    if (external) {
      bloc.externalProviders.forEach((provider) {
        children.add(InkWell(
          mouseCursor: SystemMouseCursors.click,
          onTap: () {
            selectedExternalProviderFunc(provider);
          },
          child: Image.asset(bloc.externalProviderAssets[provider]!),
        ));
        children.add(Padding(
          padding: const EdgeInsets.fromLTRB(0, 16, 0, 10),
          child: Text('or enter your details to register',
              style: Theme.of(context).textTheme.caption),
        ));
      });
    }
    return Column(
      children: children,
    );
  }
}
