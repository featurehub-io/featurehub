# A Flutter Example

This is a Flutter example which demonstrates FeatureHub Dart SDK implementation 
using GET api request to get feature states from the FeatureHub repository.

Even though it says web (it started out like that) it is in fact cross platform.

In this case, the Refresh Indicator is used to request updated features which
trigger the stream to update and repaint the screen. 

It expects a string feature called FLUTTER_COLOUR to exist and have values of 
blue, purple or yellow.  

