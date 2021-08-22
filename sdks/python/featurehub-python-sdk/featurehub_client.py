import httpx


class FeatureHubRepository:
    features = {}
    ready: bool

    def notify(self, status: str, data: list[dict]):
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


class FeatureHubClient:
    url: str

    def __init__(self, host: str, api_keys: list[str], repository: FeatureHubRepository):
        self.repository = repository

        if host and api_keys:
            # this.clientSideEvaluation = sdkUrls.stream().anyMatch(FeatureHubConfig::sdkKeyIsClientSideEvaluated);
            # this.makeRequests = true;

            # executorService = makeExecutorService();

            self.url = host + '/features?' + '&'.join(map(lambda i: 'apiKeys=' + i, api_keys))
            print(self.url)
            # if clientSideEvaluation:
            self.__check_for_updates()

        else:
            raise RuntimeError("FeatureHubClient initialized without any api keys")

    def __check_for_updates(self):
        with httpx.Client(http2=True) as client:
            # headers = {'Content-Type': 'application/json'}
            resp = client.get(self.url)
            if resp.status_code == httpx.codes.OK:
                self.repository.notify("FEATURES", resp.json())
            else:
                self.repository.notify("FAILED", None)
