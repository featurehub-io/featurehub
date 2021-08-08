class FeatureHubClient:
    def __init__(self, host: str, sdk_urls: list[str], repository, client, config):
        self.repository = repository
        self.client = client
        self.config = config

        if host is not None and sdk_urls is not None:
            # this.clientSideEvaluation = sdkUrls.stream().anyMatch(FeatureHubConfig::sdkKeyIsClientSideEvaluated);
            # this.makeRequests = true;

            # executorService = makeExecutorService();
            formatted_sdk_urls = []
            for i in sdk_urls:
                formatted_sdk_urls.append("sdkUrl=" + i)
            url = host + "/features?" + '&'.join(formatted_sdk_urls)
            print(url)

            # if clientSideEvaluation:
            #   checkForUpdates()

        else:
            raise RuntimeError("FeatureHubClient initialized without any sdk_urls")


# this is just for testing
fhClient = FeatureHubClient("http://localhost", ["abc", "123"], "repo", "client", "config")
