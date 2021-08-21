import unittest
import featurehub_client


class FeatureHubClientTest(unittest.TestCase):

    def test_build_url_func(self):
        url = featurehub_client.build_url('http://localhost', ['abc', '123', 'xyz'])
        self.assertEqual("http://localhost/features?sdkUrl=abc&sdkUrl=123&sdkUrl=xyz", url)

    def test_build_url_func_err(self):
        self.assertRaises(RuntimeError, featurehub_client.build_url, 'http://localhost', [])

    def test_build_url_func_err2(self):
        self.assertRaises(RuntimeError, featurehub_client.build_url, '', [123])

    def test_make_object(self):
        repo = featurehub_client.FeatureHubRepository()
        fh_client = featurehub_client.FeatureHubClient("http://localhost", ["abc", "123"], repo)
        self.assertIsInstance(fh_client, featurehub_client.FeatureHubClient)


if __name__ == '__main__':
  unittest.main()
