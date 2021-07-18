import 'package:flutter/material.dart';

class NotFoundRoute extends StatefulWidget {
  const NotFoundRoute({Key? key}) : super(key: key);

  @override
  _NotFoundRouteState createState() => _NotFoundRouteState();
}

class _NotFoundRouteState extends State<NotFoundRoute>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  final RelativeRectTween _relativeRectTween = RelativeRectTween(
    begin: RelativeRect.fromLTRB(24, 24, 24, 200),
    end: RelativeRect.fromLTRB(24, 24, 24, 250),
  );

  @override
  void initState() {
    super.initState();
    _controller =
    AnimationController(vsync: this, duration: const Duration(seconds: 3))
      ..repeat(reverse: true);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          PositionedTransition(

            rect: _relativeRectTween.animate(_controller),
            child: Center(
              child: Image.asset('assets/images/meditation.png',  height: 200,
                  width: 200,
                  fit:BoxFit.scaleDown),
            ),
          ),
          Positioned(
            top: 150,
            bottom: 0,
            left: 24,
            right: 24,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: const [
                Text(
                  '404',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                      fontSize: 50,
                      letterSpacing: 2,
                      color: Color(0xff2f3640),
                      fontFamily: 'Anton',
                      fontWeight: FontWeight.bold),
                ),
                Text(
                  "Sorry, we couldn't find the page!",
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 30,
                    color: Color(0xff2f3640),
                  ),
                ),
              ],
            ),
          )
        ],
      ),
    );
  }
}
