
namespace FeatureHubSDK
{
  public interface IFeatureHubConfig
  {
    string Url { get; }
    bool ServerEvaluation { get; }
  }

  public class FeatureHubConfig : IFeatureHubConfig
  {
    private readonly string _url;
    private readonly bool _serverEvaluation;

    public FeatureHubConfig(string edgeUrl, string sdkKey)
    {
      _serverEvaluation = sdkKey != null && sdkKey.Contains("*"); // two part keys are server evaluated

      if (edgeUrl.EndsWith("/"))
      {
        edgeUrl = edgeUrl.Substring(0, edgeUrl.Length - 1);
      }

      if (edgeUrl.EndsWith("/features"))
      {
        edgeUrl = edgeUrl.Substring(0, edgeUrl.Length - "/features".Length);
      }

      _url = edgeUrl + "/features/" + sdkKey;
    }

    public bool ServerEvaluation => _serverEvaluation;

    public string Url => _url;
  }
}
