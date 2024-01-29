import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/apps/webhook/webhook_env_bloc.dart';

class SlackPanelWidget extends StatefulWidget {
  final WebhookEnvironmentBloc bloc;
  final EnvironmentAndWebhookType env;

  const SlackPanelWidget(this.env, this.bloc, {super.key});

  @override
  State<StatefulWidget> createState() {
    return SlackPanelWidgetState();
  }
}

class SlackPanelWidgetState extends State<SlackPanelWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _token = TextEditingController();
  final TextEditingController _channelName = TextEditingController();
  bool _enabled = false;

  @override
  void initState() {
    super.initState();

    _setup();
  }

  @override
  void didUpdateWidget(SlackPanelWidget oldWidget) {
    super.didUpdateWidget(oldWidget);

    _setup();
  }

  _setup() {
    final env = widget.env.environment?.webhookEnvironmentInfo ?? {};
    final prefix = widget.env.type!.envPrefix;
    _enabled = env['$prefix.enabled'] == 'true';
    _token.text = env['$prefix.token'] ?? '';
    _channelName.text = env['$prefix.channel_name'] ?? '';
  }

  @override
  Widget build(BuildContext context) {
    return Card(
        margin: const EdgeInsets.all(8.0),
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Column(mainAxisAlignment: MainAxisAlignment.start, children: [
            const SizedBox(
              height: 24.0,
            ),
            Row(
              children: [
                Text(
                  'Slack Configuration',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ],
            ),
            const SizedBox(
              height: 16.0,
            ),
            Form(
              key: _formKey,
              child: Column(children: <Widget>[
                Row(
                  children: [
                    const Text('Enabled'),
                    Checkbox(
                      autofocus: true,
                      value: _enabled,
                      onChanged: (value) => setState(() {
                        _enabled = value ?? false;
                      }),
                    )
                  ],
                ),
                const SizedBox(height: 24.0),
                Row(children: [
                  Container(
                      constraints: const BoxConstraints(maxWidth: 600),
                      child: TextFormField(
                          controller: _token,
                          autofocus: true,
                          textInputAction: TextInputAction.next,
                          obscureText: _token.text == 'ENCRYPTED-TEXT',
                          obscuringCharacter: '*',
                          readOnly: _token.text == 'ENCRYPTED-TEXT',
                          decoration: const InputDecoration(
                            border: OutlineInputBorder(),
                            hintText:
                                'e.g. xoxb-1182138673840-5153275439522-sYLjc5KVxFaLrr2wY9fh8jd',
                            labelText: 'Slack Bot User OAuth Token',
                          ),
                          validator: ((v) {
                            if (v == null || v.isEmpty) {
                              return 'Please enter Slack Bot User OAuth Token';
                            }
                            return null;
                          }))),
                  const SizedBox(width: 8.0),
                  if (widget.bloc.mrBloc.identityProviders
                          .capabilityWebhookEncryption &&
                      widget.bloc.mrBloc.identityProviders
                          .capabilityWebhookDecryption)
                    if (_token.text == 'ENCRYPTED-TEXT')
                      TextButton(
                          onPressed: () => widget.bloc
                              .decryptEncryptedFields(widget.env.environment!),
                          child: const Text('Reveal hidden values')),
                  TextButton(
                    onPressed: () {
                      setState(() {
                        _token.text = '';
                      });
                    },
                    child: const Text('Reset'),
                  )
                ]),
                const SizedBox(height: 36.0),
                Row(children: [
                  Container(
                      constraints: const BoxConstraints(maxWidth: 600),
                      child: TextFormField(
                          controller: _channelName,
                          autofocus: true,
                          textInputAction: TextInputAction.next,
                          decoration: const InputDecoration(
                            border: OutlineInputBorder(),
                            hintText: 'e.g. C0150T7AF25',
                            labelText: 'Slack channel ID',
                          ),
                          validator: ((v) {
                            if (v == null || v.isEmpty) {
                              return 'Please enter a Slack channel ID';
                            }
                            return null;
                          }))),
                ]),
                const SizedBox(
                  height: 24.0,
                ),
                const SizedBox(width: 16.0),
                Row(
                  children: [
                    FilledButton(
                        onPressed: () async {
                          if (_formKey.currentState!.validate()) {
                            _formKey.currentState!.save();
                            await _updateData();
                          }
                        },
                        child: const Text('Save')),
                  ],
                )
              ]),
            )
          ]),
        ));
  }

  Future<void> _updateData() async {
    final env = widget.env.environment?.webhookEnvironmentInfo ?? {};
    final prefix = widget.env.type!.envPrefix;
    env['$prefix.enabled'] = _enabled.toString();
    env['$prefix.token'] = _token.text;
    env['$prefix.channel_name'] = _channelName.text;
    env['$prefix.encrypt'] = '$prefix.token';
    widget.bloc.mrBloc.addSnackbar(const Text("Saving Slack webhook"));
    await widget.bloc.updateEnvironmentWithWebhookData(
        widget.env.environment!, env, "$prefix.");
  }
}
