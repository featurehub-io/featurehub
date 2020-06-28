import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/stepper/stepper_container.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';

class FHappBar extends StatelessWidget {
  const FHappBar({Key key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final mrBloc = BlocProvider.of<ManagementRepositoryClientBloc>(context);

    return AppBar(
      title: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: <Widget>[
          StreamBuilder<bool>(
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
              }),
          Image.asset('assets/logo/FeatureHubPrimaryWhite.png',
              width: 150, height: 150),
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
                        color: Colors.white,
                      ),
                      SizedBox(
                        width: 16.0,
                      ),
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
