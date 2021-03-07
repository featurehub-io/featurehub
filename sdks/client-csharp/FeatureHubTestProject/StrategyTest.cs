using System;
using System.Collections.Generic;
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

      var matchCC = new TestClientContext().Country(StrategyAttributeCountryName.Turkey);
      var unmatchCC = new TestClientContext().Country(StrategyAttributeCountryName.Newzealand);

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

      var age27 = new TestClientContext().Attr("age", "27");
      var age18 = new TestClientContext().Attr("age", "18");
      var age43 = new TestClientContext().Attr("age", "43");

      // then
      Assert.AreEqual(10, repo.GetFeature("num1").WithContext(age27).NumberValue);
      Assert.AreEqual(16, repo.GetFeature("num1").WithContext(age18).NumberValue);
      Assert.AreEqual(6, repo.GetFeature("num1").WithContext(age43).NumberValue);
    }

    private void StringTypeComparison(FeatureValueType ft)
    {
      // given: we have a basic string feature with two custom strategies based on age and platform
      var feature = new FeatureState();
      feature.Key = "s1";
      feature.Value = "feature";
      feature.Version = 1;
      feature.Type = ft;
      var notMobileStrategy = new RolloutStrategy("id", "not-mobile");
      notMobileStrategy.Value = "not-mobile";
      var ruleset1 = new RolloutStrategyAttribute();
      ruleset1.Conditional = RolloutStrategyAttributeConditional.EXCLUDES;
      ruleset1.Type = RolloutStrategyFieldType.STRING;
      ruleset1.FieldName = GetEnumMemberValue(StrategyAttributeWellKnownNames.Platform);
      ruleset1.Values = new List<object> {GetEnumMemberValue(StrategyAttributePlatformName.Android), GetEnumMemberValue(StrategyAttributePlatformName.Ios)};

      notMobileStrategy.Attributes = new List<RolloutStrategyAttribute> {ruleset1};

      var over20Strategy = new RolloutStrategy("id", "older-than-twenty");
      over20Strategy.Value = "older-than-twenty";
      var ruleset2 = new RolloutStrategyAttribute();
      ruleset2.Conditional = RolloutStrategyAttributeConditional.GREATEREQUALS;
      ruleset2.Type = RolloutStrategyFieldType.NUMBER;
      ruleset2.FieldName = "age";
      ruleset2.Values = new List<object> {20};
      over20Strategy.Attributes = new List<RolloutStrategyAttribute> {ruleset2};

      feature.Strategies = new List<RolloutStrategy> {notMobileStrategy, over20Strategy};

      // when: setup repo
      repo.Notify(new List<FeatureState>{feature});

      var ccAge27Ios = new TestClientContext().Platform(StrategyAttributePlatformName.Ios).Attr("age", "27");
      var ccAge18Android = new TestClientContext().Platform(StrategyAttributePlatformName.Android).Attr("age", "18");
      var ccAge43MacOS = new TestClientContext().Platform(StrategyAttributePlatformName.Macos).Attr("age", "43");
      var ccAge18MacOS = new TestClientContext().Platform(StrategyAttributePlatformName.Macos).Attr("age", "18");
      var ccEmpty = new TestClientContext();

      switch (ft)
      {
        case FeatureValueType.STRING:
          // then
          Assert.AreEqual("feature", repo.GetFeature("s1").StringValue);
          Assert.AreEqual("feature", repo.GetFeature("s1").WithContext(ccEmpty).StringValue);
          Assert.AreEqual("feature", repo.GetFeature("s1").WithContext(ccAge18Android).StringValue);
          Assert.AreEqual("not-mobile", repo.GetFeature("s1").WithContext(ccAge18MacOS).StringValue);
          Assert.AreEqual("older-than-twenty", repo.GetFeature("s1").WithContext(ccAge27Ios).StringValue);
          Assert.AreEqual("not-mobile", repo.GetFeature("s1").WithContext(ccAge43MacOS).StringValue);
          break;
        case FeatureValueType.JSON:
          Assert.AreEqual("feature", repo.GetFeature("s1").JsonValue);
          Assert.AreEqual("feature", repo.GetFeature("s1").WithContext(ccEmpty).JsonValue);
          Assert.AreEqual("feature", repo.GetFeature("s1").WithContext(ccAge18Android).JsonValue);
          Assert.AreEqual("not-mobile", repo.GetFeature("s1").WithContext(ccAge18MacOS).JsonValue);
          Assert.AreEqual("older-than-twenty", repo.GetFeature("s1").WithContext(ccAge27Ios).JsonValue);
          Assert.AreEqual("not-mobile", repo.GetFeature("s1").WithContext(ccAge43MacOS).JsonValue);
          break;
      }
    }

    [Test]
    public void BasicStringStrategy()
    {
      StringTypeComparison(FeatureValueType.STRING);
    }

    [Test]
    public void BasicJson()
    {
      StringTypeComparison(FeatureValueType.JSON);
    }
  }
}
