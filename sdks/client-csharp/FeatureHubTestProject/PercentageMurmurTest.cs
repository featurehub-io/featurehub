

using System;
using FeatureHubSDK;
using NUnit.Framework;

namespace FeatureHubTestProject
{
  class PercentageMurmurTest
  {
    [Test]
    public void BasicMumurTest()
    {
      var featureId = Guid.NewGuid().ToString();
      var calc = new PercentageMurmur3Calculator();

      var counter = 0;
      for (var count = 0; count < 1000; count++)
      {
        if (calc.DetermineClientPercentage(Guid.NewGuid().ToString(), featureId) <= 200000)
        {
          counter++;
        }
      }
      Console.WriteLine($"Murmur counter is {counter}");
      Assert.LessOrEqual(160, counter);
      Assert.GreaterOrEqual(240, counter);
    }
  }
}
