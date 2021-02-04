using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using FeatureHubSDK;
using IO.FeatureHub.SSE.Model;
using NUnit.Framework;

namespace FeatureHubTestProject
{
  class StrategyTest
  {
    private FeatureHubRepository repo;

    [SetUp]
    public void Setup()
    {
      repo = new FeatureHubRepository();
    }

    private static string GetEnumMemberValue(Enum enumValue)
    {
      var type = enumValue.GetType();
      var info = type.GetField(enumValue.ToString());
      var da = (EnumMemberAttribute[])(info.GetCustomAttributes(typeof(EnumMemberAttribute), false));

      return da.Length > 0 ? da[0].Value : string.Empty;
    }


    [Test]
    public void BasicBooleanStrategy()
    {
      // given: we have a basic boolean feature
      var feature = new FeatureState();
      feature.Key = "bool1";
      feature.Value = true;
      feature.Version = 1;
      feature.Type = FeatureValueType.BOOLEAN;
      var strategy = new RolloutStrategy("id", "name");
      strategy.Value = false;
      var ruleset = new RolloutStrategyAttribute();
      ruleset.Conditional = RolloutStrategyAttributeConditional.EQUALS;
      ruleset.Type = RolloutStrategyFieldType.STRING;
      ruleset.FieldName = GetEnumMemberValue(StrategyAttributeWellKnownNames.Country);
      ruleset.Values = new List<object> {GetEnumMemberValue(StrategyAttributeCountryName.Turkey)};

      strategy.Attributes = new List<RolloutStrategyAttribute> {ruleset};
      feature.Strategies = new List<RolloutStrategy> {strategy};

      repo.Notify(new List<FeatureState>{feature});

      var matchCC = new IndividualClientContext().Country(StrategyAttributeCountryName.Turkey);
      var unmatchCC = new IndividualClientContext().Country(StrategyAttributeCountryName.Newzealand);

      Assert.AreEqual(false, repo.GetFeature("bool1").WithContext(matchCC).BooleanValue);
      Assert.AreEqual(true, repo.GetFeature("bool1").WithContext(unmatchCC).BooleanValue);
      Assert.AreEqual(true, repo.GetFeature("bool1").BooleanValue);
    }

    [Test]
    public void BasicNumberStrategy()
    {
      // given: we have a basic number feature with two custom strategies based on age
      var feature = new FeatureState();
      feature.Key = "num1";
      feature.Value = 16;
      feature.Version = 1;
      feature.Type = FeatureValueType.NUMBER;
      var over40Strategy = new RolloutStrategy("id", "name");
      over40Strategy.Value = 6;
      var ruleset1 = new RolloutStrategyAttribute();
      ruleset1.Conditional = RolloutStrategyAttributeConditional.GREATEREQUALS;
      ruleset1.Type = RolloutStrategyFieldType.NUMBER;
      ruleset1.FieldName = "age";
      ruleset1.Values = new List<object> {40};

      over40Strategy.Attributes = new List<RolloutStrategyAttribute> {ruleset1};

      var over20Strategy = new RolloutStrategy("id", "name");
      over20Strategy.Value = 10;
      var ruleset2 = new RolloutStrategyAttribute();
      ruleset2.Conditional = RolloutStrategyAttributeConditional.GREATEREQUALS;
      ruleset2.Type = RolloutStrategyFieldType.NUMBER;
      ruleset2.FieldName = "age";
      ruleset2.Values = new List<object> {20};
      over20Strategy.Attributes = new List<RolloutStrategyAttribute> {ruleset2};

      feature.Strategies = new List<RolloutStrategy> {over40Strategy, over20Strategy};

      // when: setup repo
      repo.Notify(new List<FeatureState>{feature});

      var age27 = new IndividualClientContext().Attr("age", "27");
      var age18 = new IndividualClientContext().Attr("age", "18");
      var age43 = new IndividualClientContext().Attr("age", "43");

      // then
      Assert.AreEqual(10, repo.GetFeature("num1").WithContext(age27).NumberValue);
      Assert.AreEqual(16, repo.GetFeature("num1").WithContext(age18).NumberValue);
      Assert.AreEqual(6, repo.GetFeature("num1").WithContext(age43).NumberValue);
    }
  }
}
