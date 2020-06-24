import 'dart:js' as js;

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
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
                        icon: Icon(Icons.menu, size: 24.0),
                        onPressed: () => mrBloc.menuOpened.add(false)),
                  );
                } else {
                  return Container(
                    child: IconButton(
                        icon: Icon(Icons.menu, size: 24.0),
                        onPressed: () => mrBloc.menuOpened.add(true)),
                  );
                }
              }),
          Image.asset('FeatureHubPrimaryWhite.png', width: 150, height: 150),
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
                      Padding(
                        padding: const EdgeInsets.only(left: 16.0),
                        child: IconButton(
                            onPressed: () => _logout(context, mrBloc),
                            icon: Icon(Icons.exit_to_app),
                            tooltip: 'Logout'),
                      ),
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

void _logout(BuildContext context, ManagementRepositoryClientBloc client) {
  client.logout().then((result) {
    // the better way to do this is probably to reload the main app.
    js.context['location']['href'] = '/';
  }).catchError((e, s) => client.dialogError(e, s));
}
