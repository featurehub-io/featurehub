import { featureHubRepository, SSEResultState } from 'featurehub-repository/dist';
import * as EventSource from 'eventsource';

export class FeatureHubEventSourceClient {
  private eventSource: EventSource;
  private sdkUrl: string;

  constructor(sdkUrl: string) {
    this.sdkUrl = sdkUrl;
  }

  init() {
    this.eventSource = new EventSource(this.sdkUrl);

    [SSEResultState.Features, SSEResultState.Feature, SSEResultState.DeleteFeature,
        SSEResultState.Bye, SSEResultState.Failure, SSEResultState.Ack].forEach((name) => {
          const fName = name.toString();
      this.eventSource.addEventListener(fName,
                                        e => {
        try {
          // console.log("received ", fName, JSON.stringify(e));
          featureHubRepository.notify(name, JSON.parse((e as any).data));
        } catch (e) { console.error(JSON.stringify(e));}
                                        });
    });

    this.eventSource.onerror = (e) => {
      // console.error("got error", e);
      featureHubRepository.notify(SSEResultState.Failure, null);
    };
  }

  close() {
    if (this.eventSource != null) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
