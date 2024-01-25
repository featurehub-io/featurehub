import 'dart:async';
import 'dart:html'; // ignore: avoid_web_libraries_in_flutter
import 'dart:math';

import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

bool validateEmail(String? email) {
  if (email == null || email.isEmpty) return false;

  return RegExp(r'^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|'
          r'(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|'
          r'(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$')
      .hasMatch(email);
}

const _characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
const _charactersLength = _characters.length - 1;
final _strategyRandom = Random();

String _generateRandomStrategyId() {
  var result           = '';

  for ( var i = 0; i < 4; i++ ) {
    result += _characters[_strategyRandom.nextInt(_charactersLength)];
  }

  return result;
}

String makeStrategyId({List<RolloutStrategy> existing = const []}) {
  // cycle round loop until we generate a unique id
  while (true) {
    final gen = _generateRandomStrategyId();
    if (!existing.any((st) => st.id == gen)) {
      return gen;
    }
  }
}

bool validateUrl(String? url) {
  if (url == null || url.isEmpty) return false;
  return RegExp(r"((([A-Za-z]{3,9}:(?:\/\/)?)(?:[-;:&=\+\$,\w]+@)?[A-Za-z0-9.-]+|(?:www.|[-;:&=\+\$,\w]+@)[A-Za-z0-9.-]+)((?:\/[\+~%\/.\w-_]*)?\??(?:[-\+=&;%@.\w_]*)#?(?:[\w]*))?)")
      .hasMatch(url);
}

bool validateFeatureKey(String key) {
  return !key.contains(' ');
}


String? validateNumber(String? numberToCheck) {
  if (numberToCheck == null || numberToCheck.isEmpty) {
    return null;
  }
  try {
    double.parse(numberToCheck);
  } catch (e) {
    return 'Must be a valid number.';
  }
  return null;
}

String condenseJson(String initialJson) {
  return initialJson
      .replaceAll('\n', '')
      .replaceAll('  ', '')
      .replaceAll('{ ', '{')
      .replaceAll(': ', ':');
}

extension RoleTypeExtensions on RoleType {
  String humanReadable() {
    switch (this) {
      case RoleType.CHANGE_VALUE:
        return 'Change value';
      case RoleType.UNLOCK:
        return 'Unlock';
      case RoleType.LOCK:
        return 'Lock';
      case RoleType.READ:
        return 'Read';
    }
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
  final String? errorMessage;

  //keep exception as dynamic variable since there could be various types of errors: e.g. Exception, ApiException, StateError
  final dynamic exception;
  final StackTrace? stackTrace;
  final bool showDetails;

  const FHError(this.humanErrorMessage,
      {this.exception,
      this.errorMessage,
      this.stackTrace,
      this.showDetails = true});

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

  factory FHError.createError(dynamic e, StackTrace? s,
      {bool showDetails = true}) {
    var message = 'An unexpected error occurred!';
    var errorMessage = '';

    if (_is5XX(e)) {
      message = 'There is an issue with FeatureHub, please try again later';
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

class Debouncer {
  Debouncer({required this.milliseconds});
  final int milliseconds;
  Timer? _timer;
  void run(VoidCallback action) {
    if (_timer?.isActive ?? false) {
      _timer?.cancel();
    }
    _timer = Timer(Duration(milliseconds: milliseconds), action);
  }
}
