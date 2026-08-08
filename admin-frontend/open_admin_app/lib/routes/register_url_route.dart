import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/utils/password_policy_validator.dart';
import 'package:open_admin_app/widgets/common/fh_card.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/password_requirements_widget.dart';
import 'package:open_admin_app/widgets/user/register/register_url_bloc.dart';
import 'package:openapi_dart_common/openapi.dart';
import 'package:zxcvbn/zxcvbn.dart';

class RegisterURLRoute extends StatefulWidget {
  final String token;

  const RegisterURLRoute(this.token, {super.key});

  @override
  State<StatefulWidget> createState() {
    return RegisterURLState();
  }
}

const _passwordScoreThreshold = 1;

class RegisterURLState extends State<RegisterURLRoute> {
  final _formKey = GlobalKey<FormState>(debugLabel: 'registration url');
  final _name = TextEditingController();
  final _pw1 = TextEditingController();
  final _pw2 = TextEditingController();
  Text _passwordStrength = const Text('');

  /// Populated when the server rejects a password this form accepted - the server is authoritative.
  List<PasswordPolicyRule>? _serverRejectedRules;

  @override
  Widget build(BuildContext context) {
    var bloc = BlocProvider.of<RegisterBloc>(context);

    return FHCardWidget(
        child: Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: <Widget>[
        SizedBox(
            width: 500,
            child: StreamBuilder(
                stream: bloc.formState,
                builder: (context, snapshot) {
                  if (snapshot.hasData) {
                    if (snapshot.data == RegisterUrlForm.initialState) {
                      return initialState(context, bloc);
                    } else if (snapshot.data == RegisterUrlForm.successState) {
                      ManagementRepositoryClientBloc.router
                          .navigateTo(context, '/');
                      return const SizedBox.shrink();
                    } else if (snapshot.data ==
                        RegisterUrlForm.alreadyLoggedIn) {
                      ManagementRepositoryClientBloc.router
                          .navigateRoute('/login');
                      return const SizedBox.shrink();
                    }
                  }
                  if (snapshot.hasError) {
                    final l10n = AppLocalizations.of(context)!;
                    String humanErrorMessage = l10n.registerUrlUnexpectedError;

                    if (snapshot.error is ApiException &&
                        !snapshot.error.toString().contains('500')) {
                      humanErrorMessage = l10n.registerUrlExpiredOrInvalid;
                    }
                    return Text(humanErrorMessage);
                  }
                  return Text(AppLocalizations.of(context)!.validatingInvitationUrl);
                })),
      ],
    ));
  }

  Widget initialState(BuildContext context, RegisterBloc bloc) {
    final l10n = AppLocalizations.of(context)!;
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Text(
            l10n.welcomeToFeatureHub,
            style: Theme.of(context).textTheme.titleLarge,
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(0, 10, 0, 10),
            child: Text(l10n.registerCompleteDetails,
                style: Theme.of(context).textTheme.titleSmall),
          ),
          TextFormField(
            enabled: false,
            decoration: InputDecoration(labelText: l10n.emailLabel),
            initialValue: bloc.person!.email,
          ),
          TextFormField(
            controller: _name,
            autofocus: true,
            decoration: InputDecoration(labelText: l10n.nameLabel),
            textInputAction: TextInputAction.next,
            validator: (v) =>
                v?.isEmpty == false ? null : l10n.nameRequired,
          ),
          TextFormField(
              controller: _pw1,
              obscureText: true,
              textInputAction: TextInputAction.next,
              decoration: InputDecoration(labelText: l10n.passwordLabel),
              onChanged: (_) => setPasswordStrength(l10n),
              validator: (v) {
                if (v == null || v.isEmpty) {
                  return l10n.passwordRequired;
                }
                final policyFailure = passwordValidationMessage(
                    v, bloc.mrClient.passwordPolicy, l10n);
                if (policyFailure != null) {
                  return policyFailure;
                }
                if (_pw2.text.isNotEmpty && v != _pw2.text) {
                  return l10n.passwordsDoNotMatch;
                }
                return null;
              }),
          Padding(
            padding: const EdgeInsets.fromLTRB(0, 8, 0, 4),
            child: PasswordRequirementsWidget(
              policy: bloc.mrClient.passwordPolicy,
              password: _pw1.text,
              serverRejectedRules: _serverRejectedRules,
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(0, 5, 0, 10),
            child: _passwordStrength,
          ),
          TextFormField(
              controller: _pw2,
              obscureText: true,
              autovalidateMode: AutovalidateMode.onUserInteraction,
              textInputAction: TextInputAction.next,
              decoration: InputDecoration(labelText: l10n.confirmPasswordLabel),
              validator: (v) {
                if (v == null || v.isEmpty) {
                  return l10n.confirmPasswordRequired;
                }
                if (v != _pw1.text) {
                  return l10n.passwordsDoNotMatch;
                }
                return null;
              }),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: <Widget>[
              Padding(
                padding: const EdgeInsets.only(top: 16.0),
                child: FHFlatButton(
                    onPressed: () async {
                      if (_formKey.currentState!.validate()) {
                        final rejected = await bloc.completeRegistration(
                            widget.token,
                            bloc.person!.email!,
                            _name.text,
                            _pw1.text,
                            _pw2.text);

                        if (!context.mounted) return;

                        if (rejected != null) {
                          // the server disagreed with us about the password - it wins
                          setState(() => _serverRejectedRules = rejected);
                          return;
                        }

                        setState(() => _serverRejectedRules = null);
                        ManagementRepositoryClientBloc.router
                            .navigateTo(context, '/');
                      }
                    },
                    title: l10n.registerButton),
              )
            ],
          )
        ],
      ),
    );
  }

  /// The strength meter is advisory only - what is actually enforced comes from the password
  /// policy, which never touches zxcvbn. zxcvbn throws a RangeError on an empty password (it
  /// unwinds its scoring table from index length-1) and has a history of similar failures on other
  /// inputs, so any failure here hides the meter instead of taking the app down.
  ///
  /// setState runs either way, so the requirements checklist re-renders as the field changes.
  void setPasswordStrength(AppLocalizations l10n) {
    Text strength = const Text('');

    if (_pw1.text.isNotEmpty) {
      try {
        final result = Zxcvbn().evaluate(_pw1.text);

        var state = l10n.passwordStrengthWeak;
        if (result.score == 1) {
          state = l10n.passwordStrengthBelowAverage;
        } else if (result.score == 2) {
          state = l10n.passwordStrengthGood;
        } else if (result.score == 3) {
          state = l10n.passwordStrengthStrong;
        }
        Color stateColor =
            (result.score == null || result.score! < _passwordScoreThreshold)
                ? Colors.red
                : Colors.green;

        if (result.score == 1) {
          stateColor = Colors.orange;
        }

        strength = Text(state, style: TextStyle(color: stateColor));
      } catch (e) {
        // Leave the meter hidden - the enforced rules are shown by the requirements checklist.
      }
    }

    setState(() {
      _passwordStrength = strength;
    });
  }
}
