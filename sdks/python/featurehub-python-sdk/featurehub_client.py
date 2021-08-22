import httpx


class FeatureHubRepository:
    features = {}
    ready = False

    def notify(self, status: str, data):
        if status == 'FEATURES':
            self.update_features(data)
            self.ready = True
        elif status == 'FAILED':
            self.ready = False

    def update_features(self, data: list[dict]):
        for feature_apikey in data:
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
            self.check_for_updates()

        else:
            raise RuntimeError("FeatureHubClient initialized without any api keys")

    def check_for_updates(self):
        with httpx.Client(http2=True) as client:
            # headers = {'Content-Type': 'application/json'}
            resp = client.get(self.url)
            if resp.status_code == httpx.codes.OK:
                self.repository.notify("FEATURES", resp.json())
            else:
                self.repository.notify("FAILED", None)
