import unittest
import respx
from httpx import Response
import featurehub_client
from unittest.mock import patch


class FeatureHubClientTest(unittest.TestCase):

    @respx.mock
    @patch('featurehub_repository.FeatureHubRepository')
    def test_init_fh_client_success(self, mock_repo):
        data = [{'id': '190017b3-3e4d-4c68-9805-d52c2b597fe0', 'features': [{'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE',
                                                                             'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': False, 'strategies': []},
                                                                            {'id': '4033e577-1157-4e8f-9f18-b72317e80a57', 'key': 'SUBMIT_COLOR_BUTTON',
                                                                             'l': False, 'version': 0, 'type': 'STRING', 'strategies': []}]}]

        respx.get("http://localhost/features?apiKeys=abc&apiKeys=123&apiKeys=xyz").mock(return_value=Response(200, json=data))
        featurehub_client.FeatureHubClient("http://localhost", ['abc', '123', 'xyz'], mock_repo)
        mock_repo.notify.assert_called_with('FEATURES', data)

    @respx.mock
    @patch('featurehub_repository.FeatureHubRepository')
    def test_init_fh_client_fail(self, mock_repo):
        respx.get("http://localhost/features?apiKeys=abc&apiKeys=123&apiKeys=xyz").mock(return_value=Response(400))
        featurehub_client.FeatureHubClient("http://localhost", ['abc', '123', 'xyz'], mock_repo)
        mock_repo.notify.assert_called_with('FAILED', None)

    @respx.mock
    @patch('featurehub_repository.FeatureHubRepository')
    def test_init_fh_client_empty_features(self, mock_repo):
        respx.get("http://localhost/features?apiKeys=abc&apiKeys=123&apiKeys=xyz").mock(return_value=Response(200, json=[{}]))
        featurehub_client.FeatureHubClient("http://localhost", ['abc', '123', 'xyz'], mock_repo)
        mock_repo.notify.assert_called_with('FEATURES', [{}])

    @respx.mock
    @patch('featurehub_repository.FeatureHubRepository')
    def test_init_fh_client_empty_features_when_none_set(self, mock_repo):
        data = [{"id":"29517115-da60-4b64-99db-9da017561edd", "features": []}]
        respx.get("http://localhost/features?apiKeys=abc&apiKeys=123&apiKeys=xyz").mock(return_value=Response(200, json=data))
        featurehub_client.FeatureHubClient("http://localhost", ['abc', '123', 'xyz'], mock_repo)
        mock_repo.notify.assert_called_with('FEATURES', data)

    @respx.mock
    @patch('featurehub_repository.FeatureHubRepository')
    def test_url_param(self, mock_repo):
        respx.get("http://localhost/features?apiKeys=abc&apiKeys=123&apiKeys=xyz").mock(return_value=Response(200, json = {}))
        client = featurehub_client.FeatureHubClient("http://localhost", ['abc', '123', 'xyz'], mock_repo)
        self.assertEqual(client.url, "http://localhost/features?apiKeys=abc&apiKeys=123&apiKeys=xyz")

    @patch('featurehub_repository.FeatureHubRepository')
    def test_url_param_no_apikey_provided(self, mock_repo):
        with self.assertRaises(RuntimeError):
            featurehub_client.FeatureHubClient("http://localhost", [], mock_repo)

    @patch('featurehub_repository.FeatureHubRepository')
    def test_url_param_no_host_provided(self, mock_repo):
        with self.assertRaises(RuntimeError):
            featurehub_client.FeatureHubClient("", ["abc"], mock_repo)


if __name__ == '__main__':
    unittest.main()
