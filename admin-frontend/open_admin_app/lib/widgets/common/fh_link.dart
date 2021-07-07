import 'package:open_admin_app/api/client_api.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

import '../../utils/translate_on_hover.dart';

class FHLinkWidget extends StatelessWidget {
  final String href;
  final String text;

  const FHLinkWidget({
    Key? key,
    required this.text,
    required this.href,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return InkWell(
      mouseCursor: SystemMouseCursors.click,
      onTap: () {
        ManagementRepositoryClientBloc.router
            .navigateTo(context, href, params: {});
      },
      child: Container(
        alignment: Alignment.centerLeft,
        child: TranslateOnHover(
          child: Text(text,
              style: Theme.of(context)
                  .textTheme
                  .button!
                  .merge(TextStyle(color: Theme.of(context).buttonColor))),
        ),
      ),
    );
  }
}

class FHLink extends StatelessWidget {
  final Map<String, List<String>>? params;
  final String href;
  final String? tooltip;
  final Widget child;

  FHLink(
      {Key? key,
      required this.href,
      this.tooltip,
      required this.child,
      this.params})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        ManagementRepositoryClientBloc.router
            .navigateTo(context, href, params: params);
      },
      child: Container(
        alignment: Alignment.centerLeft,
        child:
            tooltip != null ? Tooltip(message: tooltip!, child: child) : child,
      ),
    );
  }
}
