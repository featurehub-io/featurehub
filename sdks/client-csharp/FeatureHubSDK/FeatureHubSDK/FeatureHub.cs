using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using IO.FeatureHub.SSE.Model;
using LaunchDarkly.EventSource;
using Newtonsoft.Json;

namespace FeatureHubSDK
{
  public enum Readyness
  {
    NotReady,
    Ready,
    Failed
  }

  public interface IFeatureStateHolder
  {
    bool Exists { get; }
    bool? BooleanValue { get; }
    string StringValue { get; }
    double? NumberValue { get; }
    object JsonValue { get; }
    string Key { get; }
    FeatureValueType? Type { get; }
    object Value { get; }
    long? Version { get; }
    event EventHandler<IFeatureStateHolder> FeatureUpdateHandler;
    IFeatureStateHolder Copy();
  }

  internal class FeatureStateBaseHolder : IFeatureStateHolder
  {
    private object _value;
    private FeatureState _feature;
    public event EventHandler<IFeatureStateHolder> FeatureUpdateHandler;

    public FeatureStateBaseHolder(FeatureStateBaseHolder fs)
    {
      if (fs != null)
      {
        FeatureUpdateHandler = fs.FeatureUpdateHandler;
      }
    }

    public bool Exists => _value != null;

    public bool? BooleanValue => _feature?.Type == FeatureValueType.BOOLEAN ? (bool?) Convert.ToBoolean(_value) : null;

    public string StringValue => _feature?.Type == FeatureValueType.STRING ? Convert.ToString(_value) : null;

    public double? NumberValue => _feature?.Type == FeatureValueType.NUMBER ? (double?) Convert.ToDouble(_value) : null;

    public object JsonValue => _feature?.Type == FeatureValueType.JSON ? _value : null;
    public string Key => _feature == null ? null : _feature.Key;
    public FeatureValueType? Type => _feature == null ? null : _feature.Type;
    public object Value => _value;

    public long? Version => _feature == null ? (long?) null : _feature.Version;

    // public EventHandler<IFeatureStateHolder> FeatureUpdateHandler => _featureUpdateHandler;
    public IFeatureStateHolder Copy()
    {
      throw new NotImplementedException();
    }

    public FeatureState FeatureState
    {
      set
      {
        _feature = value;
        var oldVal = _value;
        _value = value.Value;

        // did the value change? if so, tell everyone listening via event handler
        if (_value != oldVal)
        {
          EventHandler<IFeatureStateHolder> handler = FeatureUpdateHandler;
          if (handler != null) // any listeners?
          {
            FeatureUpdateHandler(this, this);
          }
        }
      }

      get { return _feature; }
    }
  }

  public class FeatureHubRepository
  {
    private readonly Dictionary<string, FeatureStateBaseHolder> _features =
      new Dictionary<string, FeatureStateBaseHolder>();

    private Readyness _readyness = Readyness.NotReady;
    public event EventHandler<Readyness> ReadynessHandler;
    public event EventHandler<FeatureHubRepository> NewFeatureHandler;

    private void TriggerReadyness()
    {
      EventHandler<Readyness> handler = ReadynessHandler;
      if (handler != null)
      {
        handler(this, _readyness);
      }
    }

    private void TriggerNewUpdate()
    {
      EventHandler<FeatureHubRepository> handler = NewFeatureHandler;
      if (handler != null)
      {
        handler(this, this);
      }
    }

    // Notify
    public void Notify(SSEResultState state, string data)
    {
      // Console.WriteLine($"received {state} with object {data}");

      switch (state)
      {
        case SSEResultState.Ack:
          break;
        case SSEResultState.Bye:
          // swap to not ready and let everyone know
          _readyness = Readyness.NotReady;
          TriggerReadyness();
          break;
        case SSEResultState.Failure:
          _readyness = Readyness.Failed;
          TriggerReadyness();
          break;
        case SSEResultState.Features:
          if (data != null)
          {
            List<FeatureState> features = JsonConvert.DeserializeObject<List<FeatureState>>(data);
            var updated = false;
            foreach (var featureState in features)
            {
              updated = FeatureUpdate(featureState) || updated;
            }

            if (_readyness != Readyness.Ready)
            {
              // are we newly ready?
              _readyness = Readyness.Ready;
              TriggerReadyness();
            }

            // we updated something, so let the folks know
            if (updated)
            {
              TriggerNewUpdate();
            }
          }

          break;
        case SSEResultState.Feature:
          if (data != null)
          {
            if (FeatureUpdate(JsonConvert.DeserializeObject<FeatureState>(data)))
            {
              TriggerNewUpdate();
            }
          }

          break;
        case SSEResultState.Deletefeature:
          if (data != null)
          {
            DeleteFeature(JsonConvert.DeserializeObject<FeatureState>(data));
          }

          break;
        default:
          throw new ArgumentOutOfRangeException(nameof(state), state, null);
      }
    }

    private void DeleteFeature(FeatureState fs)
    {
      if (_features.Remove(fs.Key))
      {
        TriggerNewUpdate();
      }
    }

    // update the feature if its version is greater than the version we currently store
    private bool FeatureUpdate(FeatureState fs)
    {
      if (fs == null)
      {
        return false;
      }

      var keyExists = _features.ContainsKey(fs.Key);
      FeatureStateBaseHolder holder = keyExists ? _features[fs.Key] : null;
      if (holder == null || holder.Key == null)
      {
        holder = new FeatureStateBaseHolder(holder);
      }
      else if (holder.Version != null && holder.Version >= fs.Version)
      {
        // Console.WriteLine($"discarding {fs}");
        return false;
      }

      // Console.WriteLine($"storing {fs}");

      holder.FeatureState = fs;
      if (keyExists)
      {
        _features[fs.Key] = holder;
      }
      else
      {
        _features.Add(fs.Key, holder);
      }

      return true;
    }

    public IFeatureStateHolder FeatureState(string key)
    {
      if (!_features.ContainsKey(key))
      {
        _features.Add(key, new FeatureStateBaseHolder(null));
      }

      return _features[key];
    }
  }

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
