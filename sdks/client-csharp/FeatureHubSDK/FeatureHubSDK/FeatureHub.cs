using System;
using System.Collections.Generic;
using IO.FeatureHub.SSE.Model;
using Newtonsoft.Json;


namespace FeatureHubSDK
{
  // wait on API compilation

  public enum Readyness
  {
    NotReady, Ready, Failed
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
    EventHandler<IFeatureStateHolder> FeatureUpdateHandler { get; }
    IFeatureStateHolder Copy();
  }

  internal class FeatureStateBaseHolder : IFeatureStateHolder
  {
    private object _value;
    private FeatureState _feature;
    private EventHandler<IFeatureStateHolder> _featureUpdateHandler;

    public FeatureStateBaseHolder(FeatureStateBaseHolder fs)
    {
      if (fs != null)
      {
        _featureUpdateHandler = fs._featureUpdateHandler;
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
    public EventHandler<IFeatureStateHolder> FeatureUpdateHandler => _featureUpdateHandler;
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
          _featureUpdateHandler(this, this);
        }
      }

      get
      {
        return _feature;
      }
    }
  }

  public class ClientRepository
  {
    private bool _hasReceivedInitialState = false;
    private readonly Dictionary<string,FeatureStateBaseHolder> _features =
      new Dictionary<string,FeatureStateBaseHolder>();

    private Readyness _readyness = Readyness.NotReady;
    private EventHandler<Readyness> _readynessHandler;
    private EventHandler<ClientRepository> _newFeatureHandler;

    public EventHandler<Readyness> ReadynessHandler => _readynessHandler;
    public EventHandler<ClientRepository> NewFeatureHandler => _newFeatureHandler;

    // Notify
    public void Notify(SSEResultState state, string data)
    {
      if (state == null)
      {
        return;
      }

      Console.WriteLine($"received {state} with object {data}");

      switch (state)
      {
        case SSEResultState.Ack:
          break;
        case SSEResultState.Bye:
          // swap to not ready and let everyone know
          _readyness = Readyness.NotReady;
          ReadynessHandler(this, _readyness);
          break;
        case SSEResultState.Failure:
          _readyness = Readyness.Failed;
          ReadynessHandler(this, _readyness);
          break;
        case SSEResultState.Features:
          if (data != null)
          {
            List<FeatureState> features = JsonConvert.DeserializeObject<List<FeatureState>>(data);
            var updated = false;
            foreach (var featureState in features)
            {
              updated = updated || FeatureUpdate(featureState);
            }

            _hasReceivedInitialState = true;
            if (_readyness != Readyness.Ready)
            { // are we newly ready?
              _readyness = Readyness.Ready;
              ReadynessHandler(this, _readyness);
            }

            // we updated something, so let the folks know
            if (updated)
            {
              NewFeatureHandler(this, this);
            }
          }
          break;
        case SSEResultState.Feature:
          if (data != null)
          {
            if (FeatureUpdate(JsonConvert.DeserializeObject<FeatureState>(data)))
            {
              NewFeatureHandler(this, this);
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
        NewFeatureHandler(this, this);
      }
    }

    // update the feature if its version is greater than the version we currently store
    private bool FeatureUpdate(FeatureState fs)
    {
      if (fs == null)
      {
        return false;
      }

      FeatureStateBaseHolder holder = _features[fs.Key];
      if (holder == null || holder.Key == null)
      {
        holder = new FeatureStateBaseHolder(holder);
      } else if (holder.Version != null && holder.Version >= fs.Version)
      {
        return false;
      }

      holder.FeatureState = fs;
      _features[fs.Key] = holder;

      return true;
    }
  }


}
