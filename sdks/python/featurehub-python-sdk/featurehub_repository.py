from typing import Optional


class FeatureHubRepository:
    features = {}
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
                    for feature in feature_apikey['features']:
                        self.features[feature['key']] = feature

    def is_ready(self):
        return self.ready
