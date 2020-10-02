using System;
using System.Net.Http;
using System.Net.Http.Headers;

namespace FeatureHubSDK
{
  public class GoogleAnalyticsHttpClient : IGoogleAnalyticsClient
  {
    private HttpClient _client;

    public GoogleAnalyticsHttpClient()
    {
      _client = new HttpClient();
      _client.DefaultRequestHeaders.Host = "www.google-analytics.com";
    }

    public void PostBatchUpdate(string batchData)
    {
      var content = new StringContent(batchData);
      content.Headers.ContentType = MediaTypeHeaderValue.Parse("application/x-www-form-urlencoded");

      _client.PostAsync("https://www.google-analytics.com/batch", content);
    }
  }
}
