import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/version.dart';
import 'package:open_admin_app/widgets/dynamic-theme/fh_dynamic_theme.dart';
import 'package:open_admin_app/widgets/stepper/stepper_container.dart';

class FHappBar extends StatelessWidget {
  const FHappBar({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return AppBar(
      leading: Builder(
        builder: (BuildContext context) {
          return StreamBuilder<bool>(
              stream: mrBloc.menuOpened,
              builder: (context, snapshot) {
                if (snapshot.hasData && snapshot.data == true) {
                  return IconButton(
                      icon: const Icon(SimpleLineIcons.menu, size: 20.0),
                      onPressed: () => mrBloc.menuOpened.add(false));
                } else {
                  return IconButton(
                      icon: const Icon(SimpleLineIcons.menu, size: 20.0),
                      onPressed: () => mrBloc.menuOpened.add(true));
                }
              });
        },
      ),
      titleSpacing: 0.0,
      title: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          SizedBox(
            height: kToolbarHeight - 20,
            child: MediaQuery.of(context).size.width > 500
                ? Image.asset('assets/logo/FeatureHubPrimaryWhite.png')
                : Image.asset(
                    'assets/logo/FeatureHub-icon.png',
                  ),
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: Text(
              ' (v$appVersion)',
              style: const TextStyle(fontSize: 10.0),
            ),
          )
        ],
      ),
      actions: <Widget>[
        StreamBuilder<Person>(
            stream: mrBloc.personStream,
            builder: (BuildContext context, AsyncSnapshot<Person> snapshot) {
              if (snapshot.hasData && mrBloc.isLoggedIn) {
                final person = snapshot.data!;
                var light = Theme.of(context).brightness == Brightness.light;
                return Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Row(
                    children: [
                      Text(
                          //here the name should be returned from a current user
                          'Hi, ${person.name}',
                          style: Theme.of(context).primaryTextTheme.bodyText2),
                      const SizedBox(
                        width: 32.0,
                      ),
                      VerticalDivider(
                        width: 1.0,
                        color: Theme.of(context).cardColor,
                      ),
                      const SizedBox(
                        width: 16.0,
                      ),
                      IconButton(
                          tooltip: light ? 'Dark mode' : 'Light mode',
                          icon: Icon(light
                              ? MaterialCommunityIcons.weather_night
                              : Feather.sun),
                          onPressed: () {
                            DynamicTheme.of(context).setBrightness(
                                light ? Brightness.dark : Brightness.light);
                          }),
                      StepperRocketButton(mrBloc: mrBloc),
                      IconButton(
                          onPressed: () async {
                            await mrBloc.logout();
                            ManagementRepositoryClientBloc.router
                                .navigateTo(context, '/');
                          },
                          icon: const Icon(Icons.exit_to_app),
                          tooltip: 'Sign out'),
                    ],
                  ),
                );
              } else {
                return const SizedBox.shrink();
              }
            })
      ],
    );
  }
}
