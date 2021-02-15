using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using IO.FeatureHub.SSE.Model;
using LaunchDarkly.EventSource;

namespace FeatureHubSDK
{
  public interface IEdgeService
  {
    Task ContextChange(string header);
    bool ClientEvaluation { get; }

    bool IsRequiresReplacementOnHeaderChange { get;  }
    void Close();
    void Poll();
  }

  public class EventServiceListener : IEdgeService
  {
    private EventSource _eventSource;
    private readonly IFeatureHubConfig _featureHost;
    private readonly IFeatureHubNotify _repository;
    private string _xFeatureHubHeader = null;

    public EventServiceListener(IFeatureHubNotify repository, IFeatureHubConfig config)
    {
      _repository = repository;
      _featureHost = config;

      // tell the repository about how evaluation works
      // this means features don't need to know about the IEdgeService
      _repository.ServerSideEvaluation = config.ServerEvaluation;
    }

    public async Task ContextChange(string newHeader)
    {
      if (_featureHost.ServerEvaluation)
      {
        if (newHeader != _xFeatureHubHeader)
        {
          _xFeatureHubHeader = newHeader;

          if (_eventSource == null || _eventSource.ReadyState == ReadyState.Open || _eventSource.ReadyState == ReadyState.Connecting)
          {
            _eventSource?.Close();

            var promise = new TaskCompletionSource<Readyness>();

            EventHandler<Readyness> handler = (sender, r) =>
            {
              promise.TrySetResult(r);
            };

            _repository.ReadynessHandler += handler;

            Init();

            await promise.Task;

            _repository.ReadynessHandler -= handler;
          }
        }
      }
    }

    public bool ClientEvaluation => !_featureHost.ServerEvaluation;

    // "close" works on this events source and doesn't hang
    public bool IsRequiresReplacementOnHeaderChange => false;

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

    public void Poll()
    { // do nothing, is SSE
    }
  }
}
