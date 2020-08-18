import { ClientFeatureRepository, FeatureState, FeatureValueType, Readyness, SSEResultState } from '../app';
import { expect } from 'chai';

describe('Readyness listeners should fire on appropriate events', () => {
  let repo: ClientFeatureRepository;

  beforeEach(() => {
    repo = new ClientFeatureRepository();
  });

  it('should start not ready, receive a list of features and become ready and on failure be failed', () => {

    let readynessTrigger = 0;
    let lastReadyness: Readyness = undefined;
    repo.addReadynessListener((state) => {
      lastReadyness = state;
      return readynessTrigger++;
    });

    expect(repo.readyness).to.eq(Readyness.NotReady);
    expect(readynessTrigger).to.eq(1);

    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.BOOLEAN, value: true}),
    ];

    repo.notify(SSEResultState.Features, features);

    expect(repo.readyness).to.eq(Readyness.Ready);
    expect(lastReadyness).to.eq(Readyness.Ready);
    expect(readynessTrigger).to.eq(2);

    repo.notify(SSEResultState.Failure, null);
    expect(repo.readyness).to.eq(Readyness.Failed);
    expect(lastReadyness).to.eq(Readyness.Failed);
    expect(readynessTrigger).to.eq(3);
  });

  it('we should be able to be ready and then be not ready on a bye', () => {
    let readynessTrigger = 0;
    let lastReadyness: Readyness = undefined;
    repo.addReadynessListener((state) => {
      lastReadyness = state;
      return readynessTrigger++;
    });
    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.BOOLEAN, value: true}),
    ];

    repo.notify(SSEResultState.Features, features);
    repo.notify(SSEResultState.Bye, undefined);
    expect(repo.readyness).to.eq(Readyness.NotReady);
    expect(lastReadyness).to.eq(Readyness.NotReady);
    expect(readynessTrigger).to.eq(3);

  });
});


