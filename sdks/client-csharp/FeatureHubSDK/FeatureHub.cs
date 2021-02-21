using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Threading.Tasks;
using System.Transactions;
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

  public interface IAnalyticsCollector
  {
    void LogEvent(string action, Dictionary<string, string> other, List<IFeature> featureStates);
  }

  public interface IFeature
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
    /// Determines if the feature actually has a value
    /// </summary>
    bool IsEnabled { get; }

    IFeature WithContext(IClientContext context);

    /// <summary>
    /// Triggered when the value changes
    /// </summary>
    event EventHandler<IFeature> FeatureUpdateHandler;
    //IFeatureStateHolder Copy();
  }

  public abstract class BaseClientContext : IClientContext
  {
    private static readonly ILog Log = LogManager.GetLogger<BaseClientContext>();
    protected readonly Dictionary<string, List<string>> _attributes = new Dictionary<string,List<string>>();
    protected readonly IFeatureRepositoryContext _repository;
    protected readonly IFeatureHubConfig _config;

    public IFeatureHubRepository Repository => _repository;
    public abstract void Close();

    public BaseClientContext(IFeatureRepositoryContext repository, IFeatureHubConfig config)
    {
      _repository = repository;
      _config = config;
    }

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

      return da.Length > 0 ? da[0].Value : string.Empty;
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

    public IClientContext Version(string version)
    {
      _attributes["version"] = new List<string> {version};

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

    public string GetAttr(string key, string defaultValue)
    {
      if (_attributes.ContainsKey(key) && _attributes[key].Count > 0)
      {
        return _attributes[key][0];
      }

      return defaultValue;
    }


    public string DefaultPercentageKey => _attributes.ContainsKey("session") ? _attributes["session"][0] : (_attributes.ContainsKey("userkey") ? _attributes["userkey"][0] : null);
    public abstract IFeature this[string name] { get; }


    public bool IsEnabled(string name)
    {
      return this[name].BooleanValue == true;
    }
    public abstract Task<IClientContext> Build();
    public abstract IEdgeService EdgeService { get; }

    public override string ToString()
    {
      var s = "CONTEXT: ";
      foreach (var key in _attributes.Keys)
      {
        s += $"key: {key} = ";
        foreach (var val in _attributes[key])
        {
          s += $"`{val}`,";
        }

        s += "\n";
      }
      return s;
    }
  }


  public interface IFeatureRepositoryContext: IFeatureHubRepository, IFeatureHubNotify
  {

  }

  public class ServerEvalFeatureContext : BaseClientContext
  {
    private readonly EdgeServiceSource _edgeServiceSource;
    private IEdgeService _currentEdgeService;
    private string _xHeader;
    private readonly bool _weCreatedSources;

    public ServerEvalFeatureContext(IFeatureHubConfig config) : base(new FeatureHubRepository(), config)
    {
      _weCreatedSources = true;
      _edgeServiceSource = () => new EventServiceListener(_repository, config);
    }

    public ServerEvalFeatureContext(IFeatureRepositoryContext repository, IFeatureHubConfig config, EdgeServiceSource edgeServiceSource) : base(repository, config)
    {
      _edgeServiceSource = edgeServiceSource;
      _weCreatedSources = false;
    }

    public override IFeature this[string name] => _repository.GetFeature(name);

    public override async Task<IClientContext> Build()
    {
      var newHeader = string.Join(",",
        _attributes.Select((e) => e.Key + "=" +
                                 HttpUtility.UrlEncode(string.Join(",", e.Value))).OrderBy(u => u));

      if (!newHeader.Equals(_xHeader))
      {
        _xHeader = newHeader;
        _repository.NotReady();

        if (_currentEdgeService != null && _currentEdgeService.IsRequiresReplacementOnHeaderChange)
        {
          _currentEdgeService.Close();
          _currentEdgeService = _edgeServiceSource();
        }
      }

      if (_currentEdgeService == null)
      {
        _currentEdgeService = _edgeServiceSource();
      }

      await _currentEdgeService.ContextChange(_xHeader);

      return this;
    }

    public override IEdgeService EdgeService => _currentEdgeService;
    public override void Close()
    {
      if (_weCreatedSources)
      {
        _currentEdgeService?.Close();
      }
    }
  }

  public class ClientEvalFeatureContext : BaseClientContext
  {
    private readonly IEdgeService _edgeService;
    private readonly bool _weCreatedSources;

    public ClientEvalFeatureContext(IFeatureHubConfig config) : base(new FeatureHubRepository(), config)
    {
      _edgeService = new EventServiceListener(_repository, config);
      _weCreatedSources = true;
    }

    public ClientEvalFeatureContext(IFeatureRepositoryContext repository, IFeatureHubConfig config, EdgeServiceSource edgeServiceSource) : base(repository, config)
    {
      _edgeService = edgeServiceSource();
      _weCreatedSources = false;
    }

    public override IFeature this[string name] => _repository.GetFeature(name).WithContext(this);

#pragma warning disable 1998
    public override async Task<IClientContext> Build()
#pragma warning restore 1998
    {
      return this;
    }

    public override IEdgeService EdgeService => _edgeService;

    public override void Close()
    {
      if (_weCreatedSources)
      {
        _edgeService?.Close();
      }
    }
  }

  internal class FeatureStateBaseHolder : IFeature
  {
    private static readonly ILog Log = LogManager.GetLogger<FeatureStateBaseHolder>();
    private FeatureState _feature;
    private readonly ApplyFeature _applyFeature;

    public IFeature WithContext(IClientContext context)
    {
      var fsh = Copy() as FeatureStateBaseHolder;
      fsh.SetContext( context );
      return fsh;
    }

    public event EventHandler<IFeature> FeatureUpdateHandler;
    private IClientContext _context;

    internal void SetContext(IClientContext context)
    {
      _context = context;
    }

    public FeatureStateBaseHolder(FeatureStateBaseHolder fs, ApplyFeature applyFeature)
    {
      _applyFeature = applyFeature;

      if (fs != null)
      {
        FeatureUpdateHandler = fs.FeatureUpdateHandler;
      }
    }

    public IFeature Copy()
    {
      return new FeatureStateBaseHolder(null, _applyFeature) {FeatureState = this._feature};
    }

    public bool Exists => _feature != null;

    private object GetValue(FeatureValueType? type)
    {
      if (type == null || _feature?.Type != type)
      {
        return null;
      }

      if (_context != null)
      {
        Applied matched = _applyFeature.Apply(_feature.Strategies, Key, _feature.Id, _context);

        if (matched.Matched)
        {
          return matched.Value;
        }
      }

      return _feature.Value;
    }

    public bool? BooleanValue
    {
      get
      {
        var val = GetValue(FeatureValueType.BOOLEAN);
        return val == null ? (bool?)null : Convert.ToBoolean(val);
      }
    }

    public string StringValue
    {
      get
      {
        var val = GetValue(FeatureValueType.STRING);
        return val == null ? null : Convert.ToString(val);
      }
    }

    public double? NumberValue
    {
      get
      {
        var val = GetValue(FeatureValueType.NUMBER);
        return val == null ? (double?)null : Convert.ToDouble(val);
      }
    }

    public string JsonValue
    {
      get
      {
        var val = GetValue(FeatureValueType.JSON);
        return val == null ? null : Convert.ToString(val);
      }
    }
    public string Key => _feature?.Key;
    public FeatureValueType? Type => _feature?.Type;
    public object Value => _feature == null ? null : GetValue(_feature.Type);

    public bool IsEnabled => BooleanValue == true;

    public long? Version => _feature?.Version;

    public FeatureState FeatureState
    {
      set
      {
        var oldVal = GetValue(_feature?.Type);
        _feature = value;
        var val = GetValue(_feature?.Type);

        // did the value change? if so, tell everyone listening via event handler
        if (ValueChanged(oldVal, val))
        {
          var handler = FeatureUpdateHandler;
          try
          {
            handler?.Invoke(this, this);
          }
          catch (Exception e)
          {
            Log.Error($"Failed to process update for feature {Key}", e);
          }
        }
      }
    }

    public static bool ValueChanged(object oldVal, object value)
    {
      return (value != null && !value.Equals(oldVal)) || (oldVal != null && !oldVal.Equals(value));
    }
  }

  public interface IFeatureHubRepository
  {
    IFeature GetFeature(string key);

    IFeature this[string name] { get; }
    bool IsEnabled(string name);

    event EventHandler<Readyness> ReadynessHandler;
    event EventHandler<FeatureHubRepository> NewFeatureHandler;
    Readyness Readyness { get; }
    FeatureHubRepository LogAnalyticEvent(string action);
    FeatureHubRepository LogAnalyticEvent(string action, Dictionary<string, string> other);
    FeatureHubRepository AddAnalyticCollector(IAnalyticsCollector collector);
    bool Exists(string key);
  }

  public interface IFeatureHubNotify
  {
    bool ServerSideEvaluation { set; get; }
    event EventHandler<Readyness> ReadynessHandler;
    void Notify(SSEResultState state, string data);
    void NotReady();
  }

  public abstract class AbstractFeatureHubRepository : IFeatureHubRepository
  {
    public abstract IFeature GetFeature(string key);

    public IFeature this[string name] => GetFeature(name);

    public bool IsEnabled(string name)
    {
      return GetFeature(name).IsEnabled;
    }

    public abstract event EventHandler<Readyness> ReadynessHandler;
    public abstract event EventHandler<FeatureHubRepository> NewFeatureHandler;
    public abstract Readyness Readyness { get; }
    public abstract FeatureHubRepository LogAnalyticEvent(string action);
    public abstract FeatureHubRepository LogAnalyticEvent(string action, Dictionary<string, string> other);
    public abstract FeatureHubRepository AddAnalyticCollector(IAnalyticsCollector collector);
    public abstract bool Exists(string key);
  }


  public class FeatureHubRepository : AbstractFeatureHubRepository, IFeatureRepositoryContext
  {
    private static readonly ILog log = LogManager.GetLogger<FeatureHubRepository>();
    private readonly Dictionary<string, FeatureStateBaseHolder> _features =
      new Dictionary<string, FeatureStateBaseHolder>();

    private Readyness _readyness = Readyness.NotReady;
    public override event EventHandler<Readyness> ReadynessHandler;
    public override event EventHandler<FeatureHubRepository> NewFeatureHandler;
    private IList<IAnalyticsCollector> _analyticsCollectors = new List<IAnalyticsCollector>();
    private readonly ApplyFeature _applyFeature;
    private bool _serverSideEvaluation;

    public override Readyness Readyness => _readyness;

    public FeatureHubRepository()
    {
      _applyFeature = new ApplyFeature(new PercentageMurmur3Calculator(), new MatcherRegistry());
    }

    public FeatureHubRepository(ApplyFeature applyFeature)
    {
      _applyFeature = applyFeature;
    }

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

    public void Notify(IEnumerable<FeatureState> features)
    {
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

    // Notify
    public bool ServerSideEvaluation
    {
      get => _serverSideEvaluation;
      set => _serverSideEvaluation = value;
    }

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
            Notify(JsonConvert.DeserializeObject<List<FeatureState>>(data));
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

    public void NotReady()
    {
      _readyness = Readyness.NotReady;
      TriggerReadyness();
    }

    private void DeleteFeature(FeatureState fs)
    {
      if (_features.Remove(fs.Key))
      {
        TriggerNewUpdate();
      }
    }

    public override FeatureHubRepository LogAnalyticEvent(string action)
    {
      return LogAnalyticEvent(action, new Dictionary<string, string>());
    }

    public override FeatureHubRepository LogAnalyticEvent(string action, Dictionary<string, string> other)
    {
      // take a snapshot copy
      var featureCopies =
         _features.Values.Where((f) => f.Exists).Select(f => f.Copy()).ToList();

      foreach (var analyticsCollector in _analyticsCollectors)
      {
        try
        {
          analyticsCollector.LogEvent(action, other, featureCopies);
        }
        catch (Exception e)
        {
          log.Error("Failed to log analytic event", e);
        }
      }

      return this;
    }

    public override FeatureHubRepository AddAnalyticCollector(IAnalyticsCollector collector)
    {
      _analyticsCollectors.Add(collector);
      return this;
    }

    // update the feature if its version is greater than the version we currently store
    private bool FeatureUpdate(FeatureState fs)
    {
      var keyExists = _features.ContainsKey(fs.Key);
      FeatureStateBaseHolder holder = keyExists ? _features[fs.Key] : null;
      if (holder?.Key == null)
      {
        holder = new FeatureStateBaseHolder(holder, _applyFeature);
      }
      else if (holder.Version != null)
      {
        if (holder.Version > fs.Version || (
          holder.Version == fs.Version && !FeatureStateBaseHolder.ValueChanged(holder.Value, fs.Value)))
        return false;
      }

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

    public IFeature FeatureState(string key)
    {
      if (!_features.ContainsKey(key))
      {
        _features.Add(key, new FeatureStateBaseHolder(null, _applyFeature));
      }

      return _features[key];
    }

    public override bool Exists(string key)
    {
      if (_features.ContainsKey(key))
      {
        return _features[key].Type != null;
      }

      return false;
    }

    public override IFeature GetFeature(string key)
    {
      return FeatureState(key);
    }

    public bool IsSet(string key)
    {
      return FeatureState(key).IsEnabled;
    }
  }
}
