import 'package:flutter/material.dart';
import 'package:open_admin_app/api/client_api.dart';

import 'fh_circle_icon_button.dart';

class NavRail extends StatelessWidget {
  final ManagementRepositoryClientBloc mrBloc;

  const NavRail({Key? key, required this.mrBloc}) : super(key: key);
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 8.0, top: 16.0),
      child: CircleIconButton(
          icon: const Icon(Icons.menu, size: 20.0),
          onTap: () => mrBloc.menuOpened.add(true)),
    );
  }
}
