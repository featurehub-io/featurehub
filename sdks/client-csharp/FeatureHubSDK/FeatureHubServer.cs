using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using IO.FeatureHub.SSE.Model;
using LaunchDarkly.EventSource;

namespace FeatureHubSDK
{
  public interface IFeatureHubEdgeUrl
  {
    string Url { get; }
    bool ServerEvaluation { get; }
  }

  public class FeatureHubEdgeUrl : IFeatureHubEdgeUrl
  {
    private readonly string _url;
    private readonly bool _serverEvaluation;

    public FeatureHubEdgeUrl(string edgeUrl, string sdkKey)
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
