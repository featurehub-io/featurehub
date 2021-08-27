import unittest
import featurehub_repository
from fh_state_base_holder import FeatureStateBaseHolder


class FeatureHubClientTest(unittest.TestCase):

    def test_notify_func(self):
        repo = featurehub_repository.FeatureHubRepository()
        repo.notify('FEATURES', [{'features': [{'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE',
                                                 'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': False, 'strategies': []}]}])
        self.assertTrue(repo.ready)

        expected = FeatureStateBaseHolder({'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE',
                                           'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': False, 'strategies': []},)

        self.assertEqual(repo.features, repo.features | {'FEATURE_TITLE_TO_UPPERCASE': expected}, repo.features)

    def test_notify_func_update_version(self):
        repo = featurehub_repository.FeatureHubRepository()
        repo.notify('FEATURES', [{'features': [{'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE',
                                                'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': False, 'strategies': []}]}])
        self.assertTrue(repo.ready)

        expected = FeatureStateBaseHolder({'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE',
                                           'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': False, 'strategies': []},)

        self.assertEqual(repo.features, repo.features | {'FEATURE_TITLE_TO_UPPERCASE': expected}, repo.features)

        repo.notify('FEATURES', [{'features': [{'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE',
                                                'l': True, 'version': 2, 'type': 'BOOLEAN', 'value': True, 'strategies': []}]}])

        expected2 = FeatureStateBaseHolder({'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE',
                                           'l': True, 'version': 2, 'type': 'BOOLEAN', 'value': True, 'strategies': []},)

        self.assertEqual(repo.features, repo.features | {'FEATURE_TITLE_TO_UPPERCASE': expected2}, repo.features)

    def test_notify_func_add_new_feature(self):
        repo = featurehub_repository.FeatureHubRepository()
        repo.notify('FEATURES', [{'features': [{'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE',
                                                'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': False, 'strategies': []}]}])
        self.assertTrue(repo.ready)

        expected = FeatureStateBaseHolder({'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_TITLE_TO_UPPERCASE',
                                           'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': False, 'strategies': []},)

        self.assertEqual(repo.features, repo.features | {'FEATURE_TITLE_TO_UPPERCASE': expected}, repo.features)

        repo.notify('FEATURES', [{'features': [{'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_NEW',
                                                'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': True, 'strategies': []}]}])

        expected2 = FeatureStateBaseHolder({'id': '649b3792-1774-4bd5-b550-973ec6340531', 'key': 'FEATURE_NEW',
                                            'l': True, 'version': 1, 'type': 'BOOLEAN', 'value': True, 'strategies': []},)

        self.assertEqual(repo.features, repo.features | {'FEATURE_NEW': expected2}, repo.features)

    def test_notify_func_no_features(self):
        repo = featurehub_repository.FeatureHubRepository()
        repo.notify('FAILED', None)
        self.assertFalse(repo.ready)
        self.assertEqual(repo.features, repo.features | {}, repo.features)


if __name__ == '__main__':
    unittest.main()
