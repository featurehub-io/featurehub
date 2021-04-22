using System;
using System.Collections.Generic;
using NUnit.Framework;
using FeatureHubSDK;
using IO.FeatureHub.SSE.Model;
using Newtonsoft.Json;

namespace FeatureHubTestProject
{
  public class RepositoryTest
  {
    FeatureHubRepository _repository;

    [SetUp]
    public void Setup()
    {
      _repository = new FeatureHubRepository();
    }

    public static string EncodeFeatures(object value, int version = 1, FeatureValueType type = FeatureValueType.BOOLEAN)
    {
      var feature = new FeatureState(id: "1", key: "1", version: version, value: value, type: type);
      return JsonConvert.SerializeObject(new List<FeatureState>(new FeatureState[] {feature}));
    }

    public static string EncodeFeatures(int version = 1, FeatureValueType type = FeatureValueType.BOOLEAN)
    {
      return EncodeFeatures(false, version, type);
    }

    [Test]
    public void ABooleanIsStoredCorrectly()
    {
      _repository.Notify(SSEResultState.Features, EncodeFeatures(true, 1, FeatureValueType.BOOLEAN));
      Assert.AreEqual(true, _repository.GetFeature("1").BooleanValue);
    }

    [Test]
    public void ANumberIsStoredCorrectly()
    {
      _repository.Notify(SSEResultState.Features, EncodeFeatures(16.3, 1, FeatureValueType.NUMBER));
      Assert.AreEqual(16.3, _repository.GetFeature("1").NumberValue);
      Assert.AreEqual(false, _repository.IsEnabled("1"));
      Assert.AreEqual(true, _repository.IsSet("1"));
      Assert.AreEqual(false, _repository.GetFeature("1").IsEnabled);
    }

    [Test]
    public void AStringIsStoredCorrectly()
    {
      _repository.Notify(SSEResultState.Features, EncodeFeatures("some duck", 1, FeatureValueType.STRING));
      Assert.AreEqual("some duck", _repository.GetFeature("1").StringValue);
      Assert.IsNull(_repository.GetFeature("1").NumberValue);
      Assert.IsNull(_repository.GetFeature("1").JsonValue);
      Assert.IsNull(_repository.GetFeature("1").BooleanValue);
      Assert.AreEqual(false, _repository.IsEnabled("1"));
      Assert.AreEqual(false, _repository.GetFeature("1").IsEnabled);
    }

    [Test]
    public void JsonIsStoredCorrectly()
    {
      _repository.Notify(SSEResultState.Features, EncodeFeatures("{}", 1, FeatureValueType.JSON));
      Assert.AreEqual("{}", _repository.GetFeature("1").JsonValue);
    }

    [Test]
    public void ReadynessWhenFeaturesAppear()
    {
      var found = false;
      _repository.ReadynessHandler += (sender, readyness) => { found = true; };
      _repository.Notify(SSEResultState.Features, EncodeFeatures());
      Assert.AreEqual(true, found);
    }

    [Test]
    public void ExplodeWhenReadynessDoesntFailTest()
    {
      var found = false;
      _repository.ReadynessHandler += (sender, readyness) => throw new Exception();
      _repository.ReadynessHandler += (sender, readyness) => { found = true; };
      _repository.Notify(SSEResultState.Features, EncodeFeatures());
      Assert.AreEqual(false, found);
    }

    [Test]
    public void ExplodeWhenNewFeatureDoesntFailTest()
    {
      var found = false;
      _repository.NewFeatureHandler += (sender, readyness) => throw new Exception();
      _repository.NewFeatureHandler += (sender, readyness) => { found = true; };
      _repository.Notify(SSEResultState.Features, EncodeFeatures());
      Assert.AreEqual(false, found);
    }

    [Test]
    public void ExplodeWhenFeatureUpdatesDoesNotFailTest()
    {
      var found = false;
      _repository.FeatureState("1").FeatureUpdateHandler += (sender, state) => throw new Exception();
      _repository.FeatureState("1").FeatureUpdateHandler += (sender, state) => { found = true; };
      Assert.AreEqual(false, found);
      _repository.Notify(SSEResultState.Features, EncodeFeatures());
    }

    [Test]
    public void ByeTurnsOffReadyness()
    {
      _repository.Notify(SSEResultState.Features, EncodeFeatures());
      Assert.AreEqual(Readyness.Ready, _repository.Readyness);
      _repository.Notify(SSEResultState.Bye, null);
      Assert.AreEqual(Readyness.NotReady, _repository.Readyness);
    }

    [Test]
    public void WhenTheStreamHasFailedReadynessShouldFail()
    {
      var state = Readyness.Ready;
      _repository.ReadynessHandler += (sender, readyness) => { state = readyness; };
      _repository.Notify(SSEResultState.Failure, null);
      Assert.AreEqual(Readyness.Failed, state);
    }

    [Test]
    public void SendingNewVersionsOfFeaturesWillTriggerThewNewFeaturesHook()
    {
      var found = false;
      _repository.NewFeatureHandler += (sender, repository) => { found = true; };
      _repository.Notify(SSEResultState.Features, EncodeFeatures());
      _repository.Notify(SSEResultState.Features, EncodeFeatures(version: 2));
      Assert.AreEqual(true, found);
    }

    [Test]
    public void SendingTheSameVersionsWillNotTriggerNewFeaturesHook()
    {
      var nfCount = 0;
      _repository.NewFeatureHandler += (sender, repository) => { nfCount++; };
      var features = EncodeFeatures();
      _repository.Notify(SSEResultState.Features, features);
      _repository.Notify(SSEResultState.Features, features);
      _repository.Notify(SSEResultState.Features, features);
      Assert.AreEqual(1, nfCount);
    }

    [Test]
    public void ListeningForAFeatureThatDoesntExistAndThenTriggeringItTriggersHandler()
    {
      IFeature holder = null;
      var hCount = 0;
      _repository.FeatureState("1").FeatureUpdateHandler += (sender, fs) =>
      {
        holder = fs;
        hCount++;
      };
      _repository.Notify(SSEResultState.Features, EncodeFeatures());
      Assert.AreEqual(1, hCount);
      Assert.IsNotNull(holder);
      Assert.AreEqual("1", holder.Key);
      Assert.AreEqual(true, holder.Exists);
      Assert.AreEqual(1, holder.Version);
      Assert.AreEqual(false, holder.BooleanValue);
      Assert.AreEqual(false, holder.IsEnabled);
    }

    [Test]
    public void ListeningForAStringValueWorksAsExpected()
    {
      IFeature holder = null;
      _repository.FeatureState("1").FeatureUpdateHandler += (sender, fs) => { holder = fs; };
      _repository.Notify(SSEResultState.Features, EncodeFeatures("fred", version: 2, type: FeatureValueType.STRING));
      Assert.AreEqual(FeatureValueType.STRING, holder.Type);
      Assert.AreEqual("fred", holder.StringValue);
      Assert.AreEqual("fred", _repository.FeatureState("1").Value);
    }

    [Test]
    public void ListeningForNumberValueWorksAsExpected()
    {
      IFeature holder = null;
      _repository.FeatureState("1").FeatureUpdateHandler += (sender, fs) => { holder = fs; };
      _repository.Notify(SSEResultState.Features, EncodeFeatures(78.3, version: 2, type: FeatureValueType.NUMBER));
      Assert.AreEqual(FeatureValueType.NUMBER, holder.Type);
      Assert.AreEqual(78.3, holder.NumberValue);
    }

    [Test]
    public void ListeningForAJsonValueWorksAsExpected()
    {
      IFeature holder = null;
      _repository.FeatureState("1").FeatureUpdateHandler += (sender, fs) => { holder = fs; };
      _repository.Notify(SSEResultState.Features, EncodeFeatures("fred", version: 2, type: FeatureValueType.JSON));
      Assert.AreEqual(FeatureValueType.JSON, holder.Type);
      Assert.AreEqual("fred", holder.JsonValue);
      Assert.IsNull(holder.StringValue);
      Assert.IsNull(holder.BooleanValue);
      Assert.IsNull(holder.NumberValue);
    }

    [Test]
    public void ChangingFeatureValueFromOriginalTriggersEventHandler()
    {
      IFeature holder = null;
      var hCount = 0;
      _repository.FeatureState("1").FeatureUpdateHandler += (sender, fs) =>
      {
        holder = fs;
        Console.WriteLine($"{fs}");
        hCount++;
      };
      _repository.Notify(SSEResultState.Features, EncodeFeatures());
      _repository.Notify(SSEResultState.Features, EncodeFeatures()); // same again
      _repository.Notify(SSEResultState.Features, EncodeFeatures(version: 2)); // same again, new version but same value
      _repository.Notify(SSEResultState.Features, EncodeFeatures(true, version: 3));
      Assert.AreEqual(2, hCount);
      Assert.IsNotNull(holder);
      Assert.AreEqual(true, holder.BooleanValue);
      var feature = new FeatureState(id: "1", key: "1", version: 4, value: false, type: FeatureValueType.BOOLEAN);
      _repository.Notify(SSEResultState.Feature, JsonConvert.SerializeObject(feature));
      Assert.AreEqual(3, hCount);
      Assert.AreEqual(false, holder.BooleanValue);
    }

    [Test]
    public void ChangingFeatureValueWithSameVersionButDifferentValueTriggersEventHandler()
    {
      IFeature holder = null;
      var hCount = 0;
      _repository.FeatureState("1").FeatureUpdateHandler += (sender, fs) =>
      {
        holder = fs;
        Console.WriteLine($"{fs}");
        hCount++;
      };
      _repository.Notify(SSEResultState.Features, EncodeFeatures()); // false, 1, boolean
      _repository.Notify(SSEResultState.Features, EncodeFeatures(true, 1, FeatureValueType.BOOLEAN));

      Assert.AreEqual(2, hCount);
      Assert.IsNotNull(holder);
      Assert.AreEqual(true, holder.BooleanValue);
    }

    [Test]
    public void ANumberCanBeAnInteger()
    {
      _repository.Notify(SSEResultState.Features, EncodeFeatures(1L, version: 1, type: FeatureValueType.NUMBER));
      Assert.AreEqual(1, _repository.FeatureState("1").NumberValue);
      Assert.AreEqual(1, _repository.GetFeature("1").NumberValue);
    }

    [Test]
    public void DeleteRemovesFeature()
    {
      _repository.Notify(SSEResultState.Features, EncodeFeatures());
      var feature = new FeatureState(id: "1", key: "1", version: 2, value: true, type: FeatureValueType.BOOLEAN);
      Assert.AreEqual(1, _repository.FeatureState("1").Version);
      _repository.Notify(SSEResultState.Deletefeature, JsonConvert.SerializeObject(feature));
      Assert.IsNull(_repository.FeatureState("1").Version);
    }



    [Test]
    public void AnalyticsCollectorsAreCalled()
    {
      TestAnalyticsCollector ac1 = new TestAnalyticsCollector();
      TestAnalyticsCollector ac2 = new TestAnalyticsCollector();

      _repository.AddAnalyticCollector(ac1).AddAnalyticCollector(ac2);
      _repository.LogAnalyticEvent("action");

      Assert.AreEqual(ac1.Counter, 1);
      Assert.AreEqual(ac2.Counter, 1);

      _repository.LogAnalyticEvent("next-action", new Dictionary<string, string>());

      Assert.AreEqual(ac1.Counter, 2);
      Assert.AreEqual(ac2.Counter, 2);
    }
  }

  internal class TestAnalyticsCollector : IAnalyticsCollector
  {
    public int Counter = 0;

    public void LogEvent(string action, Dictionary<string, string> other, List<IFeature> featureStates)
    {
      Counter++;
    }
  }
}
