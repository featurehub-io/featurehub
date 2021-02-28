using System.Collections.Generic;
using System.Threading.Tasks;
using IO.FeatureHub.SSE.Model;

namespace FeatureHubSDK
{

  public interface IClientContext
  {
    IClientContext UserKey(string key);
    IClientContext SessionKey(string key);
    IClientContext Device(StrategyAttributeDeviceName device);
    IClientContext Platform(StrategyAttributePlatformName platform);
    IClientContext Country(StrategyAttributeCountryName country);
    /// expects semantic version Maj.Minor.Patch
    IClientContext Version(string version);
    IClientContext Attr(string key, string value);
    IClientContext Attrs(string key, List<string> values);
    IClientContext Clear();

    string GetAttr(string key, string defaultValue);

    string DefaultPercentageKey { get; }

    IFeature this[string name] { get; }

    bool IsEnabled(string name);

    Task<IClientContext> Build();

    IEdgeService EdgeService { get; }
    IFeatureHubRepository Repository { get; }

    IFeatureHubRepository LogAnalyticEvent(string action, string user = null, Dictionary<string, string> other = null);

    void Close();
  }
}
