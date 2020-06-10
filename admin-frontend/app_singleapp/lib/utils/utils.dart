import 'dart:convert';

import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

bool validateEmail(email) {
  return RegExp(r'^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|'
          r'(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|'
          r'(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$')
      .hasMatch(email);
}

bool validateFeatureKey(key) {
  return RegExp(r'^[A-Za-z0-9_]+$').hasMatch(key);
}

String validateJson(String jsonToCheck) {
  if (jsonToCheck.isEmpty) {
    return null;
  }
  try {
    json.decode(jsonToCheck);
  } catch (e) {
    return e.toString();
  }
  return null;
}

String validateNumber(String numberToCheck) {
  if (numberToCheck.isEmpty) {
    return null;
  }
  try {
    double.parse(numberToCheck);
  } catch (e) {
    return 'Must be a valid number.';
  }
  return null;
}

String condenseJson(initialJson) {
  return initialJson
      .replaceAll('\n', '')
      .replaceAll('  ', '')
      .replaceAll('{ ', '{')
      .replaceAll(': ', ':');
}

extension ServiceAccountPermissionTypeExtensions
    on ServiceAccountPermissionType {
  String humanReadable() {
    switch (this) {
      case ServiceAccountPermissionType.READ:
        return 'Read';
      case ServiceAccountPermissionType.TOGGLE_ENABLED:
        return 'Change value';
      case ServiceAccountPermissionType.TOGGLE_LOCK:
        return 'Lock/Unlock';
    }

    return '';
  }
}

String parsePermissions(json) {
  if (json == 'TOGGLE_ENABLED') {
    return 'Change value';
  } else if (json == 'TOGGLE_LOCK') {
    return 'Lock/Unlock';
  } else if (json == 'READ') {
    return 'Read';
  }
  return '';
}

class FHError {
  final String humanErrorMessage;
  final String errorMessage;

  //keep exception as dynamic variable since there could be various types of errors: e.g. Exception, ApiException, StateError
  final exception;
  final StackTrace stackTrace;
  final bool showDetails;

  const FHError(this.humanErrorMessage,
      {this.exception,
      this.errorMessage,
      this.stackTrace,
      this.showDetails = true})
      : assert(humanErrorMessage != null);

  static bool _is5XX(e) {
    if (e is ApiException && e.code > 499 && e.code < 600) {
      return true;
    }
    return false;
  }

  static bool _noAuth(e) {
    if (e is ApiException && (e.code == 401 || e.code == 403)) {
      return true;
    }
    return false;
  }

  factory FHError.createError(e, StackTrace s, {bool showDetails = true}) {
    var message = 'An unexpected error occured!';
    var errorMessage = 'Contact your FeatureHub administrator';

    if (_is5XX(e)) {
      message = 'There is an issue with FeatureHub';
    } else if (_noAuth(e)) {
      message = 'Looks like you are not authorised to perform this operation';
    } else if (e is ApiException && e.code == 409) {
      message = 'An item with this name already exists';
    } else if (e is ApiException && e.code == 422) {
      message =
          'This item has been updated by another person since you loaded it, please check the value and try again.';
    }

    return FHError(message,
        errorMessage: errorMessage,
        exception: e,
        stackTrace: s,
        showDetails: showDetails);
  }
}
