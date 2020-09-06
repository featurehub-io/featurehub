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

      var esl = new EventServiceListener();

      esl.Init("http://localhost:8553/features/default/ec6a720b-71ac-4cc1-8da1-b5e396fa00ca/Kps0MAqsGt5QhgmwMEoRougAflM2b8Q9e1EFeBPHtuIF0azpcCXeeOw1DabFojYdXXr26fyycqjBt3pa", fh);

      Console.Write("Press a key");
      Console.ReadKey();

      Console.Write("Press a key (changed context)");

      fh.ClientContext.UserKey("DJElif").Country(StrategyAttributeCountryName.Turkey).Attr("city", "istanbul").Build();
      Console.ReadKey();

      Console.Write("Press a key (change context2)");
      Console.ReadKey();

      fh.ClientContext.UserKey("AmyWiles").Country(StrategyAttributeCountryName.Unitedkingdom).Attr("city", "london").Build();
      Console.WriteLine("Ready to close");
      Console.ReadKey();
      esl.Close();
    }
  }
}
