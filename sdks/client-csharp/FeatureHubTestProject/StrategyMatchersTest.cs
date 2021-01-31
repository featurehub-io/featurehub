
using System;
using System.Collections.Generic;
using System.Linq;
using IO.FeatureHub.SSE.Model;
using Microsoft.VisualStudio.TestPlatform.ObjectModel;
using NUnit.Framework;

namespace FeatureHubTestProject
{
  class StrategyMatcherTest
  {
    private MatcherRegistry registry;

    [SetUp]
    public void Setup()
    {
      registry = new MatcherRegistry();
    }

    [Test, TestCaseSource("StringMatcherProvider")]
    public void StringMatcher(RolloutStrategyAttributeConditional conditional, List<string> vals, string suppliedVal,
      bool matches)
    {
      var rsa = new RolloutStrategyAttribute();
      rsa.Conditional = conditional;
      rsa.Type = RolloutStrategyFieldType.STRING;
      rsa.Values = vals.Select(v => v as object).ToList();

      Assert.AreEqual(registry.FindMatcher(rsa).Match(suppliedVal, rsa), matches);
    }

    public static IEnumerable<TestCaseData> StringMatcherProvider()
    {
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<string> {"a", "b"}, "a", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<string> {"a", "b"}, "a",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.INCLUDES, new List<string> {"a", "b"}, "a",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.INCLUDES, new List<string> {"a", "b"}, "c",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EXCLUDES, new List<string> {"a", "b"}, "a",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EXCLUDES, new List<string> {"a", "b"}, "c",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER, new List<string> {"a", "b"}, "a",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATEREQUALS, new List<string> {"a", "b"}, "a",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<string> {"a", "b"}, "a", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<string> {"a", "b"}, "b", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<string> {"a", "b"}, "c", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<string> {"a", "b"}, "a",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<string> {"a", "b"}, "b",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<string> {"a", "b"}, "1",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.REGEX, new List<string> {"(.*)gold(.*)"},
        "actapus (gold)", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.REGEX, new List<string> {"(.*)gold(.*)"},
        "(.*)purple(.*)", false);
    }

    [Test, TestCaseSource("BooleanMatcherProvider")]
    public void BooleanMatcher(RolloutStrategyAttributeConditional conditional, List<object> vals, string suppliedVal,
      bool matches)
    {
      var rsa = new RolloutStrategyAttribute();
      rsa.Conditional = conditional;
      rsa.Type = RolloutStrategyFieldType.BOOLEAN;
      rsa.Values = vals.Select(v => v as object).ToList();

      Assert.AreEqual(registry.FindMatcher(rsa).Match(suppliedVal, rsa), matches);
    }

    public static IEnumerable<TestCaseData> BooleanMatcherProvider()
    {
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{"true"}, "true", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object>{"true"}, "true", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object>{"true"}, "false", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{true}, "true", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{"true"}, "false", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{true}, "false", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{"false"}, "false", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{false}, "false", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{"false"}, "true", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{false}, "true", false);
    }

    [Test, TestCaseSource("SemanticVersionMatcherProvider")]
    public void SemanticVersionMatcher(RolloutStrategyAttributeConditional conditional, List<object> vals, string suppliedVal,
      bool matches)
    {
      var rsa = new RolloutStrategyAttribute();
      rsa.Conditional = conditional;
      rsa.Type = RolloutStrategyFieldType.SEMANTICVERSION;
      rsa.Values = vals.Select(v => v as object).ToList();

      Assert.AreEqual(registry.FindMatcher(rsa).Match(suppliedVal, rsa), matches);
    }

    public static IEnumerable<TestCaseData> SemanticVersionMatcherProvider()
    {
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{"2.0.3"}, "2.0.3", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{"2.0.3", "2.0.1"}, "2.0.3", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{"2.0.3"}, "2.0.1", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object>{"2.0.3"}, "2.0.3", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object>{"2.0.3"}, "2.0.1", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER, new List<object>{"2.0.0"}, "2.1.0", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER, new List<object>{"2.0.0"}, "2.0.1", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATEREQUALS, new List<object>{"7.1.0"}, "7.1.6", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATEREQUALS, new List<object>{"7.1.6"}, "7.1.6", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER, new List<object>{"8.1.0"}, "7.1.6", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<object>{"8.1.0"}, "7.1.6", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<object>{"6.1.0"}, "7.1.6", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<object>{"7.1.6"}, "7.1.6", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object>{"7.1.6"}, "7.1.6", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object>{"7.1.6"}, "6.1.6", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object>{"7.1.6"}, "8.1.6", false);
    }


  }


}
