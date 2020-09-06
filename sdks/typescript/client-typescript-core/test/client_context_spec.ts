import {
  ClientFeatureRepository,
  StrategyAttributeCountryName,
  StrategyAttributeDeviceName,
  StrategyAttributePlatformName
} from '../app';
import { expect } from 'chai';

describe('Client context should be able to encode as expected', () => {
  it('should have proper encoding', () => {
    const repo = new ClientFeatureRepository();

    repo.clientContext.userKey('DJElif')
      .sessionKey('VirtualBurningMan')
      .country(StrategyAttributeCountryName.Turkey)
      .platform(StrategyAttributePlatformName.Macos)
      .device(StrategyAttributeDeviceName.Desktop)
      .attribute_value('city', 'istanbul')
      .attribute_values('musical styles', ['deep', 'psychedelic'])
      .version('4.3.2')
      .build();

    // do it twice to ensure we can reset everything
    repo.clientContext.userKey('DJElif')
      .sessionKey('VirtualBurningMan')
      .device(StrategyAttributeDeviceName.Desktop)
      .attribute_value('city', 'istanbul')
      .attribute_values('musical styles', ['deep', 'psychedelic'])
      .version('4.3.2')
      .country(StrategyAttributeCountryName.Turkey)
      .platform(StrategyAttributePlatformName.Macos)
      .build();

    expect(repo.clientContext.generateHeader()).to.eq('city=istanbul,country=turkey,device=desktop,musical styles=deep%2Cpsychedelic,platform=macos,session=VirtualBurningMan,userkey=DJElif,version=4.3.2');
  });
});
