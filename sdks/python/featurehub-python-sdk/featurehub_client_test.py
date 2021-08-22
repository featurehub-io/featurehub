import unittest

import httpx
import respx
from httpx import Response

import featurehub_client
from unittest.mock import MagicMock, patch, Mock


class FeatureHubClientTest(unittest.TestCase):

    def test_is_ready_func(self):
        repo = featurehub_client.FeatureHubRepository()
        repo.ready = False
        self.assertFalse(repo.is_ready())
        repo.ready = True
        self.assertTrue(repo.is_ready())

    def test_url_param(self):
        repo = featurehub_client.FeatureHubRepository()
        client = featurehub_client.FeatureHubClient("http://localhost", ['abc', '123', 'xyz'], repo)
        self.assertEqual(client.url, "http://localhost/features?apiKeys=abc&sdkUrl=123&apiKeys=xyz")

    def test_url_param_no_apikey_provided(self):
        repo = featurehub_client.FeatureHubRepository()
        with self.assertRaises(RuntimeError):
            featurehub_client.FeatureHubClient("http://localhost", [], repo)

    def test_url_param_no_host_provided(self):
        repo = featurehub_client.FeatureHubRepository()
        with self.assertRaises(RuntimeError):
            featurehub_client.FeatureHubClient("", ["abc"], repo)

    @respx.mock
    def test_init_fh_client_success(self):
        data = [{'id': '190017b3-3e4d-4c68-9805-d52c2b597fe0', 'features': [{'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE',
                                                                             'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': False, 'strategies': []},
                                                                            {'id': '4033e577-1157-4e8f-9f18-b72317e80a57', 'key': 'SUBMIT_COLOR_BUTTON',
                                                                             'l': False, 'version': 0, 'type': 'STRING', 'strategies': []}]}]

        respx.get("http://localhost/features?apiKeys=abc&apiKeys=123&apiKeys=xyz").mock(return_value=Response(200, json=data))

        repo = featurehub_client.FeatureHubRepository()
        featurehub_client.FeatureHubClient("http://localhost", ['abc', '123', 'xyz'], repo)
        self.assertTrue(repo.ready)
        self.assertEqual(repo.features, {'FEATURE_TITLE_TO_UPPERCASE': {'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE',
                                                                        'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': False, 'strategies': []},
                                         'SUBMIT_COLOR_BUTTON': {'id': '4033e577-1157-4e8f-9f18-b72317e80a57', 'key': 'SUBMIT_COLOR_BUTTON',
                                                                 'l': False, 'version': 0, 'type': 'STRING', 'strategies': []}})

    @respx.mock
    def test_init_fh_client_fail(self):
        respx.get("http://localhost/features?apiKeys=abc&apiKeys=123&apiKeys=xyz").mock(return_value=Response(400))
        repo = featurehub_client.FeatureHubRepository()
        featurehub_client.FeatureHubClient("http://localhost", ['abc', '123', 'xyz'], repo)
        self.assertFalse(repo.ready)
        self.assertEqual(repo.features, {})

    @respx.mock
    def test_init_fh_client_empty_features(self):
        respx.get("http://localhost/features?apiKeys=abc&apiKeys=123&apiKeys=xyz").mock(return_value=Response(200, json=[{}]))
        repo = featurehub_client.FeatureHubRepository()
        featurehub_client.FeatureHubClient("http://localhost", ['abc', '123', 'xyz'], repo)
        self.assertTrue(repo.ready)
        self.assertEqual(repo.features, {})

    @respx.mock
    def test_init_fh_client_empty_features_when_none_set(self):
        data = [{"id":"29517115-da60-4b64-99db-9da017561edd", "features": []}]
        respx.get("http://localhost/features?apiKeys=abc&apiKeys=123&apiKeys=xyz").mock(return_value=Response(200, json=data))
        repo = featurehub_client.FeatureHubRepository()
        featurehub_client.FeatureHubClient("http://localhost", ['abc', '123', 'xyz'], repo)
        self.assertTrue(repo.ready)
        self.assertEqual(repo.features, {})




if __name__ == '__main__':
    unittest.main()
