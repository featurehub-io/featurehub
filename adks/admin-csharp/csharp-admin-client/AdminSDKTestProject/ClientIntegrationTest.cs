using System.Collections.Generic;
using System.Net;
using System.Threading.Tasks;
using IO.FeatureHub.MR.Api;
using IO.FeatureHub.MR.Client;
using IO.FeatureHub.MR.Model;
using NUnit.Framework;

namespace AdminSDKTestProject
{
  public class Tests
  {
    [SetUp]
    public void Setup()
    {
    }

    [Test]
    async public Task ClientFeaturesAvailable()
    {
      var config = new Configuration(new Dictionary<string, string>(), new Dictionary<string, string>(),
        new Dictionary<string, string>(), "http://localhost:8085");
      var authService = new AuthServiceApi(config);
      var login = await authService.LoginWithHttpInfoAsync(new UserCredentials(email: "test@mailinator.com", password: "password123"));
      Assert.AreEqual(HttpStatusCode.OK, login.StatusCode);
      config.AccessToken = login.Data.AccessToken;
      var portfolioService = new PortfolioServiceApi(config);
      var portfolios = await portfolioService.FindPortfoliosAsync(includeApplications: true);
      Assert.AreEqual(1, portfolios.Count);
      Assert.AreEqual("Test Portfolio", portfolios[0].Name);
      Assert.AreEqual(1, portfolios[0].Applications.Count);
      Assert.AreEqual("Test Application", portfolios[0].Applications[0].Name);
      var featureService = new FeatureServiceApi(config);
      var features =
        await featureService.FindAllFeatureAndFeatureValuesForEnvironmentsByApplicationAsync(portfolios[0]
          .Applications[0].Id);
      Assert.AreEqual(5, features.Features.Count);
    }
  }
}
