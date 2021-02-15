

using System.Collections.Generic;
using System.Threading.Tasks;
using FeatureHubSDK;
using IO.FeatureHub.SSE.Model;
using NUnit.Framework;

namespace FeatureHubTestProject
{
  class TestPercentageCalculator : IPercentageCalculator
  {
    public int pc = 21;

    public int DetermineClientPercentage(string percentageText, string featureId)
    {
      return pc;
    }
  }

  public class TestClientContext : BaseClientContext
  {
    public TestClientContext() : base(null, null)
    {}

    public override IFeature this[string name] => throw new System.NotImplementedException();

    public override async Task<IClientContext> Build()
    {
      return this;
    }

    public override IEdgeService EdgeService { get; }

    public override void Close()
    {
      throw new System.NotImplementedException();
    }
  }

  class ApplyFeatureTest
  {
    private ApplyFeature _applyFeature;
    private TestPercentageCalculator _percentageCalculator;


    [SetUp]
    public void Setup()
    {
      _percentageCalculator = new TestPercentageCalculator();
      _applyFeature = new ApplyFeature(_percentageCalculator, new MatcherRegistry());
    }

    [Test]
    public void NullContextDefaultValue()
    {
      // given: we have a rollout strategy that is percentage based
      var rs = new RolloutStrategy("id", "name");
      rs.Percentage = 21;
      rs.Value = "blue";

      // and: we have a context
      var cc = new TestClientContext().UserKey("mary@mary.com");

      var val = _applyFeature.Apply(new List<RolloutStrategy> {rs}, "fred", "id", null);

      Assert.AreEqual(val.Matched, false);
    }

    [Test, TestCaseSource("BasicPercentProvider")]
    public void MatchPercentageToCalculation(int underPercent, string expected, bool matched)
    {
      // given: we have a rollout strategy that is percentage based
      var rs = new RolloutStrategy("id", "name");
      rs.Percentage = underPercent;
      rs.Value = "blue";

      // and: we have a context
      var cc = new TestClientContext().UserKey("mary@mary.com");

      var val = _applyFeature.Apply(new List<RolloutStrategy> {rs}, "fred", "id", cc);

      Assert.AreEqual(expected, val.Value);
      Assert.AreEqual(matched, val.Matched);
    }

    public static IEnumerable<TestCaseData> BasicPercentProvider()
    {
      yield return new TestCaseData(22, "blue", true);
      yield return new TestCaseData(75, "blue", true);
      yield return new TestCaseData(15, null, false);
      yield return new TestCaseData(20, null, false);
    }

    [Test, TestCaseSource("NoStrategyPercentProvider")]
    public void NoRolloutStrategy(int underPercent, string expected, bool matched)
    {
      // and: we have a context
      var cc = new TestClientContext().UserKey("mary@mary.com");

      var val = _applyFeature.Apply(new List<RolloutStrategy> {}, "fred", "id", cc);

      Assert.AreEqual(expected, val.Value);
      Assert.AreEqual(matched, val.Matched);
    }

    public static IEnumerable<TestCaseData> NoStrategyPercentProvider()
    {
      yield return new TestCaseData(22, null, false);
      yield return new TestCaseData(75, null, false);
      yield return new TestCaseData(15, null, false);
    }
  }
}
