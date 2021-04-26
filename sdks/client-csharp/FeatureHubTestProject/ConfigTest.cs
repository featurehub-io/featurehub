
using FeatureHubSDK;
using NUnit.Framework;

namespace FeatureHubTestProject
{
  class ConfigTest
  {
    [Test]
    public void EnsureConfigCorrectlyDeterminesUrl()
    {
      var cfg = new EdgeFeatureHubConfig("http://localhost:80/", "123*123");
      Assert.IsTrue(!cfg.ServerEvaluation);
      Assert.AreEqual("http://localhost:80/features/123*123", cfg.Url);

      cfg = new EdgeFeatureHubConfig("http://localhost:80", "123");
      Assert.IsTrue(cfg.ServerEvaluation);
      Assert.AreEqual("http://localhost:80/features/123", cfg.Url);
    }


  }
}
