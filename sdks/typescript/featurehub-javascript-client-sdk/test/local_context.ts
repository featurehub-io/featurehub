import { expect } from 'chai';
import { Environment, FeatureState, FeatureValueType, LocalClientContext } from '../app';

describe('Local context should be able to evaluate', () => {
  it('the ', () => {
    const context = new LocalClientContext(new Environment({
      features: [
        new FeatureState({
          id: '1',
          key: 'banana',
          version: 1,
          type: FeatureValueType.Boolean,
          value: true,
        }),
        new FeatureState({
          id: '2',
          key: 'organge',
          version: 1,
          type: FeatureValueType.Boolean,
          value: false,
        }),
      ],
    }));

    expect(context.getBoolean('banana')).to.be.true;
    expect(context.getBoolean('organge')).to.be.false;
  });
});
