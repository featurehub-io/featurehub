using System;
using System.Threading.Tasks;
using IO.FeatureHub.SSE.Model;
using LaunchDarkly.EventSource;

namespace FeatureHubSDK
{
  public class EventServiceListener
  {
    private EventSource _eventSource;

    public void Init(string url, FeatureHubRepository repository)
    {
      var config = new Configuration(uri: new UriBuilder(url).Uri);
      _eventSource = new EventSource(config);
      _eventSource.MessageReceived += (sender, args) =>
      {
        // Console.WriteLine($"{args.EventName}:\n\t {args.Message.Data}");

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
