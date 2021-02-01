using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using IO.FeatureHub.SSE.Model;
using LaunchDarkly.EventSource;

namespace FeatureHubSDK
{
  public class EventServiceListener
  {
    private EventSource _eventSource;
    private string _url;
    private FeatureHubRepository _repository;
    private bool _initialized = false;
    private string xFeatureHubHeader = null;

    public void Init(string url, FeatureHubRepository repository)
    {
      if (!_initialized)
      {
        _initialized = true;
        _url = url;
        _repository = repository;

        xFeatureHubHeader = _repository.HostedClientContext.GenerateHeader();

        _repository.HostedClientContext.ContextUpdateHandler += (sender, header) =>
        {
          if (header == xFeatureHubHeader || (_eventSource.ReadyState != ReadyState.Open && _eventSource.ReadyState != ReadyState.Connecting)) return;

          xFeatureHubHeader = header;
          _eventSource.Close();
          Init(_url, _repository);
        };
      }

      var headers = new Dictionary<string, string>();
      if (xFeatureHubHeader != null)
      {
        headers.Add("x-featurehub", xFeatureHubHeader);
      }
      var config = new Configuration(uri: new UriBuilder(url).Uri, requestHeaders: headers);
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

        repository.Notify(state.Value, args.Message.Data);

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
