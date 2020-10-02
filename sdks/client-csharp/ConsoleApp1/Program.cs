using System;
using FeatureHubSDK;
using IO.FeatureHub.SSE.Model;

namespace ConsoleApp1
{
  class Program
  {
    static void FeatureChanged(object sender, IFeatureStateHolder holder)
    {
      Console.WriteLine($"Received type {holder.Key}: {holder.StringValue}");
    }
    static void Main(string[] args)
    {
      Console.WriteLine("Hello World!");

      var fh = new FeatureHubRepository();
      fh.FeatureState("FLUTTER_COLOUR").FeatureUpdateHandler += (object sender, IFeatureStateHolder holder) =>
      {
        Console.WriteLine($"Received type {holder.Key}: {holder.StringValue}");
      };
      fh.ReadynessHandler += (sender, readyness) =>
      {
        Console.WriteLine($"Readyness is {readyness}");
      };
      fh.NewFeatureHandler += (sender, repository) =>
      {
        Console.WriteLine($"New features");
      };

      fh.AddAnalyticCollector(new GoogleAnalyticsCollector("UA-example", "1234-5678-abcd-abcd",
        new GoogleAnalyticsHttpClient()));

      var esl = new EventServiceListener();

      esl.Init("http://localhost:8553/features/default/ce6b5f90-2a8a-4b29-b10f-7f1c98d878fe/VNftuX5LV6PoazPZsEEIBujM4OBqA1Iv9f9cBGho2LJylvxXMXKGxwD14xt2d7Ma3GHTsdsSO8DTvAYF", fh);

      do
      {
        fh.LogAnalyticEvent("c-sharp-console");
        Console.Write("Press a Key");
      } while (Console.ReadLine() != "x");


      // Console.Write("Press a key");
      // Console.ReadKey();
      //
      // Console.Write("Press a key (changed context)");
      //
      // fh.ClientContext.UserKey("DJElif").Country(StrategyAttributeCountryName.Turkey).Attr("city", "istanbul").Build();
      // Console.ReadKey();
      //
      // Console.Write("Press a key (change context2)");
      // Console.ReadKey();
      //
      // fh.ClientContext.UserKey("AmyWiles").Country(StrategyAttributeCountryName.Unitedkingdom).Attr("city", "london").Build();
      // Console.WriteLine("Ready to close");
      // Console.ReadKey();
      esl.Close();
    }
  }
}
