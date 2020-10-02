using System;
using System.Collections.Generic;
using System.Web;
using Common.Logging;
using IO.FeatureHub.SSE.Model;

namespace FeatureHubSDK
{
  public interface GoogleAnalyticsClient
  {
    // if you wish to pass in the "value" field to analytics, add this to the "other" dictionary
    const string GaValue = "gaValue";
    // if you wish to override the CID, add this to the "other" dictionary
    const string Cid = "cid";

    void PostBatchUpdate(string batchData);
  }

  public class GoogleAnalyticsCollector : AnalyticsCollector
  {
    private static readonly ILog log = LogManager.GetLogger<GoogleAnalyticsCollector>();
    private string _uaKey;
    private string _cid;
    private GoogleAnalyticsClient _client;

    public string Cid
    {
      set
      {
        if (!string.IsNullOrWhiteSpace(value))
        {
          _cid = value;
        }
      }
      get => _cid;
    }

    public GoogleAnalyticsCollector(string uaKey, string cid, GoogleAnalyticsClient client)
    {
      if (string.IsNullOrWhiteSpace(uaKey))
      {
        throw new ArgumentException("uaKey cannot be null or empty");
      }

      if (client == null)
      {
        throw new ArgumentException("client cannot be null");
      }

      this._uaKey = uaKey;
      this._cid = cid;
      this._client = client;
    }

    public void LogEvent(string action, Dictionary<string, string> other, List<IFeatureStateHolder> featureStates)
    {
      var batchData = "";

      var finalCid = _cid == null ? other[GoogleAnalyticsClient.Cid] : _cid;

      if (finalCid == null)
      {
        log.Error("There is no CID provided for GA, not logging any events.");
        return;
      }

      var ev = (other != null && other[GoogleAnalyticsClient.GaValue] != null)
        ? ("&ev=" + HttpUtility.UrlEncode(other[GoogleAnalyticsClient.GaValue]))
        : "";

      var baseForEachLine = "v=1&tid=" + _uaKey + "&cid=" + finalCid + "&t=event&ec=FeatureHub%20Event&ea=" +
                            HttpUtility.UrlEncode(action) + ev + "&el=";

      foreach (var fsh in featureStates)
      {
        string line = null;
        switch (fsh.Type)
        {
          case FeatureValueType.BOOLEAN:
            line = fsh.BooleanValue == true ? "on" : "off";
            break;
          case FeatureValueType.STRING:
            line = fsh.StringValue;
            break;
          case FeatureValueType.NUMBER:
            line = fsh.NumberValue == null ? null : fsh.NumberValue.ToString();
            break;
          case FeatureValueType.JSON:
            break;
          case null:
            break;
          default:
            throw new ArgumentOutOfRangeException();
        }

        if (line != null)
        {
          line = HttpUtility.UrlEncode(fsh.Key + " : " + line);
          batchData += baseForEachLine + line + "\n";
        }
      }

      if (batchData.Length > 0)
      {
        _client.PostBatchUpdate(batchData);
      }
    }
  }
}
