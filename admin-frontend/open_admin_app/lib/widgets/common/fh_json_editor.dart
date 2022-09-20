import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';

class FHJsonEditorWidget extends StatefulWidget {
  final TextEditingController controller;
  final GlobalKey<FormState> formKey;
  final bool onlyJsonValidation;

  const FHJsonEditorWidget({Key? key, required this.controller, required this.formKey, required this.onlyJsonValidation})
      : super(key: key);

  @override
  _FHJsonEditorState createState() => _FHJsonEditorState();
}

class _FHJsonEditorState extends State<FHJsonEditorWidget> {
  late bool isJsonOn;

  @override
  void initState() {
    super.initState();

    String? checkJson(value){
      try {
        json.decode(value);
      } catch (e) {
        return e.toString();
      }
      return null;
    }

    isJsonOn = widget.onlyJsonValidation;
    widget.controller.addListener(() {
      String? looksLikeJson = checkJson(widget.controller.text);
      if(looksLikeJson == null && mounted){
        setState(() {
          isJsonOn = true;
        });

      }
    });
  }


  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 500,
      child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(mainAxisAlignment: MainAxisAlignment.end, children: [
              if(!widget.onlyJsonValidation) Expanded(
                child: CheckboxListTile(
                  dense: true,
                  title: const Text("Enable JSON validation"),
                  value: isJsonOn,
                  controlAffinity: ListTileControlAffinity.leading,
                  onChanged: (newValue) {
                    setState(() {isJsonOn = newValue!; });
                    },
                ),
              ),
              if(isJsonOn) FHFlatButtonTransparent(
                onPressed: () {
                  setState(() {
                    final jsonMap = json.decode(widget.controller.text);
                    const encoder = JsonEncoder.withIndent('  ');
                    widget.controller.text = encoder.convert(jsonMap);
                  });
                },
                title: 'Format json',
                keepCase: true,
              ),
            ]),
            if(isJsonOn) Text(
              'JSON Value',
              textAlign: TextAlign.left,
              style: Theme.of(context).textTheme.caption,
            ),
            Expanded(
              child: Form(
                key: widget.formKey,
                autovalidateMode: AutovalidateMode.always,
                child: Container(
                  width: 600,
                  padding: const EdgeInsets.all(10),
                  color: Theme.of(context).scaffoldBackgroundColor,
                  child: TextFormField(
                    style: const TextStyle(fontFamily: 'SourceCodePro', fontSize: 14),
                    maxLines: 20,
                    controller: widget.controller,
                    validator: (value) {
                      if (isJsonOn) {
                        if (value == null || value.isEmpty) {
                          return null;
                        }
                        try {
                          json.decode(value);
                        } catch (e) {
                          return e.toString();
                        }
                      }
                      return null;
                    }),
                ),
              ),
            ),
          ]),
    );
  }
}
