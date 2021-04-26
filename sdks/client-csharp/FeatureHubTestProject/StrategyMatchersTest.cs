using System.Collections.Generic;
using System.Linq;
using FeatureHubSDK;
using IO.FeatureHub.SSE.Model;
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

    // need to include mocking library
    // public void MatcherTest()
    // {
    //   var rsa = new RolloutStrategyAttribute();
    //   rsa.Conditional = RolloutStrategyAttributeConditional.LESS;
    //   rsa.Type = RolloutStrategyFieldType.STRING;
    //   rsa.Values = new List<object> {"a", "b"};
    //
    //
    //
    // }

    [Test, TestCaseSource("StringMatcherProvider")]
    public void StringMatcher(RolloutStrategyAttributeConditional conditional, List<object> vals, string suppliedVal,
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
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"a", "b"}, null, false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"a", "b"}, "a", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object> {"a", "b"}, "a",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.INCLUDES, new List<object> {"a", "b"}, "a",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.INCLUDES, new List<object> {"a", "b"}, "c",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EXCLUDES, new List<object> {"a", "b"}, "a",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EXCLUDES, new List<object> {"a", "b"}, "c",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER, new List<object> {"a", "b"}, "a",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER, new List<object> {"a", "b"}, "c",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATEREQUALS, new List<object> {"a", "b"}, "a",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<object> {"a", "b"}, "a", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<object> {"a", "b"}, "b", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<object> {"a", "b"}, "c", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object> {"a", "b"}, "a",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object> {"a", "b"}, "b",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object> {"a", "b"}, "1",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object> {"a", "b"}, "c",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.STARTSWITH, new List<object> {"fr"}, "fred",
      true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.STARTSWITH, new List<object> {"fred"}, "mar",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.ENDSWITH, new List<object> {"ed"}, "fred",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.ENDSWITH, new List<object> {"fred"}, "mar",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.REGEX, new List<object> {"(.*)gold(.*)"},
        "actapus (gold)", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.REGEX, new List<object> {"(.*)gold(.*)"},
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

      Assert.AreEqual(registry.FindMatcher(rsa).Match(suppliedVal?.ToString(), rsa), matches);
    }

    public static IEnumerable<TestCaseData> BooleanMatcherProvider()
    {
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"true"}, "true",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object> {"true"}, "true",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object> {"true"}, "false",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {true}, "true", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"true"}, "false",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {true}, "false",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"false"}, "false",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {false}, "false",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"false"}, "true",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {false}, "true",
        false);
    }

    [Test, TestCaseSource("SemanticVersionMatcherProvider")]
    public void SemanticVersionMatcher(RolloutStrategyAttributeConditional conditional, List<object> vals,
      string suppliedVal,
      bool matches)
    {
      var rsa = new RolloutStrategyAttribute();
      rsa.Conditional = conditional;
      rsa.Values = vals.Select(v => v as object).ToList();
      rsa.Type = RolloutStrategyFieldType.SEMANTICVERSION;

      Assert.AreEqual(registry.FindMatcher(rsa).Match(suppliedVal, rsa), matches);
    }

    public static IEnumerable<TestCaseData> SemanticVersionMatcherProvider()
    {
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"2.0.3"}, "2.0.3",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"2.0.3", "2.0.1"},
        "2.0.3", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"2.0.3"}, "2.0.1",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object> {"2.0.3"}, "2.0.3",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object> {"2.0.3"}, "2.0.1",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER, new List<object> {"2.0.0"}, "2.1.0",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER, new List<object> {"2.0.0"}, "2.0.1",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATEREQUALS, new List<object> {"7.1.0"},
        "7.1.6", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATEREQUALS, new List<object> {"7.1.6"},
        "7.1.6", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER, new List<object> {"8.1.0"}, "7.1.6",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<object> {"8.1.0"}, "7.1.6",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<object> {"6.1.0"}, "7.1.6",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<object> {"7.1.6"}, "7.1.6",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object> {"7.1.6"}, "7.1.6",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object> {"7.1.6"}, "6.1.6",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object> {"7.1.6"}, "8.1.6",
        false);
    }


    [Test, TestCaseSource("IPAddressMatcherProvider")]
    public void IPAddressMatcher(RolloutStrategyAttributeConditional conditional, List<object> vals, string suppliedVal,
      bool matches)
    {
      var rsa = new RolloutStrategyAttribute();
      rsa.Conditional = conditional;
      rsa.Values = vals.Select(v => v as object).ToList();
      rsa.Type = RolloutStrategyFieldType.IPADDRESS;

      Assert.AreEqual(matches, registry.FindMatcher(rsa).Match(suppliedVal, rsa));
    }

    public static IEnumerable<TestCaseData> IPAddressMatcherProvider()
    {
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"192.168.86.75"},
        "192.168.86.75", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object> {"192.168.86.75"},
        "192.168.86.75", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"192.168.86.75"},
        "192.168.86.72", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object> {"192.168.86.75"},
        "192.168.86.72", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"192.168.0.0/16"},
        "192.168.86.75", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {"192.168.0.0/16"},
        "192.162.86.75", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS,
        new List<object> {"10.0.0.0/24", "192.168.0.0/16"}, "192.168.86.75", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS,
        new List<object> {"10.0.0.0/24", "192.168.0.0/16"}, "172.168.86.75", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS,
        new List<object> {"10.7.4.8", "192.168.86.75"}, "192.168.86.75", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.INCLUDES,
        new List<object> {"10.7.4.8", "192.168.86.75"}, "192.168.86.75", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS,
        new List<object> {"10.7.4.8", "192.168.86.75"}, "192.168.83.75", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EXCLUDES,
        new List<object> {"10.7.4.8", "192.168.86.75"}, "192.168.83.75", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EXCLUDES,
        new List<object> {"10.7.4.8", "192.168.86.75"}, "192.168.86.75", false);

      // library can't handle padded zeros
      // yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object>{"192.168.86.75"}, "192.168.086.075", true);
    }

    [Test, TestCaseSource("NumberMatcherProvider")]
    public void NumberMatcher(RolloutStrategyAttributeConditional conditional, List<object> vals, string suppliedVal,
      bool matches)
    {
      var rsa = new RolloutStrategyAttribute();
      rsa.Conditional = conditional;
      rsa.Values = vals.Select(v => v as object).ToList();
      rsa.Type = RolloutStrategyFieldType.NUMBER;

      Assert.AreEqual(matches, registry.FindMatcher(rsa).Match(suppliedVal, rsa));
    }

    public static IEnumerable<TestCaseData> NumberMatcherProvider()
    {
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {10, 5}, "5", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {5}, "5", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {4}, "5", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS, new List<object> {4, 7}, "5", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.INCLUDES, new List<object> {4, 7}, "5", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object> {23, 100923}, "5",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EXCLUDES, new List<object> {23, 100923}, "5",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS, new List<object> {5}, "5", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER, new List<object> {2, 4}, "5", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATEREQUALS, new List<object> {2, 5}, "5",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATEREQUALS, new List<object> {6, 5}, "5",
        true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object> {2, 5}, "5", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<object> {8, 7}, "5", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER, new List<object> {7, 10}, "5", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATEREQUALS, new List<object> {6, 7}, "5",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS, new List<object> {2, 3}, "5",
        false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS, new List<object> {1, -1}, "5", false);
    }

    [Test, TestCaseSource("DateMatcherProvider")]
    public void DateMatcher(RolloutStrategyAttributeConditional conditional, List<object> vals, string suppliedVal,
      bool matches)
    {
      var rsa = new RolloutStrategyAttribute();
      rsa.Conditional = conditional;
      rsa.Values = vals.Select(v => v as object).ToList();
      rsa.Type = RolloutStrategyFieldType.DATE;

      Assert.AreEqual(matches, registry.FindMatcher(rsa).Match(suppliedVal, rsa));
    }

    public static IEnumerable<TestCaseData> DateMatcherProvider()
    {
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-01", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.INCLUDES,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-01", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-01", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EXCLUDES,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-01", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-07", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.INCLUDES,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-07", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-07", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EXCLUDES,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-07", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-07", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATER,
        new List<object> {"2019-01-01", "2019-02-01"}, "2017-02-07", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATEREQUALS,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-07", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.GREATEREQUALS,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-01", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-07", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS,
        new List<object> {"2019-01-01", "2019-02-01"}, "2018-02-07", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-07", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS,
        new List<object> {"2019-01-01", "2019-02-01"}, "2019-02-01", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.REGEX,
        new List<object> {"2019-.*"}, "2019-07-06", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.REGEX,
        new List<object> {"2019-.*"}, "2017-07-06", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.REGEX,
        new List<object> {"2019-.*", "(.*)-03-(.*)"}, "2017-03-06", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.STARTSWITH,
        new List<object> {"2019", "2017"}, "2017-02-01", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.STARTSWITH,
        new List<object> {"2019"}, "2017-02-01", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.ENDSWITH,
        new List<object> {"01"}, "2017-02-01", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.ENDSWITH,
        new List<object> {"03", "02", "2017"}, "2017-02-01", false);
    }

    [Test, TestCaseSource("DateTimeMatcherProvider")]
    public void DateTimeMatcher(RolloutStrategyAttributeConditional conditional, List<object> vals, string suppliedVal,
      bool matches)
    {
      var rsa = new RolloutStrategyAttribute();
      rsa.Conditional = conditional;
      rsa.Values = vals.Select(v => v as object).ToList();
      rsa.Type = RolloutStrategyFieldType.DATETIME;

      Assert.AreEqual(matches, registry.FindMatcher(rsa).Match(suppliedVal, rsa));
    }

    public static IEnumerable<TestCaseData> DateTimeMatcherProvider()
    {
      // test equals
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2019-02-01T01:01:01Z", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.INCLUDES,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2019-02-01T01:01:01Z", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2019-02-01T01:01:01Z", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EXCLUDES,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2019-02-01T01:01:01Z", false);

      // test not equals
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EQUALS,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2017-02-01T01:01:01Z", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.INCLUDES,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2017-02-01T01:01:01Z", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.NOTEQUALS,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2017-02-01T01:01:01Z", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.EXCLUDES,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2017-02-01T01:01:01Z", true);

      // test  less & less =
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2016-02-01T01:01:01Z", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESS,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2020-02-01T01:01:01Z", false);

      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2019-02-01T01:01:01Z", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.LESSEQUALS,
        new List<object> {"2019-01-01T01:01:01Z", "2019-02-01T01:01:01Z"},"2019-02-02T01:01:01Z", false);

      // regex
      yield return new TestCaseData(RolloutStrategyAttributeConditional.REGEX,
        new List<object> {"2019-.*"}, "2019-07-06T01:01:01Z", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.REGEX,
        new List<object> {"2019-.*"}, "2016-07-06T01:01:01Z", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.REGEX,
        new List<object> {"2019-.*", "(.*)-03-(.*)"}, "2019-07-06T01:01:01Z", true);

      // starts with / ends with
      yield return new TestCaseData(RolloutStrategyAttributeConditional.STARTSWITH,
        new List<object> {"2019", "2017"}, "2017-03-06T01:01:01Z", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.STARTSWITH,
        new List<object> {"2019"}, "2017-03-06T01:01:01Z", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.ENDSWITH,
        new List<object> {":01Z"}, "2017-03-06T01:01:01Z", true);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.ENDSWITH,
        new List<object> {"03", "2017", "01:01"}, "2017-03-06T01:01:01Z", false);
      yield return new TestCaseData(RolloutStrategyAttributeConditional.ENDSWITH,
        new List<object> {"rubbish"}, "2017-03-06T01:01:01Z", false);
    }

    /*
      "2017-03-06T01:01:01Z"      | RolloutStrategyAttributeConditional.STARTS_WITH    | ["2019", "2017"]                                       | true
      "2017-03-06T01:01:01Z"      | RolloutStrategyAttributeConditional.STARTS_WITH    | ["2019"]                                               | false
      "2017-03-06T01:01:01Z"      | RolloutStrategyAttributeConditional.ENDS_WITH      | [":01Z"]                                               | true
      "2017-03-06T01:01:01Z"      | RolloutStrategyAttributeConditional.ENDS_WITH      | ["03", "2017", "01:01"]                                | false
      "2017-03-06T01:01:01Z"      | RolloutStrategyAttributeConditional.ENDS_WITH      | ["rubbish"]                                            | false
     */

  }
}
