import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_card.dart';
import 'package:app_singleapp/widgets/setup/setup_bloc.dart';
import 'package:flutter/material.dart';
import 'package:xcvbnm/xcvbnm.dart';

class SetupPage1Widget extends StatefulWidget {
  final SetupBloc bloc;

  const SetupPage1Widget({Key key, @required this.bloc})
      : assert(bloc != null),
        super(key: key);

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
    final result = Xcvbnm().estimate(_pw1.text);
    var state = 'Weak';
    if (result.score == 1) {
      state = 'Below average';
    } else if (result.score == 2) {
      state = 'Good';
    } else if (result.score == 3) {
      state = 'Strong';
    }
    Color stateColor =
        result.score < _PASSWORD_SCORE_THRESHOLD ? Colors.red : Colors.green;
    if (result.score == 1) {
      stateColor = Colors.orange;
    }
    final stateText = Text(state,
        style: Theme.of(context).textTheme.caption.copyWith(color: stateColor));
    setState(() {
      _passwordStrength = stateText;
    });
  }

  Widget _dataEntry(BuildContext context) {
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
                  "Well done, FeatureHub is up and running.  You\'ll be the first 'Site administrator' of your FeatureHub account, lets get a few details.",
                  style: Theme.of(context).textTheme.bodyText1),
            ),
            TextFormField(
              controller: _name,
              autofocus: true,
              decoration: InputDecoration(labelText: 'Name'),
              textInputAction: TextInputAction.next,
              onFieldSubmitted: (_) => _handleSubmitted(),
              validator: (v) => v.isEmpty ? 'Please enter your name' : null,
            ),
            TextFormField(
                controller: _email,
                decoration: InputDecoration(labelText: 'Email address'),
                textInputAction: TextInputAction.next,
                onFieldSubmitted: (_) => _handleSubmitted(),
                validator: (v) {
                  if (v.isEmpty) {
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
                  if (v.isEmpty) {
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
            Padding(
              padding: const EdgeInsets.fromLTRB(0, 5, 0, 0),
              child: _passwordStrength,
            ),
            TextFormField(
                controller: _pw2,
                obscureText: true,
                onFieldSubmitted: (_) => _handleSubmitted(),
                textInputAction: TextInputAction.next,
                decoration: InputDecoration(labelText: 'Confirm Password'),
                validator: (v) {
                  if (v != _pw1.text) {
                    return "Passwords don't match";
                  }
                  return null;
                }),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: <Widget>[
                FHFlatButton(
                  onPressed: () => _handleSubmitted(),
                  title: 'Next',
                )
              ],
            )
          ],
        ),
      ),
    );
  }

  void _handleSubmitted() {
    if (_formKey.currentState.validate()) {
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
