import 'package:flutter/material.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/decorations/fh_page_divider.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/setup/setup_bloc.dart';
import 'package:open_admin_app/widgets/user/signin/signin_provider_button.dart';
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
  final _passwordScoreThreshold = 1;

  double? _passwordScore;
  Color _passwordStrengthColor = Colors.white;

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
    double? score;
    Color stateColor;

    if (_pw1.text.isEmpty) {
      stateColor = Colors.white;
    } else {
      final result = Zxcvbn().evaluate(_pw1.text);
      score = result.score;
      stateColor =
          (result.score == null || result.score! < _passwordScoreThreshold)
              ? Colors.red
              : Colors.green;
      if (result.score != null && result.score! < 2) {
        stateColor = Colors.orange;
      }
    }

    setState(() {
      _passwordScore = score;
      _passwordStrengthColor = stateColor;
    });
  }

  Widget _dataEntry(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final external = widget.bloc.has3rdParty;
    final local = widget.bloc.hasLocal;

    return Form(
      key: _formKey,
      child: FHCardWidget(
        child: Column(
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
              l10n.setupWelcomeTitle,
              style: Theme.of(context).textTheme.titleLarge,
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(0, 16, 0, 10),
              child: Text(
                  l10n.setupWelcomeMessage,
                  style: Theme.of(context).textTheme.bodyLarge),
            ),
            if (external)
              _SetupPage1ThirdPartyProviders(
                bloc: widget.bloc,
                selectedExternalProviderFunc: _handleSelectedExternal,
              ),
            if (external && local)
              Column(
                children: [
                  const SizedBox(height: 24.0),
                  const FHPageDivider(),
                  Padding(
                      padding: const EdgeInsets.fromLTRB(0, 24, 0, 16),
                      child: Text(l10n.setupOrRegisterBelow,
                          style: Theme.of(context).textTheme.bodySmall)),
                ],
              ),
            if (local)
              Column(mainAxisAlignment: MainAxisAlignment.start, children: <
                  Widget>[
                TextFormField(
                  controller: _name,
                  autofocus: true,
                  decoration: InputDecoration(labelText: l10n.nameLabel),
                  textInputAction: TextInputAction.next,
                  onFieldSubmitted: (_) => _handleSubmitted(),
                  validator: (v) => (v == null || v.isEmpty)
                      ? l10n.nameRequired
                      : null,
                ),
                TextFormField(
                    controller: _email,
                    decoration:
                        InputDecoration(labelText: l10n.emailLabel),
                    textInputAction: TextInputAction.next,
                    onFieldSubmitted: (_) => _handleSubmitted(),
                    validator: (v) {
                      if (v == null || v.isEmpty) {
                        return l10n.emailRequired;
                      }
                      if (!validateEmail(v)) {
                        return l10n.invalidEmailAddress;
                      }
                      return null;
                    }),
                TextFormField(
                    controller: _pw1,
                    obscureText: true,
                    textInputAction: TextInputAction.next,
                    onFieldSubmitted: (_) => _handleSubmitted(),
                    decoration: InputDecoration(labelText: l10n.passwordLabel),
                    validator: (v) {
                      if (v == null || v.isEmpty) {
                        return l10n.passwordRequired;
                      }
                      if (v.length < 7) {
                        return l10n.passwordMustBe7Chars;
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
                    child: _buildPasswordStrengthText(context, l10n),
                  ),
                ),
                TextFormField(
                    controller: _pw2,
                    obscureText: true,
                    onFieldSubmitted: (_) => _handleSubmitted(),
                    decoration:
                        InputDecoration(labelText: l10n.confirmPasswordLabel),
                    validator: (v) {
                      if (v != _pw1.text) {
                        return l10n.passwordsDoNotMatch;
                      }
                      return null;
                    }),
              ]),
            if (local)
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: <Widget>[
                  Padding(
                    padding: const EdgeInsets.only(top: 16.0),
                    child: FHFlatButton(
                      onPressed: () => _handleSubmitted(),
                      title: l10n.next,
                    ),
                  )
                ],
              )
          ],
        ),
      ),
    );
  }

  Widget _buildPasswordStrengthText(BuildContext context, AppLocalizations l10n) {
    if (_passwordScore == null && _pw1.text.isEmpty) {
      return const SizedBox.shrink();
    }
    String label;
    if (_passwordScore == null || _passwordScore! < 1) {
      label = l10n.passwordStrengthWeak;
    } else if (_passwordScore! < 2) {
      label = l10n.passwordStrengthBelowAverage;
    } else if (_passwordScore! < 3) {
      label = l10n.passwordStrengthGood;
    } else {
      label = l10n.passwordStrengthStrong;
    }
    return Text(label,
        style: Theme.of(context).textTheme.bodySmall!.copyWith(color: _passwordStrengthColor));
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
    return Column(
      children: [
        for (dynamic provider in bloc.externalProviders)
          Padding(
            padding: const EdgeInsets.only(top: 12.0),
            child: SignInProviderButton(
                provider: provider,
                providedIcon: bloc.identityInfo(provider),
                func: () => selectedExternalProviderFunc(provider)),
          ),
      ],
    );
  }
}
