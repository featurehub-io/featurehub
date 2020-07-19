using System;
using FeatureHubSDK;

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
      fh.FeatureState("FLUTTER_COLOUR").FeatureUpdateHandler += FeatureChanged;
      fh.ReadynessHandler += (sender, readyness) =>
      {
        Console.WriteLine($"Readyness is {readyness}");
      };
      fh.NewFeatureHandler += (sender, repository) =>
      {
        Console.WriteLine($"New features");
      };

      var esl = new EventServiceListener();

      esl.Init("http://192.168.86.49:8553/features/default/ce6b5f90-2a8a-4b29-b10f-7f1c98d878fe/VNftuX5LV6PoazPZsEEIBujM4OBqA1Iv9f9cBGho2LJylvxXMXKGxwD14xt2d7Ma3GHTsdsSO8DTvAYF", fh);

      Console.ReadKey();

      esl.Close();
    }
  }
}
