
namespace FeatureHubSDK
{
  public delegate IEdgeService EdgeServiceSource();

  public interface IFeatureHubConfig
  {
    string Url { get; }
    bool ServerEvaluation { get; }

    IClientContext NewContext();

    IClientContext NewContext(IFeatureRepositoryContext repository, EdgeServiceSource edgeServiceSource);
  }

  public class FeatureHubConfig : IFeatureHubConfig
  {
    private readonly string _url;
    private readonly bool _serverEvaluation;

    public FeatureHubConfig(string edgeUrl, string sdkKey)
    {
      _serverEvaluation = sdkKey != null && !sdkKey.Contains("*"); // two part keys are server evaluated

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

    public IClientContext NewContext()
    {
      if (_serverEvaluation)
      {
        return new ServerEvalFeatureContext(this);
      }

      return new ClientEvalFeatureContext(this);
    }

    public IClientContext NewContext(IFeatureRepositoryContext repository, EdgeServiceSource edgeServiceSource)
    {
      if (_serverEvaluation)
      {
        return new ServerEvalFeatureContext(repository, this, edgeServiceSource);
      }

      return new ClientEvalFeatureContext(repository, this, edgeServiceSource);
    }

    public string Url => _url;
  }
}
