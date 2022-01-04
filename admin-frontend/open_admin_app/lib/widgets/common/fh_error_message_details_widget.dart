import 'package:flutter/material.dart';
import 'package:open_admin_app/utils/custom_scroll_behavior.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/copy_to_clipboard_html.dart';

class FHErrorMessageDetailsWidget extends StatefulWidget {
  final FHError fhError;

  const FHErrorMessageDetailsWidget({Key? key, required this.fhError})
      : super(key: key);

  @override
  State<FHErrorMessageDetailsWidget> createState() =>
      _FHErrorMessageDetailsWidgetState();
}

class _FHErrorMessageDetailsWidgetState
    extends State<FHErrorMessageDetailsWidget> {
  bool showDetails = false;
  String showDetailsButton = 'View details';

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Visibility(
            visible: widget.fhError.showDetails,
            child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
              TextButton(
                  onPressed: () {
                    setState(() {
                      showDetails = !showDetails;
                      showDetailsButton =
                          showDetails ? 'Hide details' : 'View details';
                    });
                  },
                  child: Text(showDetailsButton)),
              Visibility(
                visible: showDetails,
                child: FHErrorDetailsWidget(error: widget.fhError),
              )
            ]))
      ],
    );
  }
}

class FHErrorDetailsWidget extends StatelessWidget {
  final FHError error;

  const FHErrorDetailsWidget({Key? key, required this.error}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final ScrollController controller = ScrollController();
    return Column(
      children: <Widget>[
        Container(
          constraints:
              BoxConstraints(maxHeight: MediaQuery.of(context).size.height / 3),
          child: ScrollConfiguration(
            behavior: CustomScrollBehavior(),
            child: SingleChildScrollView(
              controller: controller,
              child: Text(
                '${error.exception.toString()}+\n\n${error.stackTrace.toString()}',
                style:
                    const TextStyle(fontFamily: 'SourceCodePro', fontSize: 12),
              ),
            ),
          ),
        ),
        FHCopyToClipboardFlatButton(
            caption: ' Copy error details to clipboard',
            text:
                '${error.exception.toString()} Stack trace: ${error.stackTrace.toString()}')
      ],
    );
  }
}
