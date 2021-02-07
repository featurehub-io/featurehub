using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using IO.FeatureHub.SSE.Model;
using LaunchDarkly.EventSource;

namespace FeatureHubSDK
{
  public interface IEdgeService
  {
    void ContextChange(Dictionary<string, List<string>> attributes);
    bool ClientEvaluation { get; }
    void Close();
  }

  public class EventServiceListener : IEdgeService
  {
    private EventSource _eventSource;
    private readonly IFeatureHubEdgeUrl _featureHost;
    private readonly IFeatureHubNotify _repository;
    private string _xFeatureHubHeader = null;

    public EventServiceListener(IFeatureHubNotify repository, IFeatureHubEdgeUrl edgeUrl)
    {
      _repository = repository;
      _featureHost = edgeUrl;
    }

    public void ContextChange(Dictionary<string, List<string>> attributes)
    {
      if (_featureHost.ServerEvaluation && attributes != null && attributes.Count != 0)
      {
        var newHeader = string.Join(",",
          attributes.Select((e) => e.Key + "=" +
                                   HttpUtility.UrlEncode(string.Join(",", e.Value))).OrderBy(u => u));

        if (newHeader != _xFeatureHubHeader)
        {
          _xFeatureHubHeader = newHeader;

          if (_eventSource.ReadyState == ReadyState.Open || _eventSource.ReadyState == ReadyState.Connecting)
          {
            _eventSource.Close();
            Init();
          }
        }
      }
    }

    public bool ClientEvaluation => !_featureHost.ServerEvaluation;

    private Dictionary<string, string> BuildContextHeader()
    {
      var headers = new Dictionary<string, string>();

      if (_featureHost.ServerEvaluation && _xFeatureHubHeader != null)
      {
        headers.Add("x-featurehub", _xFeatureHubHeader);
      }

      return headers;
    }

    public void Init()
    {
      var config = new Configuration(uri: new UriBuilder(_featureHost.Url).Uri,
        requestHeaders: _featureHost.ServerEvaluation ? BuildContextHeader() : null);

      _eventSource = new EventSource(config);

      _eventSource.MessageReceived += (sender, args) =>
      {
        SSEResultState? state;
        switch (args.EventName)
        {
          case "features":
            state = SSEResultState.Features;
            break;
          case "feature":
            state = SSEResultState.Feature;
            break;
          case "failure":
            state = SSEResultState.Failure;
            break;
          case "delete_feature":
            state = SSEResultState.Deletefeature;
            break;
          default:
            state = null;
            break;
        }

        if (state == null) return;

        _repository.Notify(state.Value, args.Message.Data);

        if (state == SSEResultState.Failure)
        {
          _eventSource.Close();
        }
      };

      _eventSource.StartAsync();
    }

    public void Close()
    {
      _eventSource.Close();
    }
  }
}
