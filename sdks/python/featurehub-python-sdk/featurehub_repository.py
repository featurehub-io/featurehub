from typing import Optional
from fh_state_base_holder import FeatureStateBaseHolder


class FeatureHubRepository:
    features: dict[str, FeatureStateBaseHolder] = {}
    ready: bool

    def notify(self, status: str, data: Optional[list[dict]]):
        if status == 'FEATURES':
            self.__update_features(data)
            self.ready = True
        elif status == 'FAILED':
            self.ready = False

    def __update_features(self, data: list[dict]):
        if data:
            for feature_apikey in data:
                if feature_apikey:
                    for feature_state in feature_apikey['features']:
                        self.__update_feature_state(feature_state)

    def __update_feature_state(self, feature_state):
        if not feature_state or not feature_state['key']:
            return

        # check if feature already in the dictionary, if not add to the dictionary
        holder = self.features.get(feature_state['key'])
        if not holder:
            new_feature = FeatureStateBaseHolder(feature_state)
            self.features[feature_state['key']] = new_feature
            return

        # if feature is in the dictionary, check if version has changed
        elif feature_state['version'] < holder.version:
            return
        elif feature_state['version'] == holder.version and feature_state['value'] == holder.value:
            return

        holder.set_feature_state(feature_state)

    def is_ready(self):
        return self.ready
