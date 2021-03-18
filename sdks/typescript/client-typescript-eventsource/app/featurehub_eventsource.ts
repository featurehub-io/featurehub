import { ClientFeatureRepository, featureHubRepository, SSEResultState } from 'featurehub-repository/dist';
import * as EventSource from 'eventsource';

export class FeatureHubEventSourceClient {
  private eventSource: EventSource;
  private sdkUrl: string;
  private _repository: ClientFeatureRepository;
  private _header: string;

  constructor(sdkUrl: string, repository?: ClientFeatureRepository) {
    this.sdkUrl = sdkUrl;
    this._repository = repository || featureHubRepository;
  }

  init() {
    const options: any = {};
    if (this._header) {
      options.headers = {
        'x-featurehub': this._header
      };
    }
    this.eventSource = new EventSource(this.sdkUrl, options);

    [SSEResultState.Features, SSEResultState.Feature, SSEResultState.DeleteFeature,
        SSEResultState.Bye, SSEResultState.Failure, SSEResultState.Ack].forEach((name) => {
          const fName = name.toString();
          this.eventSource.addEventListener(fName,
                                            e => {
        try {
          // console.log("received ", fName, JSON.stringify(e));
          this._repository.notify(name, JSON.parse((e as any).data));
        } catch (e) { console.error(JSON.stringify(e)); }
                                        });
    });

    this.eventSource.onerror = (e) => {
      // console.error("got error", e);
      this._repository.notify(SSEResultState.Failure, null);
    };
  }

  close() {
    if (this.eventSource != null) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
