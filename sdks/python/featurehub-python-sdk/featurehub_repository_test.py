import unittest
import featurehub_repository


class FeatureHubClientTest(unittest.TestCase):

    def test_notify_func(self):
        repo = featurehub_repository.FeatureHubRepository()
        repo.notify('FEATURES', [{'features': [{'key': 'feature_key'}]}])
        self.assertTrue(repo.ready)
        self.assertEqual({'feature_key': {'key': 'feature_key'}}, repo.features)

    def test_notify_func_no_features(self):
        repo = featurehub_repository.FeatureHubRepository()
        repo.notify('FAILED', None)
        self.assertFalse(repo.ready)
        self.assertEqual({}, repo.features)


if __name__ == '__main__':
    unittest.main()
