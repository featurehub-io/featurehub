import 'package:app_singleapp/api/client_api.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class PersonAvatar extends StatelessWidget {
  final ManagementRepositoryClientBloc mrBloc;

  const PersonAvatar({Key key, this.mrBloc}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<Person>(
        stream: mrBloc.personStream,
        builder: (BuildContext context, AsyncSnapshot<Person> snapshot) {
          if (snapshot.hasData) {
            final person = snapshot.data;
            return Padding(
                padding: const EdgeInsets.all(8.0),
                child: Column(
                  children: [
                    CircleAvatar(
                      backgroundColor: Color(0xffA6F2DE),
                      child: Text(
                        //here the name should be returned from a current user
                          '${person.name.substring(0, 1)}',
                          style: Theme.of(context)
                              .textTheme
                              .bodyText2
                              .copyWith(color: Theme.of(context).primaryColor)),
                    ),
                    SizedBox(height: 4.0),
                    Text(
                      //here the name should be returned from a current user
                        '${person.name}',
                        style: Theme.of(context).textTheme.bodyText2),
                  ],
                ));
          } else {
            return Container();
          }
        });
  }
}
