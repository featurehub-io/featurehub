using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Web;
using IO.FeatureHub.SSE.Model;
using Newtonsoft.Json;
using Common.Logging; // because dependent library does

namespace FeatureHubSDK
{
  public enum Readyness
  {
    /// <summary>
    /// NotReady - there have been no features delivered as yet.
    /// </summary>
    NotReady,
    /// <summary>
    /// Ready - the initial set of features has been delivered and we are now ready.
    /// </summary>
    Ready,
    /// <summary>
    /// The connection failed because the URL was wrong or some other failure even happened.
    /// </summary>
    Failed
  }

  public interface IFeatureStateHolder
  {
    /// <summary>
    /// if the value is not null, exists returns true
    /// </summary>
    bool Exists { get; }
    /// <summary>
    /// Type is a bool. It will only be null if the type of Feature is not a bool.
    /// </summary>
    bool? BooleanValue { get; }
    /// <summary>
    /// the type is a string and returned as such
    /// </summary>
    string StringValue { get; }
    /// <summary>
    /// A numeric value. This could be an integer or a double.
    /// </summary>
    double? NumberValue { get; }
    /// <summary>
    /// this is just the same as StringValue, no attempt to decode into JSON is done as it is easier for the end user to decode it into
    /// the format they require.
    /// </summary>
    string JsonValue { get; }
    /// <summary>
    /// The KEY of this feature
    /// </summary>
    string Key { get; }
    /// <summary>
    /// The type of this feature. Null if we start listening for a feature before it is Ready or it never exists.
    /// </summary>
    FeatureValueType? Type { get; }
    /// <summary>
    /// The raw value as it came down the wire.
    /// </summary>
    object Value { get; }
    /// <summary>
    /// The version of the current feature
    /// </summary>
    long? Version { get; }
    /// <summary>
    /// Triggered when the value changes
    /// </summary>
    event EventHandler<IFeatureStateHolder> FeatureUpdateHandler;
    //IFeatureStateHolder Copy();
  }

  public interface IClientContext
  {
    event EventHandler<string> ContextUpdateHandler;

    IClientContext UserKey(string key);
    IClientContext SessionKey(string key);
    IClientContext Device(StrategyAttributeDeviceName device);
    IClientContext Platform(StrategyAttributePlatformName platform);
    IClientContext Country(StrategyAttributeCountryName country);
    IClientContext Attr(string key, string value);
    IClientContext Attrs(string key, List<string> values);
    IClientContext Clear();
    void Build();
    string GenerateHeader();
  }

  internal class ClientContext : IClientContext
  {
    private static readonly ILog Log = LogManager.GetLogger<ClientContext>();
    private readonly Dictionary<string, List<string>> _attributes = new Dictionary<string,List<string>>();
    public event EventHandler<string> ContextUpdateHandler;
    public IClientContext UserKey(string key)
    {
      _attributes["userkey"] = new List<string>{key};
      return this;
    }

    private static string GetEnumMemberValue(Enum enumValue)
    {
      var type = enumValue.GetType();
      var info = type.GetField(enumValue.ToString());
      var da = (EnumMemberAttribute[])(info.GetCustomAttributes(typeof(EnumMemberAttribute), false));

      if (da.Length > 0)
        return da[0].Value;
      else
        return string.Empty;
    }

    public IClientContext SessionKey(string key)
    {
      _attributes["session"] = new List<string>{key};
      return this;
    }

    public IClientContext Device(StrategyAttributeDeviceName device)
    {
      _attributes["device"] = new List<string>{GetEnumMemberValue(device)};
      return this;
    }

    public IClientContext Platform(StrategyAttributePlatformName platform)
    {
      _attributes["platform"] = new List<string>{GetEnumMemberValue(platform)};
      return this;
    }

    public IClientContext Country(StrategyAttributeCountryName country)
    {
      _attributes["country"] = new List<string>{GetEnumMemberValue(country)};
      return this;
    }

    public IClientContext Attr(string key, string value)
    {
      _attributes[key] = new List<string>{value};
      return this;
    }

    public IClientContext Attrs(string key, List<string> values)
    {
      _attributes[key] = values;
      return this;
    }

    public IClientContext Clear()
    {
      _attributes.Clear();
      return this;
    }

    public void Build()
    {
      var header = GenerateHeader();

      var handler = ContextUpdateHandler;
      try
      {
        handler?.Invoke(this, header);
      }
      catch (Exception e)
      {
        Log.Error($"Failed to process client context update", e);
      }
    }

    public string GenerateHeader()
    {
      if (_attributes.Count == 0)
      {
        return null;
      }

      return string.Join(",",
        _attributes.Select((e) => e.Key + "=" +
                                  HttpUtility.UrlEncode(string.Join(",", e.Value))));
    }
  }

  internal class FeatureStateBaseHolder : IFeatureStateHolder
  {
    private static readonly ILog log = LogManager.GetLogger<FeatureStateBaseHolder>();
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

    public string JsonValue => _feature?.Type == FeatureValueType.JSON ? Convert.ToString(_value) : null;
    public string Key => _feature?.Key;
    public FeatureValueType? Type => _feature?.Type;
    public object Value => _value;

    public long? Version => _feature?.Version;

    // public EventHandler<IFeatureStateHolder> FeatureUpdateHandler => _featureUpdateHandler;
    // public IFeatureStateHolder Copy()
    // {
    //   throw new NotImplementedException();
    // }

    public FeatureState FeatureState
    {
      set
      {
        _feature = value;
        var oldVal = _value;
        _value = value.Value;

        // did the value change? if so, tell everyone listening via event handler
        if ((_value != null && !_value.Equals(oldVal)) || (oldVal != null && !oldVal.Equals(_value)))
        {
          var handler = FeatureUpdateHandler;
          try
          {
            handler?.Invoke(this, this);
          }
          catch (Exception e)
          {
            log.Error($"Failed to process update for feature {Key}", e);
          }
        }
      }
    }
  }

  public class FeatureHubRepository
  {
    private static readonly ILog log = LogManager.GetLogger<FeatureHubRepository>();
    private readonly Dictionary<string, FeatureStateBaseHolder> _features =
      new Dictionary<string, FeatureStateBaseHolder>();
    private readonly IClientContext _clientContext = new ClientContext();

    private Readyness _readyness = Readyness.NotReady;
    public event EventHandler<Readyness> ReadynessHandler;
    public event EventHandler<FeatureHubRepository> NewFeatureHandler;

    public Readyness Readyness => _readyness;

    public IClientContext ClientContext => _clientContext;

    private void TriggerReadyness()
    {
      var handler = ReadynessHandler;
      try
      {
        handler?.Invoke(this, _readyness);
      }
      catch (Exception e)
      {
        log.Error($"Failed to indicate readyness change to {_readyness}", e);
      }
    }

    private void TriggerNewUpdate()
    {
      var handler = NewFeatureHandler;
      try
      {
        handler?.Invoke(this, this);
      }
      catch (Exception e)
      {
        log.Error("Failed to indicate trigger new feature change.", e);
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
      var keyExists = _features.ContainsKey(fs.Key);
      FeatureStateBaseHolder holder = keyExists ? _features[fs.Key] : null;
      if (holder?.Key == null)
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


}
