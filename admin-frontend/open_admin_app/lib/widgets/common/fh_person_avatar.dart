import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class PersonAvatar extends StatelessWidget {
  final Person person;

  const PersonAvatar({Key? key, required this.person}) : super(key: key);

  @override
  Widget build(BuildContext context) {
            return Column(
              children: [
                CircleAvatar(
                  backgroundColor: const Color(0xffA6F2DE),
                  child: Text(
                      //here the name should be returned from a current user
                      person.name!.substring(0, 1),
                      style: Theme.of(context)
                          .textTheme
                          .bodyText2!
                          .copyWith(color: Theme.of(context).primaryColor)
                          )),
              ],
            );
          }
        }
