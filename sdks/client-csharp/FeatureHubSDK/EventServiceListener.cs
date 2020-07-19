using System;
using System.Threading.Tasks;
using IO.FeatureHub.SSE.Model;
using LaunchDarkly.EventSource;

namespace FeatureHubSDK
{
  public class EventServiceListener
  {
    private EventSource _eventSource;
    private Task _started;

    public void Init(string url, FeatureHubRepository repository)
    {
      var config = new Configuration(uri: new UriBuilder(url).Uri);
      _eventSource = new EventSource(config);
      _eventSource.MessageReceived += (sender, args) =>
      {
        // Console.WriteLine($"{args.EventName}:\n\t {args.Message.Data}");

        SSEResultState? state = args.EventName switch
        {
          "features" => SSEResultState.Features,
          "feature" => SSEResultState.Feature,
          "failure" => SSEResultState.Failure,
          "delete_feature" => SSEResultState.Deletefeature,
          _ => null
        };

        if (state == null) return;

        repository.Notify(state.Value, args.Message.Data);

        if (state == SSEResultState.Failure)
        {
          _eventSource.Close();
        }
      };

      _started = _eventSource.StartAsync();
    }

    public void Close()
    {
      _eventSource.Close();
    }
  }
}
