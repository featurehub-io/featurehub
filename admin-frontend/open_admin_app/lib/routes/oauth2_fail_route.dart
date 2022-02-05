import 'package:flutter/material.dart';

class Oauth2FailRoute extends StatelessWidget {
  const Oauth2FailRoute({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Image.asset('assets/logo/FeatureHub-icon.png',
                width: 40, height: 40),
            const SizedBox(height: 24.0),
            const Text(
              "You are not authorised to access FeatureHub",
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 30,
              ),
            ),
            const SizedBox(height: 16.0),
            const Text(
              "Please contact your administrator and ask them nicely to add your email to your organization's user list",
              textAlign: TextAlign.center,
              style: TextStyle(
              ),
            ),
          ],
        ),
      ),
    );

    // return const Card(
    //   child: Text("You are not authorised to access FeatureHub. Please contact your administrator."),
    // );
  }
}
