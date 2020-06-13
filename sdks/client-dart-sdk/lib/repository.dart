part of featurehub.client;

enum Readyness { NotReady, Ready, Failed }

abstract class FeatureStateHolder {
  bool isSet();
  bool get booleanValue;
  String get stringValue;
  num get numberValue;
  dynamic get jsonValue;
  FeatureValueType get type;
}

typedef ReadynessListener = Future<void> Function(Readyness state);
typedef PostLoadNewFeatureStateAvailableListener = Future<void> Function(
    ClientFeatureRepository repo);
typedef AnalyticsCollector = Future<void> Function(
    String action, List<FeatureStateHolder> featureStateAtCurrentTime,
    {Map<String, String> other});
typedef FeatureListener = Future<void> Function(FeatureStateHolder feature);

class _FeatureStateBaseHolder implements FeatureStateHolder {
  dynamic _value;
  FeatureState _featureState;
  List<FeatureListener> _listeners;

  get key => _featureState?.key;

  _FeatureStateBaseHolder(_FeatureStateBaseHolder fs) {
    _listeners = fs?._listeners ?? [];
  }

  set featureState(FeatureState fs) {
    _featureState = fs;
    final oldValue = _value;
    _value = fs.value;
    if (oldValue != _value) {
      _notifyListeners();
    }
  }

  bool isSet() {
    return _value != null;
  }

  bool get booleanValue =>
      _featureState?.type == FeatureValueType.BOOLEAN ? _value as bool : null;
  String get stringValue => (_featureState?.type == FeatureValueType.STRING ||
          _featureState?.type == FeatureValueType.JSON)
      ? _value as String
      : null;
  num get numberValue =>
      _featureState?.type == FeatureValueType.NUMBER ? _value as num : null;
  dynamic get jsonValue =>
      _featureState?.type == FeatureValueType.JSON ? jsonDecode(_value) : null;

  FeatureValueType get type => _featureState.type;

  void _notifyListeners() {
    _listeners.forEach((l) => l(this));
  }

  Function addListener(FeatureListener listener) {
    _listeners.add(listener);

    return () => _listeners.remove(listener);
  }

  FeatureStateHolder copy() {
    return _FeatureStateBaseHolder(null)..featureState = _featureState;
  }
}

final _log = Logger("FeatureHub");

class ClientFeatureRepository {
  bool _hasReceivedInitialState;
  Map<String, _FeatureStateBaseHolder> _features = {};
  List<AnalyticsCollector> _analyticsCollectors = [];
  Readyness _readynessState = Readyness.NotReady;
  List<ReadynessListener> _readynessListeners = [];
  bool _catchAndReleaseMode = false;
  // indexed by id (not key)
  Map<String, FeatureState> _catchReleaseStates = {};
  List<PostLoadNewFeatureStateAvailableListener>
      _newFeatureStateAvailableListeners = [];

  notify(SSEResultState state, dynamic data) {
    _log.fine('Data is $state -> $data');
    if (state != null) {
      switch (state) {
        case SSEResultState.ack:
          break;
        case SSEResultState.bye:
          _readynessState = Readyness.NotReady;
          if (!_catchAndReleaseMode) {
            _broadcastReadynessState();
          }
          break;
        case SSEResultState.failure:
          _readynessState = Readyness.Failed;
          if (!_catchAndReleaseMode) {
            _broadcastReadynessState();
          }
          break;
        case SSEResultState.features:
          final features =
              LocalApiClient.deserialize(data, 'List<FeatureState>')
                  as List<FeatureState>;
          if (_hasReceivedInitialState && _catchAndReleaseMode) {
            _catchUpdatedFeatures(features);
          } else {
            features.forEach((f) => _featureUpdate(f));
            if (!_hasReceivedInitialState) {
              _checkForInvalidFeatures();
              _hasReceivedInitialState = true;
            } else {
              _triggerNewStateAvailable();
            }
            _readynessState = Readyness.Ready;
            _broadcastReadynessState();
          }
          break;
        case SSEResultState.feature:
          final feature =
              LocalApiClient.deserialize(data, 'FeatureState') as FeatureState;
          if (_catchAndReleaseMode) {
            _catchUpdatedFeatures([feature]);
          } else {
            _featureUpdate(feature);
            _triggerNewStateAvailable();
          }
          break;
        case SSEResultState.delete_feature:
          _deleteFeature(
              LocalApiClient.deserialize(data, 'FeatureState') as FeatureState);
          break;
      }
    }
  }

  void _broadcastReadynessState() {
    _readynessListeners.forEach((l) => l(_readynessState));
  }

  void _catchUpdatedFeatures(List<FeatureState> features) {
    var updatedValues = false;
    for (var f in features) {
      final fs = _catchReleaseStates[f.id];
      if (fs == null) {
        _catchReleaseStates[f.id] = f;
        updatedValues = true;
      } else {
        if (fs.version == null || f.version > fs.version) {
          _catchReleaseStates[f.id] = f;
          updatedValues = true;
        }
      }
    }

    if (updatedValues) {
      _triggerNewStateAvailable();
    }
  }

  void _checkForInvalidFeatures() {
    final missingKeys = _features.keys
        .where((k) => _features[k].key == null)
        .toList()
        .join(",");
    if (missingKeys.isNotEmpty) {
      _log.info("We have requests for keys that are missing: ${missingKeys}");
    }
  }

  void _triggerNewStateAvailable() {
    if (_hasReceivedInitialState &&
        _newFeatureStateAvailableListeners.isNotEmpty) {
      if (!_catchAndReleaseMode || _catchReleaseStates.isNotEmpty) {
        _newFeatureStateAvailableListeners.forEach((l) => l(this));
      }
    }
  }

  Function addPostLoadNewFeatureStateAvailableListener(
      PostLoadNewFeatureStateAvailableListener listener) {
    _newFeatureStateAvailableListeners.add(listener);

    return () => _newFeatureStateAvailableListeners.remove(listener);
  }

  Function addReadynessListener(ReadynessListener listener) {
    _readynessListeners.add(listener);

    return () => _readynessListeners.remove(listener);
  }

  Function addAnalyticsCollector(AnalyticsCollector collector) {
    _analyticsCollectors.add(collector);

    return () => _analyticsCollectors.remove(collector);
  }

  void logAnalyticsEvent(String action, {Map<String, String> other}) {
    final featureStateAtCurrentTime =
        _features.values.where((f) => f.isSet()).map((f) => f.copy()).toList();

    _analyticsCollectors
        .forEach((ac) => ac(action, featureStateAtCurrentTime, other: other));
  }

  FeatureStateHolder getFeatureState(String key) {
    return _features.putIfAbsent(key, () => _FeatureStateBaseHolder(null));
  }

  bool get catchAndReleaseMode => _catchAndReleaseMode;
  set catchAndReleaseMode(bool val) => _catchAndReleaseMode = val;

  void release() {
    final states = _catchReleaseStates.values;
    _catchReleaseStates.clear();
    states.forEach((f) => _featureUpdate(f));
  }

  _featureUpdate(FeatureState feature) {
    if (feature == null) return;

    var holder = _features[feature.key];

    if (holder == null || holder.key == null) {
      holder = _FeatureStateBaseHolder(holder);
    }

    holder.featureState = feature;
  }

  void _deleteFeature(FeatureState feature) {
    _features.remove(feature.key);
  }
}
