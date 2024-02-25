import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class SystemConfigTextField extends StatefulWidget {
  final SystemConfig field;
  final InputDecoration? decoration;
  final FormFieldValidator<String>? validator;

  const SystemConfigTextField(
      {super.key,
        required this.field,
        this.decoration,
        this.validator});

  @override
  State<StatefulWidget> createState() {
    return SystemConfigTextFieldState();
  }
}

class SystemConfigTextFieldState extends State<SystemConfigTextField> {
  final TextEditingController _textField = TextEditingController();

  @override
  void initState() {
    super.initState();

    _textField.text = widget.field.value ?? '';
  }

  @override
  void didUpdateWidget(SystemConfigTextField oldWidget) {
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
    return Row(
      children: [
        Expanded(
            child: TextFormField(
              controller: _textField,
              textInputAction: TextInputAction.next,
              autofocus: true,
              validator: widget.validator,
              autocorrect: false,
              onSaved: (val) => widget.field.value = val,
              decoration: widget.decoration,
            )),
      ],
    );
  }
}
