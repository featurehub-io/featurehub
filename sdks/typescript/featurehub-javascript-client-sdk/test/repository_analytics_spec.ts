import {
  ClientFeatureRepository, EdgeFeatureHubConfig,
  FeatureState,
  FeatureStateHolder,
  FeatureValueType,
  SSEResultState
} from '../app';
import { expect } from 'chai';

describe('We should be able to log an analytics event', () => {
  let repo: ClientFeatureRepository;
  let firedAction: string;
  let firedOther: Map<string, string>;
  let firedFeatures: Array<FeatureStateHolder>;

  beforeEach(() => {
    repo = new ClientFeatureRepository();
    firedAction = undefined;
    firedOther = undefined;
    firedFeatures = undefined;

    repo.addAnalyticCollector({
      logEvent: function (action: string, other: Map<string, string>,
        featureStateAtCurrentTime: Array<FeatureStateHolder>) {
        firedAction = action;
        firedOther = other;
        firedFeatures = featureStateAtCurrentTime;
      }
    });
  });

  it('should allow us to fire analytics events via the config into the repo', () => {
    repo = new ClientFeatureRepository();
    const fhConfig = new EdgeFeatureHubConfig('http://localhost:8080', '123*123');
    fhConfig.repository(repo);
    fhConfig.addAnalyticCollector({
      logEvent: function (action: string, other: Map<string, string>,
        featureStateAtCurrentTime: Array<FeatureStateHolder>) {
        firedAction = action;
        firedOther = other;
        firedFeatures = featureStateAtCurrentTime;
      }
    });
    repo.logAnalyticsEvent('name');
    expect(firedFeatures.length).to.eq(0);
    expect(firedAction).to.eq('name');
    // tslint:disable-next-line:no-unused-expression
    expect(firedOther).to.be.undefined;
  });

  it('Should enable us to log an event with no other and no features', () => {
    repo.logAnalyticsEvent('name');
    expect(firedFeatures.length).to.eq(0);
    expect(firedAction).to.eq('name');
    // tslint:disable-next-line:no-unused-expression
    expect(firedOther).to.be.undefined;
  });

  it('should carry through the other field', () => {
    const other = new Map();
    other.set('ga', 'value');
    repo.logAnalyticsEvent('name', other);
    expect(firedFeatures.length).to.eq(0);
    expect(firedAction).to.eq('name');
    // tslint:disable-next-line:no-unused-expression
    expect(firedOther).to.eq(other);
  });

  it('should snapshot the features', () => {
    const features = [
      new FeatureState({ id: '1', key: 'banana', version: 1, type: FeatureValueType.Boolean, value: true }),
    ];

    repo.notify(SSEResultState.Features, features);

    repo.logAnalyticsEvent('name');

    expect(firedFeatures.length).to.eq(1);
    const fs = firedFeatures[0];
    // tslint:disable-next-line:no-unused-expression
    expect(fs.isSet()).to.be.true;
    // tslint:disable-next-line:no-unused-expression
    expect(fs.getBoolean()).to.be.true;
    expect(fs.getKey()).to.eq('banana');
  });
});
