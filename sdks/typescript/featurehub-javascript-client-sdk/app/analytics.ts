import { FeatureStateHolder } from './feature_state';
import { FeatureValueType } from './models';

export interface AnalyticsCollector {
  logEvent(action: string, other: Map<string, string>, featureStateAtCurrentTime: Array<FeatureStateHolder>);
}

export interface GoogleAnalyticsApiClient {
  cid(other: Map<string, string>): string;

  postBatchUpdate(batchData: string): void;
}

class BrowserGoogleAnalyticsApiClient implements GoogleAnalyticsApiClient {
  cid(other: Map<string, string>): string {
    return other.get('cid');
  }

  postBatchUpdate(batchData: string): void {
    const req = new XMLHttpRequest();
    req.open('POST', 'https://www.google-analytics.com/batch');
    req.send(batchData);
  }
}

type GoogleAnalyticsApiClientProvider = () => GoogleAnalyticsApiClient;

export class GoogleAnalyticsCollector implements AnalyticsCollector {
  private uaKey: string;
  private _cid: string;
  private apiClient: GoogleAnalyticsApiClient;

  public static googleAnalyticsClientProvider: GoogleAnalyticsApiClientProvider =
    () => new BrowserGoogleAnalyticsApiClient();

  constructor(uaKey: string, cid?: string, apiClient?: GoogleAnalyticsApiClient) {
    if (uaKey == null) {
      throw new Error('UA must be set');
    }

    this.uaKey = uaKey;
    this._cid = cid;

    if (apiClient) {
      this.apiClient = apiClient;
    } else {
      this.apiClient = GoogleAnalyticsCollector.googleAnalyticsClientProvider();
    }
  }

  set cid(value: string) {
    this._cid = value;
  }

  public logEvent(action: string, other: Map<string, string>,
    featureStateAtCurrentTime: Array<FeatureStateHolder>): void {
    const finalCid = this._cid ?? this.apiClient.cid(other);
    if (finalCid === undefined) {
      return; // cannot log this event
    }

    const ev = (other !== undefined && other !== null
      && other.get('gaValue') !== undefined) ? ('&ev=' + encodeURI(other.get('gaValue'))) : '';
    const baseForEachLine = 'v=1&tid=' + this.uaKey
      + '&cid=' + finalCid + '&t=event&ec=FeatureHub%20Event&ea=' + encodeURI(action) + ev + '&el=';

    let postString = '';
    featureStateAtCurrentTime.forEach((f) => {
      let line = null;
      if (f.getType() === FeatureValueType.Boolean) {
        line = f.getBoolean() === true ? 'on' : 'off';
      } else if (f.getType() === FeatureValueType.String) {
        line = f.getString();
      } else if (f.getType() === FeatureValueType.Number) {
        line = f.getNumber().toString();
      }

      if (line !== null && line !== undefined) {
        line = encodeURI(f.getKey() + ' : ' + line);
        postString = postString + baseForEachLine + line + '\n';
      }
    });

    if (postString.length > 0) {
      this.apiClient.postBatchUpdate(postString);
    }
  }
}
