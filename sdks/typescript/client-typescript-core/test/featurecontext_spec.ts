import { FeatureContext, featureHubRepository, FeatureState, FeatureValueType, SSEResultState } from '../app';
import { expect } from 'chai';

describe('feature context should work as expected', () => {
  it('should respond as expected to a proper feature', () => {
    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.BOOLEAN, value: true}),
    ];

    featureHubRepository.notify(SSEResultState.Features, features);

    // tslint:disable-next-line:no-unused-expression
    expect(FeatureContext.exists('banana')).to.be.true;
    // tslint:disable-next-line:no-unused-expression
    expect(FeatureContext.isActive('banana')).to.be.true;
    // tslint:disable-next-line:no-unused-expression
    expect(FeatureContext.isSet('banana')).to.be.true;

    featureHubRepository.notify(SSEResultState.Feature,
                                new FeatureState({id: '1', key: 'banana', version: 2,
                                  type: FeatureValueType.BOOLEAN, value: false}),
      );

    // tslint:disable-next-line:no-unused-expression
    expect(FeatureContext.exists('banana')).to.be.true;
    // tslint:disable-next-line:no-unused-expression
    expect(FeatureContext.isActive('banana')).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(FeatureContext.isSet('banana')).to.be.true;

    featureHubRepository.notify(SSEResultState.DeleteFeature,
                                new FeatureState({id: '1', key: 'banana', version: 3,
        type: FeatureValueType.BOOLEAN, value: true}),
    );

    // tslint:disable-next-line:no-unused-expression
    expect(FeatureContext.exists('banana')).to.be.true;
    // tslint:disable-next-line:no-unused-expression
    expect(FeatureContext.isActive('banana')).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(FeatureContext.isSet('banana')).to.be.false;
  });
});
