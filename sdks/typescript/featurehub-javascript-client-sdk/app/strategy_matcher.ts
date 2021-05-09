import {
  RolloutStrategy,
  RolloutStrategyAttribute,
  RolloutStrategyAttributeConditional,
  RolloutStrategyFieldType
} from './models';


import { eq, gt, gte, lt, lte } from 'semver';
// this library was node specific so required de-noding
import { Addr, CIDR } from './ip6addr';
// this library is not node specific
import { v3 as murmur3 } from 'murmurhash';
import { ClientContext } from './client_context';

export interface PercentageCalculator {
  determineClientPercentage(percentageText: string, featureId: string): number;
}

export class Murmur3PercentageCalculator implements PercentageCalculator {
  private readonly MAX_PERCENTAGE = 1000000;

  public determineClientPercentage(percentageText: string, featureId: string): number {
    const result = murmur3(percentageText + featureId);
    return Math.floor(result / Math.pow(2, 32) * this.MAX_PERCENTAGE);
  }
}

export class Applied {
  public readonly matched: boolean;
  public readonly value: any;

  constructor(matched: boolean, value: any) {
    this.matched = matched;
    this.value = value;
  }
}

export class ApplyFeature {
  private readonly _percentageCalculator: PercentageCalculator;
  private readonly _matcherRepository: MatcherRepository;

  constructor(percentageCalculator?: PercentageCalculator, matcherRepository?: MatcherRepository) {
    this._percentageCalculator = percentageCalculator || new Murmur3PercentageCalculator();
    this._matcherRepository = matcherRepository || new MatcherRegistry();
  }

  public apply(strategies: Array<RolloutStrategy>, key: string, featureValueId: string,
               context: ClientContext): Applied {
    if (context != null && strategies != null && strategies.length > 0) {
      let percentage: number = null;
      let percentageKey: string = null;
      let basePercentage = new Map<string, number>();
      const defaultPercentageKey = context.defaultPercentageKey();

      for (let rsi of strategies) {
        if (rsi.percentage !== 0 && (defaultPercentageKey != null ||
          (rsi.percentageAttributes !== undefined && rsi.percentageAttributes.length > 0))) {
          let newPercentageKey = this.determinePercentageKey(context, rsi.percentageAttributes);

          if (!basePercentage.has(newPercentageKey)) {
            basePercentage.set(newPercentageKey, 0);
          }

          let basePercentageVal = basePercentage.get(newPercentageKey);

          // if we have changed the key or we have never calculated it, calculate it and set the
          // base percentage to null
          if (percentage === null || newPercentageKey !== percentageKey) {
            percentageKey = newPercentageKey;
            percentage = this._percentageCalculator.determineClientPercentage(percentageKey, featureValueId);
          }

          let useBasePercentage = (rsi.attributes === undefined || rsi.attributes.length === 0) ? basePercentageVal : 0;

          // if the percentage is lower than the user's key +
          // id of feature value then apply it
          if (percentage <= (useBasePercentage + rsi.percentage)) {
            if (rsi.attributes != null && rsi.attributes.length > 0) {
              if (this.matchAttribute(context, rsi)) {
                return new Applied(true, rsi.value);
              }
            } else {
              return new Applied(true, rsi.value);
            }
          }

          // this was only a percentage and had no other attributes
          if (rsi.attributes !== undefined && rsi.attributes.length > 0) {
            basePercentage.set(percentageKey, basePercentage.get(percentageKey) + rsi.percentage);
          }
        }

        if ((rsi.percentage === 0 || rsi.percentage === undefined) && rsi.attributes !== undefined
              && rsi.attributes.length > 0 &&
          this.matchAttribute(context, rsi)) { // nothing to do with a percentage
          return new Applied(true, rsi.value);
        }
      }
    }

    return new Applied(false, null);
  }

  private determinePercentageKey(context: ClientContext, percentageAttributes: Array<string>): string {
    if (percentageAttributes == null || percentageAttributes.length === 0) {
      return context.defaultPercentageKey();
    }

    return percentageAttributes.filter((pa) => context.getAttr(pa, '<none>')).join('$');
  }

  private matchAttribute(context: ClientContext, rsi: RolloutStrategy): boolean {
    for (let attr of rsi.attributes) {
      let suppliedValue = context.getAttr(attr.fieldName, null);
      if (suppliedValue === null && attr.fieldName.toLowerCase() === 'now') {
        // tslint:disable-next-line:switch-default
        switch (attr.type) {
          case RolloutStrategyFieldType.Date:
            suppliedValue = new Date().toISOString().substring(0, 10);
            break;
          case RolloutStrategyFieldType.Datetime:
            suppliedValue = new Date().toISOString();
            break;
        }
      }

      if (attr.values == null && suppliedValue == null) {
        if (attr.conditional !== RolloutStrategyAttributeConditional.Equals) {
          return false;
        }

        continue; // skip
      }

      if (attr.values == null || suppliedValue == null) {
        return false;
      }

      if (!this._matcherRepository.findMatcher(attr).match(suppliedValue, attr)) {
        return false;
      }
    }

    return true;
  }
}

export interface StrategyMatcher {
  match(suppliedValue: string, attr: RolloutStrategyAttribute): boolean;
}

export interface MatcherRepository {
  findMatcher(attr: RolloutStrategyAttribute): StrategyMatcher;
}

export class MatcherRegistry implements MatcherRepository {
  findMatcher(attr: RolloutStrategyAttribute): StrategyMatcher {
    // tslint:disable-next-line:switch-default
    switch (attr?.type) {
      case RolloutStrategyFieldType.String:
        return new StringMatcher();
      case RolloutStrategyFieldType.SemanticVersion:
        return new SemanticVersionMatcher();
      case RolloutStrategyFieldType.Number:
        return new NumberMatcher();
      case RolloutStrategyFieldType.Date:
        return new DateMatcher();
      case RolloutStrategyFieldType.Datetime:
        return new DateTimeMatcher();
      case RolloutStrategyFieldType.Boolean:
        return new BooleanMatcher();
      case RolloutStrategyFieldType.IpAddress:
        return new IPNetworkMatcher();
    }

    return new FallthroughMatcher();
  }

}

class FallthroughMatcher implements StrategyMatcher {
  match(suppliedValue: string, attr: RolloutStrategyAttribute): boolean {
    return false;
  }
}

class BooleanMatcher implements StrategyMatcher {
  match(suppliedValue: string, attr: RolloutStrategyAttribute): boolean {
    const val = 'true' === suppliedValue;

    if (attr.conditional === RolloutStrategyAttributeConditional.Equals) {
      return val === (attr.values[0].toString() === 'true');
    }

    if (attr.conditional === RolloutStrategyAttributeConditional.NotEquals) {
      return val !== (attr.values[0].toString() === 'true');
    }

    return false;
  }
}

class StringMatcher implements StrategyMatcher {

  match(suppliedValue: string, attr: RolloutStrategyAttribute): boolean {
    const vals = this.attrToStringValues(attr);

    // tslint:disable-next-line:switch-default
    switch (attr.conditional) {
      case RolloutStrategyAttributeConditional.Equals:
        return vals.findIndex((v) => v === suppliedValue) >= 0;
      case RolloutStrategyAttributeConditional.EndsWith:
        return vals.findIndex((v) => suppliedValue.endsWith(v)) >= 0;
      case RolloutStrategyAttributeConditional.StartsWith:
        return vals.findIndex((v) => suppliedValue.startsWith(v)) >= 0;
      case RolloutStrategyAttributeConditional.Greater:
        return vals.findIndex((v) => suppliedValue > v) >= 0;
      case RolloutStrategyAttributeConditional.GreaterEquals:
        return vals.findIndex((v) => suppliedValue >= v) >= 0;
      case RolloutStrategyAttributeConditional.Less:
        return vals.findIndex((v) => suppliedValue < v) >= 0;
      case RolloutStrategyAttributeConditional.LessEquals:
        return vals.findIndex((v) => suppliedValue <= v) >= 0;
      case RolloutStrategyAttributeConditional.NotEquals:
        return vals.findIndex((v) => v === suppliedValue) === -1;
      case RolloutStrategyAttributeConditional.Includes:
        return vals.findIndex((v) => suppliedValue.includes(v)) >= 0;
      case RolloutStrategyAttributeConditional.Excludes:
        return vals.findIndex((v) => suppliedValue.includes(v)) === -1;
      case RolloutStrategyAttributeConditional.Regex:
        return vals.findIndex((v) => suppliedValue.match(v)) >= 0;
    }

    return false;
  }

  protected attrToStringValues(attr: RolloutStrategyAttribute): Array<string> {
    return attr.values.filter((v) => v != null).map((v) => v.toString());
  }
}

class DateMatcher extends StringMatcher {
  match(suppliedValue: string, attr: RolloutStrategyAttribute): boolean {
    try {
      const parsedDate = new Date(suppliedValue);

      if (parsedDate == null) {
        return false;
      }

      return super.match(parsedDate.toISOString().substring(0, 10), attr);
    } catch (e) {
      return false;
    }
  }

  protected attrToStringValues(attr: RolloutStrategyAttribute): Array<string> {
    return attr.values.filter((v) => v != null)
      .map((v) => (v instanceof Date) ? v.toISOString().substring(0, 10) : v.toString());
  }
}

class DateTimeMatcher extends StringMatcher {
  match(suppliedValue: string, attr: RolloutStrategyAttribute): boolean {
    try {
      const parsedDate = new Date(suppliedValue);

      if (parsedDate == null) {
        return false;
      }

      return super.match(parsedDate.toISOString().substr(0, 19) + 'Z', attr);
    } catch (e) {
      return false;
    }
  }

  protected attrToStringValues(attr: RolloutStrategyAttribute): Array<string> {
    return attr.values.filter((v) => v != null)
      .map((v) => (v instanceof Date) ? (v.toISOString().substr(0, 19) + 'Z') : v.toString());
  }
}

class NumberMatcher implements StrategyMatcher {
  match(suppliedValue: string, attr: RolloutStrategyAttribute): boolean {
    try {
      const isFloat = suppliedValue.indexOf('.') >= 0;
      const num = isFloat ? parseFloat(suppliedValue) : parseInt(suppliedValue, 10);
      const conv = (v) => isFloat ? parseFloat(v) : parseInt(v, 10);

      const vals = attr.values.filter((v) => v != null).map((v) => v.toString());

      // tslint:disable-next-line:switch-default
      switch (attr.conditional) {
        case RolloutStrategyAttributeConditional.Equals:
          return vals.findIndex((v) => conv(v) === num) >= 0;
        case RolloutStrategyAttributeConditional.EndsWith:
          return vals.findIndex((v) => suppliedValue.endsWith(v)) >= 0;
        case RolloutStrategyAttributeConditional.StartsWith:
          return vals.findIndex((v) => suppliedValue.startsWith(v)) >= 0;
        case RolloutStrategyAttributeConditional.Greater:
          return vals.findIndex((v) => num > conv(v)) >= 0;
        case RolloutStrategyAttributeConditional.GreaterEquals:
          return vals.findIndex((v) => num >= conv(v)) >= 0;
        case RolloutStrategyAttributeConditional.Less:
          return vals.findIndex((v) => num < conv(v)) >= 0;
        case RolloutStrategyAttributeConditional.LessEquals:
          return vals.findIndex((v) => num <= conv(v)) >= 0;
        case RolloutStrategyAttributeConditional.NotEquals:
          return vals.findIndex((v) => conv(v) === num) === -1;
        case RolloutStrategyAttributeConditional.Includes:
          return vals.findIndex((v) => suppliedValue.includes(v)) >= 0;
        case RolloutStrategyAttributeConditional.Excludes:
          return vals.findIndex((v) => suppliedValue.includes(v)) === -1;
        case RolloutStrategyAttributeConditional.Regex:
          return vals.findIndex((v) => suppliedValue.match(v)) >= 0;
      }
    } catch (e) {
      return false;
    }

    return false;
  }
}

class SemanticVersionMatcher implements StrategyMatcher {
  match(suppliedValue: string, attr: RolloutStrategyAttribute): boolean {
    const vals = attr.values.filter((v) => v != null).map((v) => v.toString());

    // tslint:disable-next-line:switch-default
    switch (attr.conditional) {
      case RolloutStrategyAttributeConditional.Includes:
      case RolloutStrategyAttributeConditional.Equals:
        return vals.findIndex((v) => eq(suppliedValue, v)) >= 0;
      case RolloutStrategyAttributeConditional.EndsWith:
        break;
      case RolloutStrategyAttributeConditional.StartsWith:
        break;
      case RolloutStrategyAttributeConditional.Greater:
        return vals.findIndex((v) => gt(suppliedValue, v)) >= 0;
      case RolloutStrategyAttributeConditional.GreaterEquals:
        return vals.findIndex((v) => gte(suppliedValue, v)) >= 0;
      case RolloutStrategyAttributeConditional.Less:
        return vals.findIndex((v) => lt(suppliedValue, v)) >= 0;
      case RolloutStrategyAttributeConditional.LessEquals:
        return vals.findIndex((v) => lte(suppliedValue, v)) >= 0;
      case RolloutStrategyAttributeConditional.NotEquals:
      case RolloutStrategyAttributeConditional.Excludes:
        return vals.findIndex((v) => !eq(suppliedValue, v)) >= 0;
      case RolloutStrategyAttributeConditional.Regex:
        break;
    }

    return false;
  }
}

class IPNetworkMatcher implements StrategyMatcher {
  match(suppliedValue: string, attr: RolloutStrategyAttribute): boolean {
    const ip = new IPNetworkProxy(suppliedValue);
    const vals = attr.values.filter((v) => v != null).map((v) => new IPNetworkProxy(v.toString()));

    // tslint:disable-next-line:switch-default
    switch (attr.conditional) {
      case RolloutStrategyAttributeConditional.Equals:
      case RolloutStrategyAttributeConditional.Includes:
        return vals.findIndex((v) => v.contains(ip)) >= 0;
      case RolloutStrategyAttributeConditional.NotEquals:
      case RolloutStrategyAttributeConditional.Excludes:
        return vals.findIndex((v) => v.contains(ip)) === -1;
    }

    return false;
  }
}

class IPNetworkProxy {
  private readonly _address: Addr;
  private readonly _network: CIDR;
  private readonly _isAddress: boolean;
  private readonly _original: string;

  constructor(addr: string) {
    this._original = addr;

    if (addr.includes('/')) {
      this._isAddress = false;
      this._network = new CIDR(addr);
    } else {
      this._isAddress = true;
      this._address = Addr.toAddr(addr);
    }
  }

  public contains(proxy: IPNetworkProxy): boolean {
    if (proxy._isAddress && this._isAddress) {
      return proxy._address.compare(this._address) === 0;
    }

    if (!proxy._isAddress && !this._isAddress) {
      return this._network.compare(proxy._network) === 0;
    }

    if (proxy._isAddress && !this._isAddress) {
      return this._network.contains(proxy._original);
    }
  }
}
