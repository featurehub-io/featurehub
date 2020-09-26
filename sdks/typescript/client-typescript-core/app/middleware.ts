/// Our goal here is to determine if the appropriate environment variable is loaded and if so,
/// allow overriding of the fh repository. But it has to be addressed in the request context.
///
/// For this we are using the W3C Baggage standard for future supportability

import { ClientFeatureRepository, FeatureHubRepository, Readyness } from './client_feature_repository';
import { FeatureListener, FeatureStateHolder } from './feature_state';
import { FeatureValueType } from './models/models';
import { FeatureStateValueInterceptor } from './feature_state_holders';

class BaggageHolder implements FeatureStateHolder {
  protected readonly existing: FeatureStateHolder;
  protected readonly value: string;

  constructor(existing: FeatureStateHolder, value: string) {
    this.existing = existing;
    this.value = value;
  }

// tslint:disable-next-line:no-empty
  addListener(listener: FeatureListener): void {
  }

  getBoolean(): boolean | undefined {
    if (this.existing.isLocked()) {
      return this.existing.getBoolean();
    }

    return this.existing.getType() === FeatureValueType.Boolean ? ('true' === this.value) : undefined;
  }

  getKey(): string | undefined {
    return this.existing.getKey();
  }

  getNumber(): number | undefined {
    if (this.existing.isLocked()) {
      return this.existing.getNumber();
    }

    if (this.existing.getType() === FeatureValueType.Number && this.value !== undefined) {
      if (this.value.includes('.')) {
        return parseFloat(this.value);
      } else {
        // tslint:disable-next-line:radix
        return parseInt(this.value);
      }
    }

    return undefined;
  }

  getRawJson(): string | undefined {
    return undefined;
  }

  getString(): string | undefined {
    if (this.existing.isLocked()) {
      return this.existing.getString();
    }

    if (this.existing.getType() === FeatureValueType.String) {
      return this.value;
    }

    return undefined;
  }

  getType(): FeatureValueType | undefined {
    return this.existing.getType();
  }

  getVersion(): number | undefined {
    return this.existing.getVersion();
  }

  isLocked(): boolean | undefined {
    return this.existing.isLocked();
  }

  isSet(): boolean {
    return this.value != null;
  }

  triggerListeners(feature: FeatureStateHolder): void {
    this.existing.triggerListeners(feature);
  }

  addValueInterceptor(matcher: FeatureStateValueInterceptor): void {
    this.existing.addValueInterceptor(matcher);
  }
}

class BaggageRepository implements FeatureHubRepository {
  private readonly repo: FeatureHubRepository;
  private baggage: Map<string, string|undefined>;
  private mappedBaggage = new Map<string, FeatureStateHolder>();

  constructor(repo: FeatureHubRepository, baggage: Map<string, string>) {
    this.repo = repo;
    this.baggage = baggage;
  }

  get readyness(): Readyness {
    return this.repo.readyness;
  }

  hasFeature(key: string): FeatureStateHolder {
    return this.feature(key);
  }

  feature(key: string): FeatureStateHolder {
    const realFeature = this.repo.hasFeature(key);

    if (realFeature !== undefined && realFeature.getType() !== undefined) {
      if ( this.baggage.has(key)) {
        let fh = this.mappedBaggage.get(key);

        // we don't map json types, create it if it isn't there
        if (fh === undefined && realFeature.getType() !== FeatureValueType.Json) {
          fh = new BaggageHolder(realFeature, this.baggage.get(key));
          this.mappedBaggage.set(key, fh);
        }

        // return it if we created it
        if (fh !== undefined) {
          return fh;
        }
      }
    }

    return realFeature;
  }

  logAnalyticsEvent(action: string, other?: Map<string, string>) {
    const otherCopy = other ? other : new Map<string, string>();
    const baggageCopy = new Map<string, string>([...this.baggage, ...otherCopy]);

    // merge bother together and

    this.repo.logAnalyticsEvent(action, baggageCopy);
  }

  simpleFeatures(): Map<string, string | undefined> {
    // captures what they are right now
    const features = this.repo.simpleFeatures();

    // override them with what we have
    this.baggage.forEach((value, key) => features.set(key, value));

    return features;
  }
}

export function featurehubMiddleware(repo: FeatureHubRepository) {

  return (req: any, res: any, next: any) => {
    let reqRepo: FeatureHubRepository = repo;

    if (process.env.FEATUREHUB_ACCEPT_BAGGAGE !== undefined) {
      const baggage = req.header('baggage');

      if (baggage != null) {
        const baggageMap = new Map<string, string|undefined>();

        // we are expecting a single key/value pair, fhub=
        baggage.split(',')
          .map(b => b.trim())
          .filter(b => b.startsWith('fhub='))
          .forEach(b => {
            //
            const fhub = decodeURIComponent(b.substring(5));
            fhub.split(',')
              .forEach(feature => {
                const parts = feature.split('=');
                if (parts.length === 2) {
                  baggageMap.set(parts[0], decodeURIComponent(parts[1]));
                } else if (parts.length === 1) {
                  baggageMap.set(parts[0], undefined);
                }
              });
          });

        if (baggageMap.size > 0) {
          reqRepo = new BaggageRepository(repo, baggageMap);
        }
      }
    }

    req.featureHub = reqRepo;

    next();
  };
}
