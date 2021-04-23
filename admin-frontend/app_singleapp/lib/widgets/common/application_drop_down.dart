import 'package:app_singleapp/api/mr_client_aware.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

class ApplicationDropDown extends StatefulWidget {
  final List<Application> applications;
  final ManagementRepositoryAwareBloc bloc;

  const ApplicationDropDown({Key? key, this.applications, this.bloc})
      : super(key: key);

  @override
  _ApplicationDropDownState createState() => _ApplicationDropDownState();
}

class _ApplicationDropDownState extends State<ApplicationDropDown> {
  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: () => {},
      child: Container(
        padding: EdgeInsets.all(4.0),
        constraints: BoxConstraints(maxWidth: 200),
        child: StreamBuilder<String>(
            stream: widget.bloc.mrClient.streamValley.currentAppIdStream,
            builder: (context, snapshot) {
              return DropdownButtonHideUnderline(
                child: DropdownButton(
                  icon: Padding(
                    padding: EdgeInsets.only(left: 8.0),
                    child: Icon(
                      Icons.keyboard_arrow_down,
                      size: 24,
                    ),
                  ),
                  isExpanded: true,
                  isDense: true,
                  items: widget.applications != null &&
                          widget.applications.isNotEmpty
                      ? widget.applications.map((Application application) {
                          return DropdownMenuItem<String>(
                              value: application.id,
                              child: Text(
                                application.name,
                                style: Theme.of(context).textTheme.bodyText2,
                                overflow: TextOverflow.ellipsis,
                              ));
                        }).toList()
                      : null,
                  hint: Text('Select application',
                      style: Theme.of(context).textTheme.subtitle2),
                  onChanged: (value) {
                    setState(() {
                      widget.bloc.mrClient.setCurrentAid(value);
                    });
                  },
                  value: widget.applications
                      .firstWhere((a) => a.id == snapshot.data,
                          orElse: () => null)
                      ?.id,
                ),
              );
            }),
      ),
    );
  }
}
