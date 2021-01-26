import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/stepper/stepper_container.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:dynamic_theme/dynamic_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';

class FHappBar extends StatelessWidget {
  const FHappBar({Key key}) : super(key: key);

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
                  return Container(
                    child: IconButton(
                        icon: Icon(SimpleLineIcons.menu, size: 20.0),
                        onPressed: () => mrBloc.menuOpened.add(false)),
                  );
                } else {
                  return Container(
                    child: IconButton(
                        icon: Icon(SimpleLineIcons.menu, size: 20.0),
                        onPressed: () => mrBloc.menuOpened.add(true)),
                  );
                }
              });
        },
      ),
      titleSpacing: 0.0,
      title: Row(
        children: [
          SizedBox(
            height: kToolbarHeight - 20,
            child: MediaQuery.of(context).size.width > 500
                ? Image.asset('assets/logo/FeatureHubPrimaryWhite.png')
                : Image.asset(
                    'assets/logo/FeatureHub-icon.png',
                  ),
          ),
        ],
      ),
      actions: <Widget>[
        StreamBuilder<Person>(
            stream: mrBloc.personStream,
            builder: (BuildContext context, AsyncSnapshot<Person> snapshot) {
//            if (snapshot.hasData &&
//                snapshot.data == InitializedCheckState.logged_in) {
              if (snapshot.hasData) {
                final person = snapshot.data;
                return Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Row(
                    children: [
                      Text(
                          //here the name should be returned from a current user
                          'Hi, ${person.name}',
                          style: Theme.of(context).primaryTextTheme.bodyText2),
                      SizedBox(
                        width: 32.0,
                      ),
                      VerticalDivider(
                        width: 1.0,
                        color: Theme.of(context).cardColor,
                      ),
                      SizedBox(
                        width: 16.0,
                      ),
                      IconButton(icon: Icon(Icons.nightlight_round), onPressed: () {
                DynamicTheme.of(context).setBrightness(Theme.of(context).brightness ==
                    Brightness.dark? Brightness.light: Brightness.dark);
                }),
                      StepperRocketButton(mrBloc: mrBloc),
                      IconButton(
                          onPressed: () async {
                            await mrBloc.logout();
                            ManagementRepositoryClientBloc.router
                                .navigateTo(context, '/');
                          },
                          icon: Icon(Icons.exit_to_app),
                          tooltip: 'Sign out'),
                    ],
                  ),
                );
              } else {
                return SizedBox.shrink();
              }
            })
      ],
    );
  }
}
