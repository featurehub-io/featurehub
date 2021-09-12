import {
  ClientFeatureRepository,
  FeatureState,
  FeatureValueType,
  SSEResultState
} from '../app';
import { expect } from 'chai';

describe('if a feature is deleted it becomes undefined', () => {
  let repo: ClientFeatureRepository;

  beforeEach(() => {
    repo = new ClientFeatureRepository();
  });

  it('should allow us to delete a feature', () => {
    const features = [
      new FeatureState({ id: '1', key: 'banana', version: 1, type: FeatureValueType.Boolean, value: true }),
    ];

    repo.notify(SSEResultState.Features, features);
    expect(repo.getFeatureState('banana').getBoolean()).to.eq(true);
    expect(repo.getFlag('banana')).to.eq(true);
    repo.notify(SSEResultState.DeleteFeature, features[0]);
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('banana').getBoolean()).to.undefined;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFlag('banana')).to.undefined;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.isSet('banana')).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('banana').isSet()).to.be.false;
  });

  it('should ignore deleting a feature that doesnt exist', () => {
    repo.notify(SSEResultState.DeleteFeature,
      new FeatureState(
        { id: '1', key: 'banana', version: 1, type: FeatureValueType.Boolean, value: true })
    );

    // tslint:disable-next-line:no-unused-expression
    expect(repo.getFeatureState('banana').isSet()).to.be.false;
  });
});
