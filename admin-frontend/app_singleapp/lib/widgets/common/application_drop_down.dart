import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class ApplicationDropDown extends StatefulWidget {
  final List<Application> applications;
  final bloc;

  const ApplicationDropDown({Key key, this.applications, this.bloc})
      : super(key: key);

  @override
  _ApplicationDropDownState createState() => _ApplicationDropDownState();
}

class _ApplicationDropDownState extends State<ApplicationDropDown> {
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(4.0),
      constraints: BoxConstraints(maxWidth: 200),
      decoration: BoxDecoration(
          color: Colors.white,
          border: Border.all(color: Colors.black87, width: 0.5),
          borderRadius: BorderRadius.all(Radius.circular(2.0))),
      child: DropdownButtonHideUnderline(
        child: DropdownButton(
          isExpanded: true,
          isDense: true,
          items: widget.applications != null && widget.applications.isNotEmpty
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
          value: widget.bloc.mrClient.getCurrentAid(),
        ),
      ),
    );
  }
}
