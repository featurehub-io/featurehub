

using System;
using System.Collections.Generic;

namespace FeatureHubSDK
{
  public delegate IEdgeService EdgeServiceSource(IFeatureRepositoryContext repository, IFeatureHubConfig config);

  public class FeatureHubConfig
  {
    public static EdgeServiceSource defaultEdgeProvider = (repository, config) => {
      return new EventServiceListener(repository, config);
    };

  }

  public interface IFeatureHubConfig
  {

    string Url { get; }
    bool ServerEvaluation { get; }

    /*
     * Initialise the configuration. This will kick off the event source to connect and attempt to start
     * pushing data into the FeatureHub repository for use in contexts.
     */
    void Init();

    IFeatureRepositoryContext Repository { get; set; }
    IEdgeService EdgeService { get; set; }

    IClientContext NewContext(IFeatureRepositoryContext repository = null, EdgeServiceSource edgeServiceSource = null);

    // is the system ready? use this in your liveness/health check
    Readyness Readyness { get; }

    void AddAnalyticCollector(IAnalyticsCollector collector);
  }

  public class EdgeFeatureHubConfig : IFeatureHubConfig
  {
    private readonly string _url;
    private readonly bool _serverEvaluation;

    public EdgeFeatureHubConfig(string edgeUrl, string sdkKey)
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

    public void Init()
    {
      EdgeService.Poll();
    }

    public bool ServerEvaluation => _serverEvaluation;

    private IEdgeService _edgeService;

    public IEdgeService EdgeService
    {
      get
      {
        if (_edgeService == null)
        {
          _edgeService = FeatureHubConfig.defaultEdgeProvider(this.Repository, this);
        }

        return _edgeService;
      }
      set
      {
        _edgeService = value;
      }
    }

    private IFeatureRepositoryContext _repository;

    public IFeatureRepositoryContext Repository
    {
      get
      {
        if (_repository == null)
        {
          _repository = new FeatureHubRepository();
        }

        return _repository;
      }
      set
      {
        _repository = value;
      }
    }

    public IClientContext NewContext(IFeatureRepositoryContext repository = null, EdgeServiceSource edgeServiceSource = null)
    {
      if (repository == null)
      {
        repository = Repository;
      }

      if (edgeServiceSource == null)
      {
        edgeServiceSource = (repo, config) => FeatureHubConfig.defaultEdgeProvider(repo, config);
      }

      if (_serverEvaluation)
      {
        return new ServerEvalFeatureContext(repository, this, edgeServiceSource);
      }

      return new ClientEvalFeatureContext(repository, this, edgeServiceSource);
    }

    public Readyness Readyness => Repository.Readyness;

    public void AddAnalyticCollector(IAnalyticsCollector collector)
    {
      Repository.AddAnalyticCollector(collector);
    }

    public string Url => _url;
  }
}
