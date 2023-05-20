import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_font_icons/flutter_font_icons.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/version.dart';
import 'package:open_admin_app/widget_creator.dart';
import 'package:open_admin_app/widgets/common/fh_person_avatar.dart';
import 'package:open_admin_app/widgets/dynamic-theme/fh_dynamic_theme.dart';
import 'package:open_admin_app/widgets/stepper/stepper_container.dart';

class FHappBar extends StatelessWidget {
  const FHappBar({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);
    bool isWide = MediaQuery.of(context).size.width > 815;

    return AppBar(
      leading: Builder(
        builder: (BuildContext context) {
          return StreamBuilder<bool>(
              stream: mrBloc.menuOpened,
              builder: (context, snapshot) {
                if (snapshot.hasData && snapshot.data == true) {
                  return IconButton(
                      splashRadius: 20,
                      icon: const Icon(SimpleLineIcons.menu, size: 20.0),
                      onPressed: () => mrBloc.menuOpened.add(false));
                } else {
                  return IconButton(
                      splashRadius: 20,
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
            child: isWide
                ? ( Theme.of(context).brightness == Brightness.light ?
            Image.asset('assets/logo/FeatureHub-primary-navy.png') : Image.asset('assets/logo/FeatureHubPrimaryWhite.png'))
                : Image.asset(
                    'assets/logo/FeatureHub-icon.png',
                  ),
          ),
          if (appVersion != 'main') Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: Text(
              ' (v$appVersion)',
              style: const TextStyle(fontSize: 10.0),
            ),
          )
        ],
      ),
      actions: <Widget>[
        widgetCreator.orgNameContainer(mrBloc),
        const SizedBox(width: 8.0),
        StreamBuilder<Person>(
            stream: mrBloc.personStream,
            builder: (BuildContext context, AsyncSnapshot<Person> snapshot) {
              if (snapshot.hasData && mrBloc.isLoggedIn) {
                final person = snapshot.data!;
                var light = Theme.of(context).brightness == Brightness.light;
                return ExcludeFocus(
                  excluding: true, // prevent tabbing to the app bar
                  child: Row(
                    children: [
                      Tooltip(
                          message: "${person.name} \n"
                              "${person.email}",
                          child: PersonAvatar(person: person)),
                      if (isWide) const SizedBox(
                        width: 32.0,
                      ),
                      widgetCreator.externalDocsLinksWidget(),
                      if (isWide) const SizedBox(
                        width: 16.0,
                      ),
                      IconButton(
                          // splashRadius: 20,
                          tooltip: light ? 'Dark mode' : 'Light mode',
                          color: Theme.of(context).colorScheme.primary,
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
                          color: Theme.of(context).colorScheme.primary,
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
