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

  public static class FeatureLogging
  {
    // Attach event handler to receive Trace level logs
    public static EventHandler<String> TraceLogger;
    // Attach event handler to receive Debug level logs
    public static EventHandler<String> DebugLogger;
    // Attach event handler to receive Info level logs
    public static EventHandler<String> InfoLogger;
    // Attach event handler to receive Error level logs
    public static EventHandler<String> ErrorLogger;
  }

  public class EventServiceListener : IEdgeService
  {
    private EventSource _eventSource;
    private readonly IFeatureHubConfig _featureHost;
    private readonly IFeatureRepositoryContext _repository;
    private string _xFeatureHubHeader = null;

    public EventServiceListener(IFeatureRepositoryContext repository, IFeatureHubConfig config)
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
      else if (_eventSource == null)
      {
        Init();
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
        backoffResetThreshold: TimeSpan.MaxValue,
        delayRetryDuration: TimeSpan.Zero,
        requestHeaders: _featureHost.ServerEvaluation ? BuildContextHeader() : null);

      if (FeatureLogging.InfoLogger != null)
      {
        FeatureLogging.InfoLogger(this, $"Opening connection to ${_featureHost.Url}");
      }

      _eventSource = new EventSource(config);

      if (FeatureLogging.DebugLogger != null)
      {
        _eventSource.Closed += (sender, args) =>
        {
          FeatureLogging.DebugLogger(this, "source closed");
        };
      }

      _eventSource.MessageReceived += (sender, args) =>
      {
        SSEResultState? state;
        switch (args.EventName)
        {
          case "features":
            state = SSEResultState.Features;
            if (FeatureLogging.TraceLogger != null)
            {
              FeatureLogging.TraceLogger(this, "featurehub: fresh feature set received, ready to rumble");
            }

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
          case "bye":
            state = null;
            if (FeatureLogging.TraceLogger != null)
            {
              FeatureLogging.TraceLogger(this, "featurehub: renewing connection process started");
            }

            break;
          default:
            state = null;
            break;
        }

        if (FeatureLogging.TraceLogger != null)
        {
          FeatureLogging.TraceLogger(this , $"featurehub: The state was {state} with value {args.Message.Data}");
        }

        if (state == null) return;

        _repository.Notify(state.Value, args.Message.Data);

        if (state == SSEResultState.Failure)
        {
          if (FeatureLogging.ErrorLogger != null)
          {
            FeatureLogging.ErrorLogger(this, "featurehub: received a failure so closing");
          }

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
    {
      if (_eventSource == null)
      {
        Init();
      }
    }
  }
}
