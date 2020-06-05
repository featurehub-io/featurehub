import 'package:app_singleapp/api/client_api.dart';
import 'package:flutter/material.dart';

import 'fh_icon_button.dart';

class NavRail extends StatelessWidget {
  final ManagementRepositoryClientBloc mrBloc;

  const NavRail({Key key, @required this.mrBloc}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    return Container(child: FHIconButton(icon: Icon(Icons.chevron_right), onPressed: () => mrBloc.menuOpened.add(true)),);
  }
}
