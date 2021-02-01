using System;
using System.Collections.Generic;
using System.Linq;
using FeatureHubSDK;
using IO.FeatureHub.SSE.Model;
using NUnit.Framework;

namespace FeatureHubTestProject
{
  public class GoogleAnalyticsTest
  {
    private GoogleAnalyticsCollector _gac;
    private FakeClient _client;

    [SetUp]
    public void Setup()
    {
      _client = new FakeClient();
      _gac = new GoogleAnalyticsCollector("UA-1234", "cid", _client);
    }

    [Test]
    public void WithNoFeaturesBatcherGeneratesNothing()
    {
      _gac.LogEvent("action", null, new List<IFeatureStateHolder>());
      Assert.AreEqual(_client.CalledCount, 0);
    }

    private List<IFeatureStateHolder> Features(FakeFeatureHolder[] items)
    {
      List<IFeatureStateHolder> l = new List<IFeatureStateHolder>();
      foreach (var fakeFeatureHolder in items)
      {
        l.Add(fakeFeatureHolder);
      }

      return l;
    }

    [Test]
    public void WithFeaturesGeneratesCorrectBatchString()
    {
      _gac.LogEvent("action", null, Features(new[]
      {
        new FakeFeatureHolder("bool", FeatureValueType.BOOLEAN, true),
        new FakeFeatureHolder("string", FeatureValueType.STRING, "string_val"),
        new FakeFeatureHolder("num", FeatureValueType.NUMBER, 17.3),
        new FakeFeatureHolder("json", FeatureValueType.JSON, "{}"),
      }));
      Assert.AreEqual(_client.CalledCount, 1);
      Assert.AreEqual(_client.Data,
"v=1&tid=UA-1234&cid=cid&t=event&ec=FeatureHub%20Event&ea=action&el=bool+%3a+on\nv=1&tid=UA-1234&cid=cid&t=event&ec=FeatureHub%20Event&ea=action&el=string+%3a+string_val\nv=1&tid=UA-1234&cid=cid&t=event&ec=FeatureHub%20Event&ea=action&el=num+%3a+17.3\n");
    }

    [Test]
    public void WithFeaturesWithNullValuesBatchIsOk()
    {
      _gac.LogEvent("action", null, Features(new[]
      {
        new FakeFeatureHolder("bool", FeatureValueType.BOOLEAN, false),
        new FakeFeatureHolder("string", FeatureValueType.STRING, null),
        new FakeFeatureHolder("num", FeatureValueType.NUMBER, null),
        new FakeFeatureHolder("json", FeatureValueType.JSON, "{}"),
      }));
      Assert.AreEqual(_client.CalledCount, 1);
      Assert.AreEqual(_client.Data,
        "v=1&tid=UA-1234&cid=cid&t=event&ec=FeatureHub%20Event&ea=action&el=bool+%3a+off\n");
    }
  }

  internal class FakeFeatureHolder : IFeatureStateHolder
  {
    private object _data;
    private FeatureValueType _type;
    private string _key;

    public FakeFeatureHolder(string key, FeatureValueType type, object data)
    {
      _data = data;
      _key = key;
      _type = type;
    }

    public bool Exists
    {
      get => _data != null;
    }
    public bool? BooleanValue
    {
      get => ((bool)_data);
    }
    public string StringValue
    {
      get => _data.ToString();
    }
    public double? NumberValue
    {
      get => (double) _data;
    }
    public string JsonValue
    {
      get => _data.ToString();
    }
    public string Key
    {
      get => _key;
    }
    public FeatureValueType? Type
    {
      get => _type;
    }
    public object Value
    {
      get => _data;
    }
    public long? Version
    {
      get => 1;
    }
    public bool IsSet
    {
      get => _data != null;
    }

    public IFeatureStateHolder WithContext(IClientContext context)
    {
      throw new NotImplementedException();
    }

    public event EventHandler<IFeatureStateHolder> FeatureUpdateHandler;
  }

  internal class FakeClient : IGoogleAnalyticsClient
  {
    public string Data;
    public int CalledCount = 0;

    public void PostBatchUpdate(string batchData)
    {
      Data = batchData;
      CalledCount++;
    }
  }
}
