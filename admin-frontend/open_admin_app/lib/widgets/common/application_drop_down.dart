import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/mr_client_aware.dart';

class ApplicationDropDown extends StatefulWidget {
  final List<Application> applications;
  final ManagementRepositoryAwareBloc bloc;

  const ApplicationDropDown(
      {Key? key, required this.applications, required this.bloc})
      : super(key: key);

  @override
  ApplicationDropDownState createState() => ApplicationDropDownState();
}

class ApplicationDropDownState extends State<ApplicationDropDown> {
  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: () => {},
      child: Container(
        constraints: const BoxConstraints(maxWidth: 200),
        child: StreamBuilder<String?>(
            stream: widget.bloc.mrClient.streamValley.currentAppIdStream,
            builder: (context, snapshot) {
              return DropdownButtonHideUnderline(
                child: DropdownButton(
                  icon: const Padding(
                    padding: EdgeInsets.only(left: 8.0),
                    child: Icon(
                      Icons.keyboard_arrow_down,
                      size: 18,
                    ),
                  ),
                  isExpanded: true,
                  isDense: true,
                  items: widget.applications.isNotEmpty
                      ? widget.applications.map((Application application) {
                          return DropdownMenuItem<String>(
                              value: application.id,
                              child: Text(
                                application.name,
                                style: Theme.of(context).textTheme.bodyMedium,
                                overflow: TextOverflow.ellipsis,
                              ));
                        }).toList()
                      : null,
                  hint: Text('Select application',
                      style: Theme.of(context).textTheme.titleSmall),
                  onChanged: (String? value) {
                    if (value != null) {
                      setState(() {
                        widget.bloc.mrClient.setCurrentAid(value);
                      });
                    }
                  },
                  value: widget.applications
                      .firstWhereOrNull((a) => a.id == snapshot.data)
                      ?.id,
                ),
              );
            }),
      ),
    );
  }
}
