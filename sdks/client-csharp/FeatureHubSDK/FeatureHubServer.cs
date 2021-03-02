
namespace FeatureHubSDK
{
  public delegate IEdgeService EdgeServiceSource();

  public interface IFeatureHubConfig
  {
    string Url { get; }
    bool ServerEvaluation { get; }
    IFeatureRepositoryContext Repository { get; set; }
    IEdgeService EdgeService { get; set; }

    IClientContext NewContext(IFeatureRepositoryContext repository = null, EdgeServiceSource edgeServiceSource = null);
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

    public IEdgeService EdgeService { get; set; }

    public IFeatureRepositoryContext Repository { get; set; }
    
    public IClientContext NewContext(IFeatureRepositoryContext repository = null, EdgeServiceSource edgeServiceSource = null)
    {
      if (_serverEvaluation)
      {
        if (repository != null && edgeServiceSource != null)
        {
          return new ServerEvalFeatureContext(repository, this, edgeServiceSource);
        }

        if (Repository != null && EdgeService != null)
        {
          return new ServerEvalFeatureContext(Repository, this, () => EdgeService);
        }

        return new ServerEvalFeatureContext(this);
      }

      if (repository != null && edgeServiceSource != null)
      {
        return new ClientEvalFeatureContext(repository, this, edgeServiceSource);
      }

      if (Repository != null && EdgeService != null)
      {
        return new ClientEvalFeatureContext(Repository, this, () => EdgeService);
      }

      return new ClientEvalFeatureContext(this);
    }

    public string Url => _url;
  }
}
