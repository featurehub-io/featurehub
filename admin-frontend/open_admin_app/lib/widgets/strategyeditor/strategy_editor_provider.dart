
import 'package:mrapi/api.dart';

import 'editing_rollout_strategy.dart';

/**
 * This provides an interface to do a few things that the strategy editor cannot do within its own context.
 */

abstract class StrategyEditorProvider {

  /// This requires the the strategy be validated within its context. When it
  /// is on a feature value for instance, it allows it to be grouped along with
  /// all of the group strategies and validated as a bundle.
  ///
  /// This capability may not exist in which case null can be returned. Local validation
  /// MUST have taken place by this point so conversion to the desired type
  /// (RolloutStrategy, FeatureStrategy) will not produce a null value.
  Future<RolloutStrategyValidationResponse?> validateStrategy(EditingRolloutStrategy rs);

  /// This tells the source of the strategy that the user has finished editing the strategy and wishes to save it.
  Future<void> updateStrategy(EditingRolloutStrategy rs);
}
