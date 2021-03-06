import {
  EdgeService, FeatureHubConfig,
  StrategyAttributeCountryName,
  StrategyAttributeDeviceName,
  StrategyAttributePlatformName
} from '../app';
import { expect } from 'chai';
import { Substitute, Arg, SubstituteOf } from '@fluffy-spoon/substitute';
import { InternalFeatureRepository } from '../app/internal_feature_repository';
import { ClientEvalFeatureContext, ServerEvalFeatureContext } from '../app/client_context';

describe('Client context should be able to encode as expected', () => {
  let repo: SubstituteOf<InternalFeatureRepository>;
  let config: SubstituteOf<FeatureHubConfig>;
  let edge: SubstituteOf<EdgeService>;

  beforeEach(() => {
    repo = Substitute.for<InternalFeatureRepository>();
    config = Substitute.for<FeatureHubConfig>();
    edge = Substitute.for<EdgeService>();
  });

  it('the server context should trigger a header change update', async () => {
    const serverContext = new ServerEvalFeatureContext(repo, config, () => edge);
    await serverContext.userKey('DJElif')
      .sessionKey('VirtualBurningMan')
      .country(StrategyAttributeCountryName.Turkey)
      .platform(StrategyAttributePlatformName.Macos)
      .device(StrategyAttributeDeviceName.Desktop)
      .attribute_value('city', 'istanbul')
      .attribute_values('musical styles', ['deep', 'psychedelic'])
      .version('4.3.2')
      .build();

    repo.received(1).notReady();

    // do it twice to ensure we can reset everything
    await serverContext.userKey('DJElif')
      .sessionKey('VirtualBurningMan')
      .device(StrategyAttributeDeviceName.Desktop)
      .attribute_value('city', 'istanbul')
      .attribute_values('musical styles', ['deep', 'psychedelic'])
      .version('4.3.2')
      .country(StrategyAttributeCountryName.Turkey)
      .platform(StrategyAttributePlatformName.Macos)
      .build();

    edge.received(2).contextChange('city=istanbul,country=turkey,device=desktop,musical styles=deep%2Cpsychedelic,platform=macos,session=VirtualBurningMan,userkey=DJElif,version=4.3.2');
  });

  it('the server context should close the current edge if it has been told to reset edge', async () => {
    const serverContext = new ServerEvalFeatureContext(repo, config, () => edge);
    edge.requiresReplacementOnHeaderChange().mimicks(() => true);
    await serverContext.userKey('DJElif')
      .sessionKey('VirtualBurningMan').build();
    await serverContext.userKey('DJElif')
      .sessionKey('VirtualBurningMan1').build();
    edge.received(1).close();
  });

  it('the client context should not trigger a context change or a not ready', async () => {
    const clientContext = new ClientEvalFeatureContext(repo, config, edge);
    await clientContext.userKey('DJElif')
      .sessionKey('VirtualBurningMan').build();
    repo.received(0).notReady();
    edge.received(0).contextChange(Arg.all());
  });
});
