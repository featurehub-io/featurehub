import {
  ApplyFeature,
  MatcherRegistry,
  MatcherRepository,
  PercentageCalculator,
  StrategyMatcher
} from '../app/strategy_matcher';
import { Arg, Substitute, SubstituteOf } from '@fluffy-spoon/substitute';
import { ClientContext } from '../app/client_context';
import { expect } from 'chai';
import {
  RolloutStrategy,
  RolloutStrategyAttribute,
  RolloutStrategyAttributeConditional,
  RolloutStrategyFieldType
} from '../app';

describe('apply feature works as expected', () => {
  let pCalc: SubstituteOf<PercentageCalculator>;
  let matcher: SubstituteOf<MatcherRepository>;
  let app: ApplyFeature;

  beforeEach(() => {
    pCalc = Substitute.for<PercentageCalculator>();
    matcher = Substitute.for<MatcherRepository>();

    app = new ApplyFeature(pCalc, matcher);
  });

  it('should always return false when there is an undefined context', () => {
    const found = app.apply([new RolloutStrategy()], 'key', 'fid', undefined);

    // tslint:disable-next-line:no-unused-expression
    expect(found.matched).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(found.value).to.be.null;
    // let ctx = Substitute.for<ClientContext>();
    // ctx.attribute_values()
  });

  it('should be false when the rollout strategies are empty', () => {
    const found = app.apply([], 'key', 'fid', Substitute.for<ClientContext>());

    // tslint:disable-next-line:no-unused-expression
    expect(found.matched).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(found.value).to.be.null;
  });

  it('should be false when the rollout strategies are null', () => {
    const found = app.apply(undefined, 'key', 'fid', Substitute.for<ClientContext>());

    // tslint:disable-next-line:no-unused-expression
    expect(found.matched).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(found.value).to.be.null;
  });

  it('should be false if none of the rollout strategies match the context', () => {
    const ctx = Substitute.for<ClientContext>();
    ctx.defaultPercentageKey().returns('userkey');
    ctx.getAttr('warehouseId', null).returns(null);
    const found = app.apply([new RolloutStrategy({
      attributes: [
        new RolloutStrategyAttribute({
          fieldName: 'warehouseId',
          conditional: RolloutStrategyAttributeConditional.Includes,
          values: ['ponsonby'],
          type: RolloutStrategyFieldType.String
        })]
    })], 'FEATURE_NAME', 'fid', ctx);
    // tslint:disable-next-line:no-unused-expression
    expect(found.matched).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(found.value).to.be.null;
  });

  it('should not match the percentage but should match the field comparison', () => {
    const ctx = Substitute.for<ClientContext>();
    ctx.defaultPercentageKey().returns('userkey');
    ctx.getAttr('warehouseId', null).returns('ponsonby');
    const found = app.apply([new RolloutStrategy({
      value: 'sausage',
      attributes: [
        new RolloutStrategyAttribute({
          fieldName: 'warehouseId',
          conditional: RolloutStrategyAttributeConditional.Includes,
          values: ['ponsonby'],
          type: RolloutStrategyFieldType.String
        })]
    })], 'FEATURE_NAME', 'fid', ctx);

    // tslint:disable-next-line:no-unused-expression
    expect(found.matched).to.be.true;
    // tslint:disable-next-line:no-unused-expression
    expect(found.value).to.be.eq('sausage');
  });

  it('should not match the field comparison if the value is different', () => {
    const ctx = Substitute.for<ClientContext>();

    ctx.defaultPercentageKey().returns('userkey');
    ctx.getAttr('warehouseId', null).returns('ponsonby');

    const sMatcher = Substitute.for<StrategyMatcher>();
    sMatcher.match('ponsonby', Arg.any()).returns(false);

    matcher.findMatcher(Arg.any()).returns(sMatcher);

    const found = app.apply([new RolloutStrategy({
      value: 'sausage',
      attributes: [
        new RolloutStrategyAttribute({
          fieldName: 'warehouseId',
          conditional: RolloutStrategyAttributeConditional.Includes,
          values: ['ponsonby'],
          type: RolloutStrategyFieldType.String
        })]
    })], 'FEATURE_NAME', 'fid', ctx);

    // tslint:disable-next-line:no-unused-expression
    expect(found.matched).to.be.false;
    // tslint:disable-next-line:no-unused-expression
    expect(found.value).to.be.null;
  });

  it('should process basic percentages properly', () => {
    const ctx = Substitute.for<ClientContext>();

    ctx.defaultPercentageKey().returns('userkey');
    pCalc.determineClientPercentage('userkey', 'fid').returns(15);

    const sApp = new ApplyFeature(pCalc, new MatcherRegistry());

    const found = sApp.apply([new RolloutStrategy({
      value: 'sausage',
      percentage: 20,
    })], 'FEATURE_NAME', 'fid', ctx);

    // tslint:disable-next-line:no-unused-expression
    expect(found.matched).to.be.true;
    // tslint:disable-next-line:no-unused-expression
    expect(found.value).to.eq('sausage');
  });

  it('should bounce bad percentages properly', () => {
    const ctx = Substitute.for<ClientContext>();

    ctx.defaultPercentageKey().returns('userkey');
    pCalc.determineClientPercentage('userkey', 'fid').returns(21);

    const sApp = new ApplyFeature(pCalc, new MatcherRegistry());

    const found = sApp.apply([new RolloutStrategy({
      value: 'sausage',
      percentage: 20,
    })], 'FEATURE_NAME', 'fid', ctx);

    // tslint:disable-next-line:no-unused-expression
    expect(found.matched).to.be.false;
  });


  it('should process pattern match percentages properly', () => {
    const ctx = Substitute.for<ClientContext>();

    ctx.defaultPercentageKey().returns('userkey');
    ctx.getAttr('warehouseId', null).returns('ponsonby');
    pCalc.determineClientPercentage('userkey', 'fid').returns(15);

    const sApp = new ApplyFeature(pCalc, new MatcherRegistry());

    const found = sApp.apply([new RolloutStrategy({
      value: 'sausage',
      percentage: 20,
      attributes: [
        new RolloutStrategyAttribute({
          fieldName: 'warehouseId',
          conditional: RolloutStrategyAttributeConditional.Includes,
          values: ['ponsonby'],
          type: RolloutStrategyFieldType.String
        })]
    })], 'FEATURE_NAME', 'fid', ctx);

    // tslint:disable-next-line:no-unused-expression
    expect(found.matched).to.be.true;
    // tslint:disable-next-line:no-unused-expression
    expect(found.value).to.eq('sausage');
  });

  it('should fail pattern match percentages properly', () => {
    const ctx = Substitute.for<ClientContext>();

    ctx.defaultPercentageKey().returns('userkey');
    ctx.getAttr('warehouseId', null).returns(null);
    pCalc.determineClientPercentage('userkey', 'fid').returns(15);

    const sApp = new ApplyFeature(pCalc, new MatcherRegistry());

    const found = sApp.apply([new RolloutStrategy({
      value: 'sausage',
      percentage: 20,
      attributes: [
        new RolloutStrategyAttribute({
          fieldName: 'warehouseId',
          conditional: RolloutStrategyAttributeConditional.Includes,
          values: ['ponsonby'],
          type: RolloutStrategyFieldType.String
        })]
    })], 'FEATURE_NAME', 'fid', ctx);

    // tslint:disable-next-line:no-unused-expression
    expect(found.matched).to.be.false;
  });

});
