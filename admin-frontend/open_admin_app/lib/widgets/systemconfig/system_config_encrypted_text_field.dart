import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/systemconfig/systemconfig_bloc.dart';

class SystemConfigEncryptableTextField extends StatefulWidget {
  final SystemConfig field;
  final InputDecoration? decoration;
  final FormFieldValidator<String>? validator;

  const SystemConfigEncryptableTextField(
      {super.key, required this.field, this.decoration, this.validator});

  @override
  State<StatefulWidget> createState() {
    return SystemConfigEncryptableTextFieldState();
  }
}

class SystemConfigEncryptableTextFieldState
    extends State<SystemConfigEncryptableTextField> {
  final TextEditingController _textField = TextEditingController();

  @override
  void initState() {
    super.initState();

    _textField.text = widget.field.value ?? '';
  }

  @override
  void didUpdateWidget(SystemConfigEncryptableTextField oldWidget) {
    super.didUpdateWidget(oldWidget);

    _textField.text = widget.field.value ?? '';
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    _textField.text = widget.field.value ?? '';
  }

  @override
  Widget build(BuildContext context) {
    final SystemConfigBloc configBloc = BlocProvider.of(context);

    return Row(
      children: [
        Expanded(
            child: TextFormField(
          controller: _textField,
          readOnly: _textField.text == 'ENCRYPTED-TEXT',
          textInputAction: TextInputAction.next,
          obscuringCharacter: '*',
          autofocus: true,
          autocorrect: false,
          validator: widget.validator,
          onSaved: (val) {
            if (val?.trim().isEmpty == true) {
              widget.field.value = null;
            } else {
              widget.field.value = val;
            }
          },
          decoration: widget.decoration,
        )),
        if (_textField.text == 'ENCRYPTED-TEXT')
          Padding(
            padding: const EdgeInsets.only(left: 8.0, right: 8.0),
            child: TextButton(
              onPressed: () {
                setState(() {
                  _textField.text = '';
                  widget.field.value = null;
                });
              },
              child: const Text('Clear'),
            ),
          ),
        if (configBloc.mrClient.identityProviders.capabilityWebhookEncryption &&
            configBloc.mrClient.identityProviders.capabilityWebhookDecryption)
          if (_textField.text == 'ENCRYPTED-TEXT')
            TextButton(
                onPressed: () async {
                  final val = await configBloc.systemConfigServiceApi
                      .decryptSystemConfig(widget.field.key);
                  if (val.result != null) {
                    setState(() {
                      _textField.text = val.result!;
                      widget.field.value = val.result;
                    });
                  }
                },
                child: const Text('Show value')),
      ],
    );
  }
}
