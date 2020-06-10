import 'dart:convert';

import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:flutter/material.dart';

class FHJsonEditorWidget extends StatefulWidget {
  final TextEditingController controller;

  const FHJsonEditorWidget({Key key, this.controller}) : super(key: key);

  @override
  _FHJsonEditorState createState() => _FHJsonEditorState();
}

class _FHJsonEditorState extends State<FHJsonEditorWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();

  Widget build(BuildContext context) {
    return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(mainAxisAlignment: MainAxisAlignment.end, children: [
            FHFlatButtonTransparent(
                onPressed: () {
                  setState(() {
                    var jsonMap = json.decode(widget.controller.text);
                    var encoder = JsonEncoder.withIndent('  ');
                    widget.controller.text = encoder.convert(jsonMap);
                  });
                },
                title: 'Format json'),
          ]),
          Container(
            child: Text(
              'JSON Value',
              textAlign: TextAlign.left,
              style: Theme.of(context).textTheme.caption,
            ),
          ),
          Form(
            key: _formKey,
            autovalidate: true,
            child: Container(
              padding: EdgeInsets.all(10),
              width: 600,
              color: Theme.of(context).scaffoldBackgroundColor,
              child: TextFormField(
                style: TextStyle(fontFamily: 'source', fontSize: 13),
                maxLines: 20,
                controller: widget.controller,
                validator: validateJson,
              ),
            ),
          ),
        ]);
  }
}
