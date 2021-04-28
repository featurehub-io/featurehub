using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Threading.Tasks;
using System.Web;
using Common.Logging;
using IO.FeatureHub.SSE.Model;

namespace FeatureHubSDK
{

  public interface IClientContext
  {
    IClientContext UserKey(string key);
    IClientContext SessionKey(string key);
    IClientContext Device(StrategyAttributeDeviceName device);
    IClientContext Platform(StrategyAttributePlatformName platform);
    IClientContext Country(StrategyAttributeCountryName country);
    /// expects semantic version Maj.Minor.Patch
    IClientContext Version(string version);
    IClientContext Attr(string key, string value);
    IClientContext Attrs(string key, List<string> values);
    IClientContext Clear();

    string GetAttr(string key, string defaultValue);

    string DefaultPercentageKey { get; }

    IFeature this[string name] { get; }

    /// <summary>
    /// Is this feature enabled - it has to be a boolean and true. The same as context[name].IsEnabled
    /// </summary>
    /// <param name="name">the name of the feature.</param>
    /// <returns></returns>
    bool IsEnabled(string name);

    /// <summary>
    /// Does this feature have a value? The same as context[name].IsSet
    /// </summary>
    /// <param name="name">feature name</param>
    /// <returns></returns>
    bool IsSet(string name);

    Task<IClientContext> Build();

    IEdgeService EdgeService { get; }
    IFeatureHubRepository Repository { get; }

    IFeatureHubRepository LogAnalyticEvent(string action, string user = null, Dictionary<string, string> other = null);

    void Close();
  }

    public abstract class BaseClientContext : IClientContext
  {
    private static readonly ILog Log = LogManager.GetLogger<BaseClientContext>();
    protected readonly Dictionary<string, List<string>> _attributes = new Dictionary<string,List<string>>();
    protected readonly IFeatureRepositoryContext _repository;
    protected readonly IFeatureHubConfig _config;

    public IFeatureHubRepository Repository => _repository;

    public IFeatureHubRepository LogAnalyticEvent(string action, string user = null, Dictionary<string, string> other = null)
    {
      if (user == null)
      {
        user = GetAttr("userkey", null);
      }

      if (user != null)
      {
        if (other == null)
        {
          other = new Dictionary<string, string>();
        }

        other[GoogleConstants.Cid] = user;
      }

      _repository.LogAnalyticEvent(action, other);

      return _repository;
    }

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
      return this[name].IsEnabled;
    }

    public bool IsSet(string name)
    {
      return this[name].IsSet;
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

  public class ServerEvalFeatureContext : BaseClientContext
  {
    private readonly EdgeServiceSource _edgeServiceSource;
    private IEdgeService _currentEdgeService;
    private string _xHeader;
    private readonly bool _weCreatedSources;

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
          _currentEdgeService = null;
        }
      }

      if (_currentEdgeService == null)
      {
        _currentEdgeService = _edgeServiceSource(_repository, _config);
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

    public ClientEvalFeatureContext(IFeatureRepositoryContext repository, IFeatureHubConfig config,
      EdgeServiceSource edgeServiceSource) : base(repository, config)
    {
      _edgeService = edgeServiceSource(repository, config);
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
}
