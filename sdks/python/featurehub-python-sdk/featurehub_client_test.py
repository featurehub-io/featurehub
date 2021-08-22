import unittest
import featurehub_client
from unittest.mock import MagicMock


class FeatureHubClientTest(unittest.TestCase):

    def test_make_fh_client_object(self):
        repo = featurehub_client.FeatureHubRepository()
        fh_client = featurehub_client.FeatureHubClient("http://localhost", ["abc", "123"], repo)
        # testing
        # fh_client = featurehub_client.FeatureHubClient("https://kd0gwa.demo.featurehub.io", ["default/190017b3-3e4d-4c68-9805-d52c2b597fe0/hrn6TUohWtUFIvlPCar6RVYkZ5fZH9*3VnmTK5Wp47EQiG6x38n"
        # ,'default/29517115-da60-4b64-99db-9da017561edd/7NZq23UuFEdmpcwxx7lkKSH0QH1EIS*dmGCHaOErbXyMLfzHCe0'], repo)

        self.assertIsInstance(fh_client, featurehub_client.FeatureHubClient)

    def test_is_ready_func(self):
        repo = featurehub_client.FeatureHubRepository()
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

    def test_update_features_func(self):
        repo = featurehub_client.FeatureHubRepository()
        data = [{'id': '190017b3-3e4d-4c68-9805-d52c2b597fe0', 'features': [{'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE', 'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': False, 'strategies': []},
                                                                            {'id': '4033e577-1157-4e8f-9f18-b72317e80a57', 'key': 'SUBMIT_COLOR_BUTTON', 'l': False, 'version': 0, 'type': 'STRING', 'strategies': []}]}]
        repo.update_features(data)
        self.assertEqual(repo.features, {'FEATURE_TITLE_TO_UPPERCASE': {'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE', 'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': False, 'strategies': []},
                                         'SUBMIT_COLOR_BUTTON': {'id': '4033e577-1157-4e8f-9f18-b72317e80a57', 'key': 'SUBMIT_COLOR_BUTTON', 'l': False, 'version': 0, 'type': 'STRING', 'strategies': []}})

    def test_update_features_func_empty_list(self):
        repo = featurehub_client.FeatureHubRepository()
        data = []
        repo.update_features(data)
        self.assertEqual(repo.features, {})

    def test_notify_func(self):
        repo = featurehub_client.FeatureHubRepository()
        repo.update_features = MagicMock()
        repo.notify('FEATURES', [{'features': [{'key': 'feature_key'}]}])
        self.assertTrue(repo.ready)
        repo.update_features.assert_called()

    def test_notify_func_no_features(self):
        repo = featurehub_client.FeatureHubRepository()
        repo.update_features = MagicMock()
        repo.notify('FAILED', [{}])
        self.assertFalse(repo.ready)
        repo.update_features.assert_not_called()


if __name__ == '__main__':
    unittest.main()
