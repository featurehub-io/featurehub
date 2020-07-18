part of featurehub.client;

enum Readyness { NotReady, Ready, Failed }

abstract class FeatureStateHolder {
  bool get exists;
  bool get booleanValue;
  String get stringValue;
  num get numberValue;
  String get key;
  dynamic get jsonValue;
  FeatureValueType get type;

  Stream<FeatureStateHolder> get featureUpdateStream;
  FeatureStateHolder copy();
}

typedef AnalyticsCollector = Future<void> Function(
    String action, List<FeatureStateHolder> featureStateAtCurrentTime,
    {Map<String, String> other});

class _FeatureStateBaseHolder implements FeatureStateHolder {
  dynamic _value;
  FeatureState _featureState;
  BehaviorSubject<FeatureStateHolder> _listeners;

  @override
  String get key => _featureState?.key;
  @override
  Stream<FeatureStateHolder> get featureUpdateStream => _listeners.stream;

  _FeatureStateBaseHolder(_FeatureStateBaseHolder fs) {
    _listeners = fs?._listeners ?? BehaviorSubject<FeatureStateHolder>();
  }

  set featureState(FeatureState fs) {
    _featureState = fs;
    final oldValue = _value;
    _value = fs.value;
    if (oldValue != _value) {
      _listeners.add(this);
    }
  }

  @override
  bool get exists => _value != null;

  @override
  bool get booleanValue =>
      _featureState?.type == FeatureValueType.BOOLEAN ? _value as bool : null;

  @override
  String get stringValue => (_featureState?.type == FeatureValueType.STRING ||
          _featureState?.type == FeatureValueType.JSON)
      ? _value as String
      : null;

  @override
  num get numberValue =>
      _featureState?.type == FeatureValueType.NUMBER ? _value as num : null;

  @override
  dynamic get jsonValue =>
      _featureState?.type == FeatureValueType.JSON ? jsonDecode(_value) : null;

  @override
  FeatureValueType get type => _featureState.type;

  @override
  FeatureStateHolder copy() {
    return _FeatureStateBaseHolder(null)..featureState = _featureState;
  }

  void shutdown() {
    _listeners.close();
  }
}

final _log = Logger('FeatureHub');

class ClientFeatureRepository {
  bool _hasReceivedInitialState = false;
  // indexed by key
  final Map<String, _FeatureStateBaseHolder> _features = {};
  final List<AnalyticsCollector> _analyticsCollectors = [];
  Readyness _readynessState = Readyness.NotReady;
  final _readynessListeners =
      BehaviorSubject<Readyness>.seeded(Readyness.NotReady);
  final _newFeatureStateAvailableListeners =
      PublishSubject<ClientFeatureRepository>();
  bool _catchAndReleaseMode = false;
  // indexed by id (not key)
  final Map<String, FeatureState> _catchReleaseStates = {};

  Stream<Readyness> get readynessStream => _readynessListeners.stream;
  Stream<ClientFeatureRepository> get newFeatureStateAvailable =>
      _newFeatureStateAvailableListeners.stream;

  void notify(SSEResultState state, dynamic data) {
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
              LocalApiClient.deserialize(data, 'List<FeatureState>');
          if (_hasReceivedInitialState && _catchAndReleaseMode) {
            _catchUpdatedFeatures(features);
          } else {
            var _updated = false;
            features.forEach((f) => _updated = _updated || _featureUpdate(f));
            if (!_hasReceivedInitialState) {
              _checkForInvalidFeatures();
              _hasReceivedInitialState = true;
            } else if (_updated) {
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
            if (_featureUpdate(feature)) {
              _triggerNewStateAvailable();
            }
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
    _readynessListeners.add(_readynessState);
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
        .join(',');
    if (missingKeys.isNotEmpty) {
      _log.info('We have requests for keys that are missing: ${missingKeys}');
    }
  }

  void _triggerNewStateAvailable() {
    if (_hasReceivedInitialState) {
      if (!_catchAndReleaseMode || _catchReleaseStates.isNotEmpty) {
        _newFeatureStateAvailableListeners.add(this);
      }
    }
  }

  Function addAnalyticsCollector(AnalyticsCollector collector) {
    _analyticsCollectors.add(collector);

    return () => _analyticsCollectors.remove(collector);
  }

  void logAnalyticsEvent(String action, {Map<String, String> other}) {
    final featureStateAtCurrentTime =
        _features.values.where((f) => f.exists).map((f) => f.copy()).toList();

    _analyticsCollectors
        .forEach((ac) => ac(action, featureStateAtCurrentTime, other: other));
  }

  FeatureStateHolder getFeatureState(String key) {
    return _features.putIfAbsent(key, () => _FeatureStateBaseHolder(null));
  }

  bool get catchAndReleaseMode => _catchAndReleaseMode;
  set catchAndReleaseMode(bool val) => _catchAndReleaseMode = val;
  Readyness get readyness => _readynessState;

  void release() {
    final states = _catchReleaseStates.values;
    _catchReleaseStates.clear();
    states.forEach((f) => _featureUpdate(f));
  }

  bool _featureUpdate(FeatureState feature) {
    if (feature == null) return false;

    var holder = _features[feature.key];

    if (holder == null || holder.key == null) {
      holder = _FeatureStateBaseHolder(holder);
    } else {
      if (holder._featureState.version >= feature.version) {
        return false;
      }
    }

    holder.featureState = feature;
    _features[feature.key] = holder;

    return true;
  }

  void _deleteFeature(FeatureState feature) {
    _features.remove(feature.key);
  }

  /// after this this repository is not usable, create a new one.
  void shutdown() {
    _features.values.forEach((f) => f.shutdown());
    _features.clear();
    _readynessListeners.close();
    _newFeatureStateAvailableListeners.close();
  }
}
