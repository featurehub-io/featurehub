using System;
using System.Collections.Generic;
using NUnit.Framework;
using FeatureHubSDK;
using IO.FeatureHub.SSE.Model;
using Newtonsoft.Json;

namespace FeatureHubTestProject
{
  public class Tests
  {
     FeatureHubRepository _repository;

    [SetUp]
    public void Setup()
    {
      _repository = new FeatureHubRepository();
    }

    private string EncodeFeatures(object value, int version = 1, FeatureValueType type = FeatureValueType.BOOLEAN)
    {
      var feature = new FeatureState(id: "1", key: "1", version: version, value: value, type: type);
      return JsonConvert.SerializeObject(new List<FeatureState>(new FeatureState[]{feature}));
    }

    private string EncodeFeatures(int version = 1, FeatureValueType type = FeatureValueType.BOOLEAN)
    {
      return EncodeFeatures(false, version, type);
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
      _repository.NewFeatureHandler += (sender, repository) => { nfCount ++; };
      var features = EncodeFeatures();
      _repository.Notify(SSEResultState.Features, features);
      _repository.Notify(SSEResultState.Features, features);
      _repository.Notify(SSEResultState.Features, features);
      Assert.AreEqual(1, nfCount);
    }

    [Test]
    public void ListeningForAFeatureThatDoesntExistAndThenTriggeringItTriggersHandler()
    {
      IFeatureStateHolder holder = null;
      var hCount = 0;
      _repository.FeatureState("1").FeatureUpdateHandler += (sender, fs) => {
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
    }

    [Test]
    public void ListeningForAStringValueWorksAsExpected()
    {
      IFeatureStateHolder holder = null;
      _repository.FeatureState("1").FeatureUpdateHandler += (sender, fs) => {
        holder = fs;
      };
      _repository.Notify(SSEResultState.Features, EncodeFeatures("fred", version: 2, type: FeatureValueType.STRING));
      Assert.AreEqual(FeatureValueType.STRING, holder.Type);
      Assert.AreEqual("fred", holder.StringValue );
    }

    [Test]
    public void ListeningForNumberValueWorksAsExpected()
    {
      IFeatureStateHolder holder = null;
      _repository.FeatureState("1").FeatureUpdateHandler += (sender, fs) => {
        holder = fs;
      };
      _repository.Notify(SSEResultState.Features, EncodeFeatures(78.3, version: 2, type: FeatureValueType.NUMBER));
      Assert.AreEqual(FeatureValueType.NUMBER, holder.Type);
      Assert.AreEqual(78.3, holder.NumberValue );
    }


  }
}
