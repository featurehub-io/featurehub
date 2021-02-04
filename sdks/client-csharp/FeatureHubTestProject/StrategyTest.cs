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
      var attr = new RolloutStrategyAttribute();
      attr.Conditional = RolloutStrategyAttributeConditional.EQUALS;
      attr.Type = RolloutStrategyFieldType.STRING;
      attr.FieldName = GetEnumMemberValue(StrategyAttributeWellKnownNames.Country);
      attr.Values = new List<object> {GetEnumMemberValue(StrategyAttributeCountryName.Turkey)};

      strategy.Attributes = new List<RolloutStrategyAttribute> {attr};
      feature.Strategies = new List<RolloutStrategy> {strategy};

      repo.Notify(new List<FeatureState>{feature});

      var matchCC = new IndividualClientContext().Country(StrategyAttributeCountryName.Turkey);
      var unmatchCC = new IndividualClientContext().Country(StrategyAttributeCountryName.Newzealand);

      Assert.AreEqual(false, repo.GetFeature("bool1").WithContext(matchCC).BooleanValue);
      Assert.AreEqual(true, repo.GetFeature("bool1").WithContext(unmatchCC).BooleanValue);
      Assert.AreEqual(true, repo.GetFeature("bool1").BooleanValue);
    }
  }
}
