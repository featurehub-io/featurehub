

using System;
using System.Collections.Generic;
using FeatureHubSDK;
using IO.FeatureHub.SSE.Model;
using NUnit.Framework;

namespace FeatureHubTestProject
{
  public class ContextTest
  {
    FeatureHubRepository _repository;

    [SetUp]
    public void Setup()
    {
      _repository = new FeatureHubRepository();
    }

    internal class EdgeServiceStub : IEdgeService
    {
      public string header;
      public int closeCalled = 0;
      public bool replace = false;

      public void ContextChange(string header)
      {
        this.header = header;
      }

      public bool ClientEvaluation => true;
      public bool IsRequiresReplacementOnHeaderChange => replace;

      public void Close()
      {
        closeCalled++;
      }

      public void Poll()
      {
      }
    }

    [Test]
    public void ChangeInContextFiresRequestToEdgeService()
    {
      var edgeStub = new EdgeServiceStub();
      var ctx = new ServerEvalFeatureContext(_repository, null, () => edgeStub)
        .Attr("city", "Istanbul City")
        .Attrs("family", new List<String> {"Bambam", "DJ Elif"})
        .Country(StrategyAttributeCountryName.Turkey)
        .Platform(StrategyAttributePlatformName.Ios)
        .Device(StrategyAttributeDeviceName.Mobile)
        .UserKey("tv-show")
        .Version("6.2.3")
        .SessionKey("session-key")
        .Build();

      Assert.AreEqual(_repository, ctx.Repository);
      Assert.AreEqual("Istanbul City", ctx.GetAttr("city", "here"));
      Assert.AreEqual("here", ctx.GetAttr("city-scape", "here"));

      Assert.AreEqual(
        "city=Istanbul+City,country=turkey,device=mobile,family=Bambam%2cDJ+Elif,platform=ios,session=session-key,userkey=tv-show,version=6.2.3", edgeStub.header);

      Assert.NotNull(ctx.ToString());

      Assert.NotNull(ctx["fred"]);
      Assert.AreEqual(edgeStub, ctx.EdgeService);

      ctx.Clear().Build();
      Assert.AreEqual("", edgeStub.header);
    }

    [Test]
    public void EnsureStubIsReplacedOnBuildForServerEval()
    {
      var edgeStub = new EdgeServiceStub();
      edgeStub.replace = true;
      var ctx = new ServerEvalFeatureContext(_repository, null, () => edgeStub).Build();
      Assert.AreEqual(0, edgeStub.closeCalled);
      ctx.Attr("replaceme", "now");
      ctx.Build();
      Assert.AreEqual(1, edgeStub.closeCalled);
    }

    [Test]
    public void EnabledFlagWorksIsTrueOnlyOnTrue()
    {
      var ctx = new ClientEvalFeatureContext(_repository, null, () => null);
      Assert.AreEqual(false, ctx.IsEnabled("1"));
      _repository.Notify(SSEResultState.Features, RepositoryTest.EncodeFeatures(true, 2, FeatureValueType.BOOLEAN));
      Assert.AreEqual(true, ctx.IsEnabled("1"));
      _repository.Notify(SSEResultState.Features, RepositoryTest.EncodeFeatures(false, 3, FeatureValueType.BOOLEAN));
      Assert.AreEqual(false, ctx.IsEnabled("1"));
    }

  }
}
