import { MatcherRegistry } from '../app/strategy_matcher';
import {
  RolloutStrategyAttribute,
  RolloutStrategyAttributeConditional,
  RolloutStrategyFieldType
} from '../app';
import { expect } from 'chai';

describe('test the strategy matchers', () => {
  let matcher: MatcherRegistry;
  let type: RolloutStrategyFieldType;

  beforeEach(() => {
    matcher = new MatcherRegistry();
  });

  function equals(condition: RolloutStrategyAttributeConditional, vals: any[],
    suppliedVal: string, matches: boolean): void {
    const rsa = new RolloutStrategyAttribute();
    rsa.conditional = condition;
    rsa.type = type;
    rsa.values = vals;

    expect(matcher.findMatcher(rsa).match(suppliedVal, rsa)).to.eq(matches);
  }

  it('the boolean strategy matcher should work as expected', () => {
    type = RolloutStrategyFieldType.Boolean;

    equals(RolloutStrategyAttributeConditional.Equals, ['true'], 'true', true);
    equals(RolloutStrategyAttributeConditional.NotEquals, ['true'], 'true', false);
    equals(RolloutStrategyAttributeConditional.NotEquals, ['true'], 'false', true);
    equals(RolloutStrategyAttributeConditional.Equals, [true], 'true', true);
    equals(RolloutStrategyAttributeConditional.Equals, ['true'], 'false', false);
    equals(RolloutStrategyAttributeConditional.Equals, [true], 'false', false);
    equals(RolloutStrategyAttributeConditional.Equals, [false], 'false', true);
    equals(RolloutStrategyAttributeConditional.Equals, [false], 'true', false);
    equals(RolloutStrategyAttributeConditional.Equals, ['false'], 'true', false);
  });

  it('the string strategy matcher should work as expected', () => {
    type = RolloutStrategyFieldType.String;

    equals(RolloutStrategyAttributeConditional.Equals, ['a', 'b'], null, false);
    equals(RolloutStrategyAttributeConditional.Equals, ['a', 'b'], 'a', true);
    equals(RolloutStrategyAttributeConditional.Includes, ['a', 'b'], 'a', true);
    equals(RolloutStrategyAttributeConditional.NotEquals, ['a', 'b'], 'a', false);
    equals(RolloutStrategyAttributeConditional.Excludes, ['a', 'b'], 'a', false);
    equals(RolloutStrategyAttributeConditional.Excludes, ['a', 'b'], 'c', true);
    equals(RolloutStrategyAttributeConditional.Greater, ['a', 'b'], 'a', false);
    equals(RolloutStrategyAttributeConditional.GreaterEquals, ['a', 'b'], 'a', true);
    equals(RolloutStrategyAttributeConditional.Greater, ['a', 'b'], 'c', true);
    equals(RolloutStrategyAttributeConditional.Less, ['a', 'b'], 'a', true); // < b
    equals(RolloutStrategyAttributeConditional.Less, ['a', 'b'], '1', true);
    equals(RolloutStrategyAttributeConditional.Less, ['a', 'b'], 'b', false);
    equals(RolloutStrategyAttributeConditional.Less, ['a', 'b'], 'c', false);
    equals(RolloutStrategyAttributeConditional.LessEquals, ['a', 'b'], 'a', true);
    equals(RolloutStrategyAttributeConditional.LessEquals, ['a', 'b'], 'b', true);
    equals(RolloutStrategyAttributeConditional.LessEquals, ['a', 'b'], '1', true);
    equals(RolloutStrategyAttributeConditional.LessEquals, ['a', 'b'], 'c', false);
    equals(RolloutStrategyAttributeConditional.StartsWith, ['fr'], 'fred', true);
    equals(RolloutStrategyAttributeConditional.StartsWith, ['fr'], 'mar', false);
    equals(RolloutStrategyAttributeConditional.EndsWith, ['ed'], 'fred', true);
    equals(RolloutStrategyAttributeConditional.EndsWith, ['fred'], 'mar', false);
    equals(RolloutStrategyAttributeConditional.Regex, ['(.*)gold(.*)'], 'actapus (gold)', true);
    equals(RolloutStrategyAttributeConditional.Regex, ['(.*)gold(.*)'], '(.*)purple(.*)', false);
  });

  it('semantic version matcher should work as expected', () => {
    type = RolloutStrategyFieldType.SemanticVersion;

    equals(RolloutStrategyAttributeConditional.Equals, ['2.0.3'], '2.0.3', true);
    equals(RolloutStrategyAttributeConditional.Equals, ['2.0.3', '2.0.1'], '2.0.3', true);
    equals(RolloutStrategyAttributeConditional.Equals, ['2.0.3'], '2.0.1', false);
    equals(RolloutStrategyAttributeConditional.NotEquals, ['2.0.3'], '2.0.1', true);
    equals(RolloutStrategyAttributeConditional.NotEquals, ['2.0.3'], '2.0.3', false);
    equals(RolloutStrategyAttributeConditional.Greater, ['2.0.0'], '2.1.0', true);
    equals(RolloutStrategyAttributeConditional.Greater, ['2.0.0'], '2.0.1', true);
    equals(RolloutStrategyAttributeConditional.Greater, ['2.0.0'], '2.0.1', true);
    equals(RolloutStrategyAttributeConditional.Greater, ['2.0.0'], '1.2.1', false);
    equals(RolloutStrategyAttributeConditional.GreaterEquals, ['7.1.0'], '7.1.6', true);
    equals(RolloutStrategyAttributeConditional.GreaterEquals, ['7.1.6'], '7.1.6', true);
    equals(RolloutStrategyAttributeConditional.GreaterEquals, ['7.1.6'], '7.1.2', false);
    equals(RolloutStrategyAttributeConditional.Less, ['2.0.0'], '1.1.0', true);
    equals(RolloutStrategyAttributeConditional.Less, ['2.0.0'], '1.0.1', true);
    equals(RolloutStrategyAttributeConditional.Less, ['2.0.0'], '1.9.9', true);
    equals(RolloutStrategyAttributeConditional.Less, ['2.0.0'], '3.2.1', false);
    equals(RolloutStrategyAttributeConditional.LessEquals, ['7.1.0'], '7.0.6', true);
    equals(RolloutStrategyAttributeConditional.LessEquals, ['7.1.6'], '7.1.2', true);
    equals(RolloutStrategyAttributeConditional.LessEquals, ['7.1.2'], '7.1.6', false);
  });

  it('ip address matcher', () => {
    type = RolloutStrategyFieldType.IpAddress;

    equals(RolloutStrategyAttributeConditional.Equals, ['192.168.86.75'], '192.168.86.75', true);
    equals(RolloutStrategyAttributeConditional.Equals, ['192.168.86.75', '10.7.4.8'], '192.168.86.75', true);
    equals(RolloutStrategyAttributeConditional.Equals, ['192.168.86.75', '10.7.4.8'], '192.168.83.75', false);
    equals(RolloutStrategyAttributeConditional.Excludes, ['192.168.86.75', '10.7.4.8'], '192.168.83.75', true);
    equals(RolloutStrategyAttributeConditional.Excludes, ['192.168.86.75', '10.7.4.8'], '192.168.86.75', false);
    equals(RolloutStrategyAttributeConditional.Includes, ['192.168.86.75', '10.7.4.8'], '192.168.86.75', true);
    equals(RolloutStrategyAttributeConditional.Equals, ['192.168.86.75'], '192.168.86.72', false);
    equals(RolloutStrategyAttributeConditional.NotEquals, ['192.168.86.75'], '192.168.86.75', false);
    equals(RolloutStrategyAttributeConditional.NotEquals, ['192.168.86.75'], '192.168.86.72', true);
    equals(RolloutStrategyAttributeConditional.Equals, ['192.168.0.0/16'], '192.168.86.72', true);
    equals(RolloutStrategyAttributeConditional.Equals, ['192.168.0.0/16'], '192.162.86.72', false);
    equals(RolloutStrategyAttributeConditional.Equals, ['10.0.0.0/24', '192.168.0.0/16'], '192.168.86.72', true);
    equals(RolloutStrategyAttributeConditional.Equals, ['10.0.0.0/24', '192.168.0.0/16'], '172.168.86.72', false);
  });

  it('date matcher', () => {
    type = RolloutStrategyFieldType.Date;

    equals(RolloutStrategyAttributeConditional.Equals, ['2019-01-01', '2019-02-01'], '2019-02-01', true);
    equals(RolloutStrategyAttributeConditional.Equals, [new Date('2019-01-01'), new Date('2019-02-01')],
      '2019-02-01', true);
    equals(RolloutStrategyAttributeConditional.Equals, ['2019-01-01', '2019-02-01'], '2019-02-01', true);
    equals(RolloutStrategyAttributeConditional.Includes, ['2019-01-01', '2019-02-01'], '2019-02-01', true);
    equals(RolloutStrategyAttributeConditional.NotEquals, ['2019-01-01', '2019-02-01'], '2019-02-01', false);
    equals(RolloutStrategyAttributeConditional.Excludes, ['2019-01-01', '2019-02-01'], '2019-02-01', false);

    equals(RolloutStrategyAttributeConditional.Equals, ['2019-01-01', '2019-02-01'], '2019-02-07', false);
    equals(RolloutStrategyAttributeConditional.Includes, ['2019-01-01', '2019-02-01'], '2019-02-07', false);
    equals(RolloutStrategyAttributeConditional.NotEquals, ['2019-01-01', '2019-02-01'], '2019-02-07', true);
    equals(RolloutStrategyAttributeConditional.Excludes, ['2019-01-01', '2019-02-01'], '2019-02-07', true);

    equals(RolloutStrategyAttributeConditional.Greater, ['2019-01-01', '2019-02-01'], '2019-02-07', true);
    equals(RolloutStrategyAttributeConditional.GreaterEquals, ['2019-01-01', '2019-02-01'], '2019-02-07', true);
    equals(RolloutStrategyAttributeConditional.GreaterEquals, ['2019-01-01', '2019-02-01'], '2019-02-01', true);
    equals(RolloutStrategyAttributeConditional.Less, ['2019-01-01', '2019-02-01'], '2017-02-01', true);
    equals(RolloutStrategyAttributeConditional.Less, ['2019-01-01', '2019-02-01'], '2019-02-01', false);
    equals(RolloutStrategyAttributeConditional.LessEquals, ['2019-01-01', '2019-02-01'], '2019-02-01', true);
    equals(RolloutStrategyAttributeConditional.LessEquals, ['2019-01-01', '2019-02-01'], '2019-03-01', false);
    equals(RolloutStrategyAttributeConditional.Regex, ['2019-.*'], '2019-03-01', true);
    equals(RolloutStrategyAttributeConditional.Regex, ['2019-.*'], '2017-03-01', false);
    equals(RolloutStrategyAttributeConditional.Regex, ['2019-.*', '(.*)-03-(.*)'], '2017-03-01', true);

    equals(RolloutStrategyAttributeConditional.StartsWith, ['2019', '2017'], '2019-02-07', true);
    equals(RolloutStrategyAttributeConditional.StartsWith, ['2019'], '2017-02-07', false);

    equals(RolloutStrategyAttributeConditional.EndsWith, ['01'], '2017-02-01', true);
    equals(RolloutStrategyAttributeConditional.EndsWith, ['03', '02', '2017'], '2017-02-01', false);
  });

  it ('number matcher', () => {
    type = RolloutStrategyFieldType.Number;
    equals(RolloutStrategyAttributeConditional.Equals, [10, 5], '5', true);
    equals(RolloutStrategyAttributeConditional.Equals, [5], '5', true);
    equals(RolloutStrategyAttributeConditional.Equals, [4], '5', false);
    equals(RolloutStrategyAttributeConditional.Equals, [4, 7], '5', false);
    equals(RolloutStrategyAttributeConditional.Includes, [4, 7], '5', false);
    equals(RolloutStrategyAttributeConditional.NotEquals, [23, 100923], '5', true);
    equals(RolloutStrategyAttributeConditional.Excludes, [23, 100923], '5', true);
    equals(RolloutStrategyAttributeConditional.NotEquals, [5], '5', false);
    equals(RolloutStrategyAttributeConditional.Greater, [2, 4], '5', true);
    equals(RolloutStrategyAttributeConditional.GreaterEquals, [2, 5], '5', true);
    equals(RolloutStrategyAttributeConditional.GreaterEquals, [4, 5], '5', true);
    equals(RolloutStrategyAttributeConditional.LessEquals, [2, 5], '5', true);
    equals(RolloutStrategyAttributeConditional.Less, [8, 7], '5', true);
    equals(RolloutStrategyAttributeConditional.Greater, [7, 10], '5', false);
    equals(RolloutStrategyAttributeConditional.GreaterEquals, [6, 7], '5', false);
    equals(RolloutStrategyAttributeConditional.LessEquals, [2, 3], '5', false);
    equals(RolloutStrategyAttributeConditional.Less, [1, -1], '5', false);
  });

  it('datetime matcher', () => {
    type = RolloutStrategyFieldType.Datetime;
    // test equals
    equals(RolloutStrategyAttributeConditional.Equals, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2019-02-01T01:01:01Z', true);
    equals(RolloutStrategyAttributeConditional.Includes, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2019-02-01T01:01:01Z', true);
    equals(RolloutStrategyAttributeConditional.NotEquals, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2019-02-01T01:01:01Z', false);
    equals(RolloutStrategyAttributeConditional.Excludes, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2019-02-01T01:01:01Z', false);

    // test not equals
    equals(RolloutStrategyAttributeConditional.Equals, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2017-02-01T01:01:01Z', false);
    equals(RolloutStrategyAttributeConditional.Includes, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2017-02-01T01:01:01Z', false);
    equals(RolloutStrategyAttributeConditional.NotEquals, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2017-02-01T01:01:01Z', true);
    equals(RolloutStrategyAttributeConditional.Excludes, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2017-02-01T01:01:01Z', true);

    // test  less & less =
    equals(RolloutStrategyAttributeConditional.Less, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2016-02-01T01:01:01Z', true);
    equals(RolloutStrategyAttributeConditional.Less, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2020-02-01T01:01:01Z', false);

    equals(RolloutStrategyAttributeConditional.LessEquals, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2019-02-01T01:01:01Z', true);
    equals(RolloutStrategyAttributeConditional.LessEquals, ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z'],
      '2020-02-01T01:01:01Z', false);

    equals(RolloutStrategyAttributeConditional.Regex, ['2019-.*'], '2019-07-06T01:01:01Z', true);
    equals(RolloutStrategyAttributeConditional.Regex, ['2019-.*'], '2016-07-06T01:01:01Z', false);
    equals(RolloutStrategyAttributeConditional.Regex, ['2019-.*', '(.*)-03-(.*)'], '2019-07-06T01:01:01Z', true);
    equals(RolloutStrategyAttributeConditional.Regex, ['2019-.*', '(.*)-03-(.*)'], '2014-03-06T01:01:01Z', true);

    equals(RolloutStrategyAttributeConditional.StartsWith, ['2019', '2017'], '2017-03-06T01:01:01Z', true);
    equals(RolloutStrategyAttributeConditional.StartsWith, ['2019'], '2017-03-06T01:01:01Z', false);
    equals(RolloutStrategyAttributeConditional.EndsWith, [':01Z'], '2017-03-06T01:01:01Z', true);
    equals(RolloutStrategyAttributeConditional.EndsWith, ['03', '2017', '01:01'], '2017-03-06T01:01:01Z', false);
    equals(RolloutStrategyAttributeConditional.EndsWith, ['rubbish'], '2017-03-06T01:01:01Z', false);
  });
});
