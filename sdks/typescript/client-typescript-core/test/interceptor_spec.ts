import { ClientFeatureRepository, FeatureHubRepository, FeatureState, FeatureValueType, SSEResultState } from '../app';
import { FeatureStateValueInterceptor, InterceptorValueMatch } from '../app/feature_state_holders';
import { expect } from 'chai';

class KeyValueInterceptor implements FeatureStateValueInterceptor {
  private key: string;
  private value: string;

  constructor(key: string, value: string) {
    this.key = key;
    this.value = value;
  }

  matched(key: string): InterceptorValueMatch {
    return key === this.key ? new InterceptorValueMatch(this.value) : undefined;
  }

  repository(repo: FeatureHubRepository): void {
    //
  }
}

describe('Interceptor functionality works as expected', () => {
  let repo: ClientFeatureRepository;

  beforeEach(() => {
    repo = new ClientFeatureRepository();
  });

  it('should allow us to override unlocked values', () => {
    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.Boolean, value: false}),
      new FeatureState({id: '1', key: 'apricot', version: 1, type: FeatureValueType.Number, value: 16.2}),
      new FeatureState({id: '1', key: 'nashi', version: 1, type: FeatureValueType.String, value: 'oook'}),
      new FeatureState({
        id: '3', key: 'peach', version: 1, type: FeatureValueType.Json,
        value: '{"variety": "golden queen"}'
      }),
    ];

    repo.notify(SSEResultState.Features, features);

    repo.addValueInterceptor(new KeyValueInterceptor('banana', 'true'));
    repo.addValueInterceptor(new KeyValueInterceptor('apricot', '17.3'));
    repo.addValueInterceptor(new KeyValueInterceptor('nashi', 'wombat'));
    repo.addValueInterceptor(new KeyValueInterceptor('peach', '{}'));

    expect(repo.feature('banana').getBoolean()).to.eq(true);
    expect(repo.feature('apricot').getNumber()).to.eq(17.3);
    expect(repo.feature('nashi').getString()).to.eq('wombat');
    expect(repo.feature('peach').getRawJson()).to.eq('{}');
  });

  it('should not allow us to override locked values', () => {
    const features = [
      new FeatureState({id: '1', key: 'banana', version: 1, type: FeatureValueType.Boolean, l: true, value: false}),
      new FeatureState({id: '1', key: 'apricot', version: 1, type: FeatureValueType.Number, l: true, value: 16.2}),
      new FeatureState({id: '1', key: 'nashi', version: 1, type: FeatureValueType.String, l: true, value: 'oook'}),
      new FeatureState({
        id: '3', key: 'peach', version: 1, type: FeatureValueType.Json, l: true,
        value: '{"variety": "golden queen"}'
      }),
    ];

    repo.notify(SSEResultState.Features, features);

    repo.addValueInterceptor(new KeyValueInterceptor('banana', 'true'));
    repo.addValueInterceptor(new KeyValueInterceptor('apricot', '17.3'));
    repo.addValueInterceptor(new KeyValueInterceptor('nashi', 'wombat'));
    repo.addValueInterceptor(new KeyValueInterceptor('peach', '{}'));

    expect(repo.feature('banana').getBoolean()).to.eq(false);
    expect(repo.feature('apricot').getNumber()).to.eq(16.2);
    expect(repo.feature('nashi').getString()).to.eq('oook');
    expect(repo.feature('peach').getRawJson()).to.eq('{"variety": "golden queen"}');
  });
});
