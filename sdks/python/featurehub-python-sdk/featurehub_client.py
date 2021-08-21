import httpx


class FeatureHubRepository:
    features = {}
    ready: bool

    def notify(self, status: str, data):
        if status == 'FEATURES':
            self.update_features(data)
            self.ready = True
        elif status == 'FAILED':
            self.ready = False

    def update_features(self, data: dict):
        for feature_apikey in data:
            for feature in feature_apikey['features']:
                self.features[feature['key']] = feature
        print(self.features)

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
                print('success')
            else:
                self.repository.notify("FAILED", None)


# this is just for testing
repo = FeatureHubRepository()
fhClient = FeatureHubClient("https://kd0gwa.demo.featurehub.io", ["default/190017b3-3e4d-4c68-9805-d52c2b597fe0/hrn6TUohWtUFIvlPCar6RVYkZ5fZH9*3VnmTK5Wp47EQiG6x38n"
                                                                  ,'default/29517115-da60-4b64-99db-9da017561edd/7NZq23UuFEdmpcwxx7lkKSH0QH1EIS*dmGCHaOErbXyMLfzHCe0'], repo)
