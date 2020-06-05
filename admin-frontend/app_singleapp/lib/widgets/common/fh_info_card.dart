import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';

class FHInfoCardWidget extends StatelessWidget {
  final String message;
  final double width;

  const FHInfoCardWidget({Key key, @required this.message, this.width = 540}) : super(key: key);

  Widget build(BuildContext context) {
    double _containerWidth = MediaQuery.of(context).size.width*0.5;
    return Container(
      width:_containerWidth,
      child: Card(
        margin: EdgeInsets.all(20),
        color: Theme.of(context).backgroundColor,
        child: Container(
          padding: EdgeInsets.all(20),
          child: Row(
            children: <Widget>[
              Container(padding:EdgeInsets.only(right:20),child: Icon(Icons.info, color: Color(0xff6DD3F4),size: 30,)),
              Flexible(
                child: Text(message,
                  textAlign:TextAlign.left,
                  style: Theme.of(context).textTheme.caption,),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
