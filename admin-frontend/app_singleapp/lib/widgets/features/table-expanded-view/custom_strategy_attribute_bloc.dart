// import 'package:bloc_provider/bloc_provider.dart';
// import 'package:mrapi/api.dart';
// import 'package:rxdart/rxdart.dart';
//
// class CustomStrategyAttributeBloc extends Bloc {
//
//   final EnvironmentFeatureValues environmentFeatureValue;
//     final RolloutStrategy rolloutStrategy;
//   final _rolloutStartegyAttributeList = BehaviorSubject<List<RolloutStrategyAttribute>>();
//
//   Stream<List<RolloutStrategyAttribute>> get attributes => _rolloutStartegyAttributeList.stream;
//
//   CustomStrategyAttributeBloc(this.environmentFeatureValue, this.feature, this.featureValue, this.rolloutStrategy){
//
//     _rolloutStartegyAttributeList.add(rolloutStrategy.attributes);
//   }
//
//   void addAttribute(RolloutStrategy rs) {
//     final strategies = _strategySource.value;
//     strategies.add(rs);
//     markDirty();
//     _strategySource.add(strategies);
//   }
//
//   void updateStrategy() {
//     final strategies = _strategySource.value;
//     markDirty();
//     _strategySource.add(strategies);
//   }
//
//   @override
//   void dispose() {
//     // TODO: implement dispose
//   }
//
// }
