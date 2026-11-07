import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';

/// the placeholder the server sends back instead of an encrypted value
const encryptedTextPlaceholder = 'ENCRYPTED-TEXT';

/// A text field for a system config value that the server stores encrypted. When a value has been stored, the
/// server only ever returns [encryptedTextPlaceholder], so the field is read only and offers a "Clear" action
/// (and "Show value" if [onDecrypt] is provided).
///
/// [field.value] is kept up to date as the user types, so the value survives the parent rebuilding (for example
/// when another control on the same form changes).
class SystemConfigEncryptableTextField extends StatefulWidget {
  final SystemConfig field;
  final InputDecoration? decoration;
  final FormFieldValidator<String>? validator;

  /// called after the user has cleared a stored (encrypted) value, so the parent can react (e.g. disable a feature
  /// that requires this value)
  final VoidCallback? onCleared;

  /// if provided, a "Show value" action is offered for stored values, and this is called to fetch the decrypted value
  final Future<String?> Function()? onDecrypt;

  const SystemConfigEncryptableTextField(
      {super.key,
      required this.field,
      this.decoration,
      this.validator,
      this.onCleared,
      this.onDecrypt});

  @override
  State<StatefulWidget> createState() {
    return SystemConfigEncryptableTextFieldState();
  }
}

class SystemConfigEncryptableTextFieldState
    extends State<SystemConfigEncryptableTextField> {
  final TextEditingController _textField = TextEditingController();

  bool get _isEncrypted => _textField.text == encryptedTextPlaceholder;

  @override
  void initState() {
    super.initState();

    _textField.text = widget.field.value ?? '';
  }

  @override
  void didUpdateWidget(SystemConfigEncryptableTextField oldWidget) {
    super.didUpdateWidget(oldWidget);

    // only reload when we have been handed a different config (e.g. after a save + refresh). A rebuild of the
    // parent with the same config must not wipe what the user has typed.
    if (!identical(oldWidget.field, widget.field)) {
      // changing the controller notifies the enclosing Form, which must not happen while we are being built
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          setState(() {
            _textField.text = widget.field.value ?? '';
          });
        }
      });
    }
  }

  @override
  void dispose() {
    _textField.dispose();
    super.dispose();
  }

  String? _normalise(String? val) =>
      (val == null || val.trim().isEmpty) ? null : val;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Row(
      children: [
        Expanded(
            child: TextFormField(
          controller: _textField,
          readOnly: _isEncrypted,
          textInputAction: TextInputAction.next,
          obscuringCharacter: '*',
          autofocus: true,
          autocorrect: false,
          validator: widget.validator,
          onChanged: (val) {
            widget.field.value = _normalise(val);
          },
          onSaved: (val) {
            widget.field.value = _normalise(val);
          },
          decoration: widget.decoration,
        )),
        if (_isEncrypted)
          Padding(
            padding: const EdgeInsets.only(left: 8.0, right: 8.0),
            child: TextButton(
              onPressed: () {
                setState(() {
                  _textField.text = '';
                  widget.field.value = null;
                });
                widget.onCleared?.call();
              },
              child: Text(l10n.clearAction),
            ),
          ),
        if (_isEncrypted && widget.onDecrypt != null)
          TextButton(
              onPressed: () async {
                final val = await widget.onDecrypt!();
                if (val != null && mounted) {
                  setState(() {
                    _textField.text = val;
                    widget.field.value = val;
                  });
                }
              },
              child: Text(l10n.showValue)),
      ],
    );
  }
}
