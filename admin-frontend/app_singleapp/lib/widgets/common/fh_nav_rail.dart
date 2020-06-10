import 'package:app_singleapp/api/client_api.dart';
import 'package:flutter/material.dart';

import 'fh_circle_icon_button.dart';

class NavRail extends StatelessWidget {
  final ManagementRepositoryClientBloc mrBloc;

  const NavRail({Key key, @required this.mrBloc}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    return Container(
      child: Padding(
        padding: const EdgeInsets.only(left: 8.0, top: 16.0),
        child: CircleIconButton(
            icon: Icon(Icons.chevron_right),
            onTap: () => mrBloc.menuOpened.add(true)),
      ),
    );
  }
}
