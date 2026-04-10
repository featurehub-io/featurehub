import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/common/fh_header.dart';

class PageNotFoundRoute extends StatelessWidget {
  const PageNotFoundRoute({super.key, required this.title});
  final String title;

  @override
  Widget build(BuildContext context) {
    return FHHeader(
      title: AppLocalizations.of(context)!.pageNotFoundMessage,
    );
  }
}
