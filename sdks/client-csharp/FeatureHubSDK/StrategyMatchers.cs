

using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using FeatureHubSDK;
using IO.FeatureHub.SSE.Model;
using Murmur;
using NodaTime;
using NodaTime.Text;
using Version = SemVer.Version;

namespace FeatureHubSDK
{
  public interface IPercentageCalculator
  {
    int DetermineClientPercentage(string percentageText, string featureId);
  }

  public class PercentageMurmur3Calculator : IPercentageCalculator, IDisposable
  {
    public static int MAX_PERCENTAGE = 1000000;
    private readonly HashAlgorithm _hashAlgorithm;

    public PercentageMurmur3Calculator()
    {
      _hashAlgorithm = MurmurHash.Create32();
    }

    public PercentageMurmur3Calculator(uint seed)
    {
      _hashAlgorithm = MurmurHash.Create32(seed: seed);
    }

    public int DetermineClientPercentage(string percentageText, string featureId)
    {
      var hashCode = _hashAlgorithm.ComputeHash(Encoding.UTF8.GetBytes(percentageText + featureId));
      var result = BitConverter.ToInt32(hashCode, 0);
      var ratio = (result & 0xFFFFFFFFL) / Math.Pow(2, 32);
      return (int) Math.Floor(MAX_PERCENTAGE * ratio);
    }

    public void Dispose()
    {
      _hashAlgorithm?.Dispose();
    }
  }

  public class Applied
  {
    public bool Matched { get; }
    public object Value { get; }

    public Applied(bool matched, object value)
    {
      this.Matched = matched;
      this.Value = value;
    }
  }

  public class ApplyFeature
  {
    private readonly IPercentageCalculator _percentageCalculator;
    private readonly IMatcherRepository _matcherRepository;

    public ApplyFeature(IPercentageCalculator percentageCalculator, IMatcherRepository matcherRepository)
    {
      _percentageCalculator = percentageCalculator;
      _matcherRepository = matcherRepository;
    }

    public Applied Apply(List<RolloutStrategy> strategies, string key, string featureValueId,
      IClientContext context)
    {
      if (context != null && strategies != null && strategies.Count != 0)
      {
        int? percentage = null;
        string percentageKey = null;
        var basePercentage = new Dictionary<string, int>();
        var defaultPercentageKey = context.DefaultPercentageKey;

        foreach (var rsi in strategies)
        {
          if (rsi.Percentage != 0 && (defaultPercentageKey != null || rsi.PercentageAttributes.Count > 0))
          {
            // determine what the percentage key is
            var newPercentageKey = DeterminePercentageKey(context, rsi.PercentageAttributes);

            if (!basePercentage.ContainsKey(newPercentageKey))
            {
              basePercentage[newPercentageKey] = 0;
            }

            var basePercentageVal = basePercentage[newPercentageKey];

            // if we have changed the key or we have never calculated it, calculate it and set the
            // base percentage to null
            if (percentage == null || !newPercentageKey.Equals(percentageKey))
            {
              percentageKey = newPercentageKey;
              percentage = _percentageCalculator.DetermineClientPercentage(percentageKey, featureValueId);
            }

            int useBasePercentage = (rsi.Attributes == null || rsi.Attributes.Count == 0) ? basePercentageVal : 0;
            // if the percentage is lower than the user's key +
            // id of feature value then apply it
            if (percentage <= (useBasePercentage + rsi.Percentage))
            {
              if (rsi.Attributes != null && rsi.Attributes.Count != 0)
              {
                if (MatchAttributes(context, rsi))
                {
                  return new Applied(true, rsi.Value);
                }
              }
              else
              {
                return new Applied(true, rsi.Value);
              }
            }

              // this was only a percentage and had no other attributes
            if (rsi.Attributes == null || rsi.Attributes.Count == 0)
            {
              basePercentage[percentageKey] = basePercentage[percentageKey] + rsi.Percentage;
            }
          }

          if (rsi.Percentage == 0 && rsi.Attributes != null && rsi.Attributes.Count > 0)
          {
            if (MatchAttributes(context, rsi))
            {
              return new Applied(true, rsi.Value);
            }
          }
        }

      }

      return new Applied(false, null);
    }

    protected bool MatchAttributes(IClientContext context, RolloutStrategy rsi)
    {
      foreach (var attr in rsi.Attributes)
      {
        var suppliedValue = context.GetAttr(attr.FieldName, null);

        if (suppliedValue == null && attr.FieldName.ToLower().Equals("now"))
        {
          switch (attr.Type)
          {
            case RolloutStrategyFieldType.DATE:
              suppliedValue = LocalDatePattern.Iso.Format(LocalDate.FromDateTime(DateTime.Now));
              break;
            case RolloutStrategyFieldType.DATETIME:
              suppliedValue = LocalDateTimePattern.GeneralIso.Format(LocalDateTime.FromDateTime(DateTime.Now));
              break;
          }
        }

        object val = attr.Values;

        if (val == null && suppliedValue == null)
        {
          if (attr.Conditional != RolloutStrategyAttributeConditional.EQUALS)
          {
            return false;
          }

          continue; //skip
        }

        if (val == null || suppliedValue == null)
        {
          return false;
        }

        if (!_matcherRepository.FindMatcher(attr).Match(suppliedValue, attr))
        {
          return false;
        }
      }

      return true;
    }

    private string DeterminePercentageKey(IClientContext context, List<string> rsiPercentageAttributes)
    {
      if (rsiPercentageAttributes == null || rsiPercentageAttributes.Count == 0)
      {
        return context.DefaultPercentageKey;
      }

      return string.Join("$", rsiPercentageAttributes.Select(pa => context.GetAttr(pa, "<none>")));
    }
  }

  public interface IStrategyMatcher
  {
    bool Match(string suppliedValue, RolloutStrategyAttribute attr);
  }

  public interface IMatcherRepository
  {
    IStrategyMatcher FindMatcher(RolloutStrategyAttribute attr);
  }

  public class MatcherRegistry : IMatcherRepository
  {
    public IStrategyMatcher FindMatcher(RolloutStrategyAttribute attr)
    {
      switch (attr.Type)
      {
        case RolloutStrategyFieldType.STRING:
          return new StringMatcher();
        case RolloutStrategyFieldType.SEMANTICVERSION:
          return new SemanticVersionMatcher();
        case RolloutStrategyFieldType.NUMBER:
          return new NumberMatcher();
        case RolloutStrategyFieldType.DATE:
          return new DateMatcher();
        case RolloutStrategyFieldType.DATETIME:
          return new DateTimeMatcher();
        case RolloutStrategyFieldType.BOOLEAN:
          return new BooleanMatcher();
        case RolloutStrategyFieldType.IPADDRESS:
          return new IPNetworkMatcher();
        case null:
          return new FallthroughMatcher();
        default:
          return new FallthroughMatcher();
      }
    }

    private class FallthroughMatcher : IStrategyMatcher
    {
      public bool Match(string suppliedValue, RolloutStrategyAttribute attr)
      {
        return false;
      }
    }
  }

  internal class BooleanMatcher : IStrategyMatcher
  {
    public bool Match(string suppliedValue, RolloutStrategyAttribute attr)
    {
      var val = "true".Equals(suppliedValue);

      if (suppliedValue == null)
      {
        return false;
      }

      if (attr.Conditional == RolloutStrategyAttributeConditional.EQUALS)
      {
        return val == (attr.Values[0] is bool ? ((bool) attr.Values[0]) : bool.Parse(attr.Values[0].ToString()));
      }

      if (attr.Conditional == RolloutStrategyAttributeConditional.NOTEQUALS)
      {
        return val != (attr.Values[0] is bool ? ((bool) attr.Values[0]) : bool.Parse(attr.Values[0].ToString()));
      }

      return false;
    }
  }

  internal class StringMatcher : IStrategyMatcher
  {
    public bool Match(string suppliedValue, RolloutStrategyAttribute attr)
    {
      var vals = attr.Values.Where(v => v != null).Select(v => v.ToString()).ToList();

      switch (attr.Conditional)
      {
        case RolloutStrategyAttributeConditional.EQUALS:
          return vals.Any(v => v.Equals(suppliedValue));
        case RolloutStrategyAttributeConditional.ENDSWITH:
          return vals.Any(suppliedValue.EndsWith);
        case RolloutStrategyAttributeConditional.STARTSWITH:
          return vals.Any(suppliedValue.StartsWith);
        case RolloutStrategyAttributeConditional.GREATER:
          return vals.Any(v => string.Compare(suppliedValue, v, StringComparison.Ordinal) > 0);
        case RolloutStrategyAttributeConditional.GREATEREQUALS:
          return vals.Any(v => string.Compare(suppliedValue, v, StringComparison.Ordinal) >= 0);
        case RolloutStrategyAttributeConditional.LESS:
          return vals.Any(v => string.Compare(suppliedValue, v, StringComparison.Ordinal) < 0);
        case RolloutStrategyAttributeConditional.LESSEQUALS:
          return vals.Any(v => string.Compare(suppliedValue, v, StringComparison.Ordinal) <= 0);
        case RolloutStrategyAttributeConditional.NOTEQUALS:
          return !vals.Any(v => v.Equals(suppliedValue));
        case RolloutStrategyAttributeConditional.INCLUDES:
          return vals.Any(suppliedValue.Contains);
        case RolloutStrategyAttributeConditional.EXCLUDES:
          return !vals.Any(suppliedValue.Contains);
        case RolloutStrategyAttributeConditional.REGEX:
          return vals.Any(v => Regex.IsMatch(suppliedValue, v));
        case null:
          return false;
        default:
          return false;
      }
    }
  }

  internal class DateMatcher : IStrategyMatcher
  {
    public bool Match(string suppliedValue, RolloutStrategyAttribute attr)
    {
      var suppliedDate = LocalDate.FromDateTime(DateTime.Parse(suppliedValue));
      var vals = attr.Values.Where(v => v != null).Select(v => v.ToString()).ToList();

      switch (attr.Conditional)
      {
        case RolloutStrategyAttributeConditional.EQUALS:
        case RolloutStrategyAttributeConditional.INCLUDES:
          return vals.Any(v => suppliedDate.Equals(LocalDate.FromDateTime(DateTime.Parse(v))));
        case RolloutStrategyAttributeConditional.ENDSWITH:
          return vals.Any(suppliedValue.EndsWith);
        case RolloutStrategyAttributeConditional.STARTSWITH:
          return vals.Any(suppliedValue.StartsWith);
        case RolloutStrategyAttributeConditional.GREATER:
          return vals.Any(v => suppliedDate.CompareTo(LocalDate.FromDateTime(DateTime.Parse(v))) > 0);
        case RolloutStrategyAttributeConditional.GREATEREQUALS:
          return vals.Any(v => suppliedDate.CompareTo(LocalDate.FromDateTime(DateTime.Parse(v))) >= 0);
        case RolloutStrategyAttributeConditional.LESS:
          return vals.Any(v => suppliedDate.CompareTo(LocalDate.FromDateTime(DateTime.Parse(v))) < 0);
        case RolloutStrategyAttributeConditional.LESSEQUALS:
          return vals.Any(v => suppliedDate.CompareTo(LocalDate.FromDateTime(DateTime.Parse(v))) <= 0);
        case RolloutStrategyAttributeConditional.EXCLUDES:
        case RolloutStrategyAttributeConditional.NOTEQUALS:
          return !vals.Any(v => suppliedDate.Equals(LocalDate.FromDateTime(DateTime.Parse(v.ToString()))));
        case RolloutStrategyAttributeConditional.REGEX:
          return vals.Any(v => Regex.IsMatch(suppliedValue, v));
        case null:
          return false;
        default:
          return false;
      }
    }
  }

  internal class DateTimeMatcher : IStrategyMatcher
  {
    public bool Match(string suppliedValue, RolloutStrategyAttribute attr)
    {
      var suppliedDate = LocalDateTime.FromDateTime(DateTime.Parse(suppliedValue));
      var vals = attr.Values.Where(v => v != null).Select(v => v.ToString()).ToList();

      switch (attr.Conditional)
      {
        case RolloutStrategyAttributeConditional.EQUALS:
        case RolloutStrategyAttributeConditional.INCLUDES:
          return vals.Any(v => suppliedDate.Equals(LocalDateTime.FromDateTime(DateTime.Parse(v))));
        case RolloutStrategyAttributeConditional.ENDSWITH:
          return vals.Any(suppliedValue.EndsWith);
        case RolloutStrategyAttributeConditional.STARTSWITH:
          return vals.Any(suppliedValue.StartsWith);
        case RolloutStrategyAttributeConditional.GREATER:
          return vals.Any(v => suppliedDate.CompareTo(LocalDateTime.FromDateTime(DateTime.Parse(v))) > 0);
        case RolloutStrategyAttributeConditional.GREATEREQUALS:
          return vals.Any(v => suppliedDate.CompareTo(LocalDateTime.FromDateTime(DateTime.Parse(v))) >= 0);
        case RolloutStrategyAttributeConditional.LESS:
          return vals.Any(v => suppliedDate.CompareTo(LocalDateTime.FromDateTime(DateTime.Parse(v))) < 0);
        case RolloutStrategyAttributeConditional.LESSEQUALS:
          return vals.Any(v => suppliedDate.CompareTo(LocalDateTime.FromDateTime(DateTime.Parse(v))) <= 0);
        case RolloutStrategyAttributeConditional.EXCLUDES:
        case RolloutStrategyAttributeConditional.NOTEQUALS:
          return !vals.Any(v => suppliedDate.Equals(LocalDateTime.FromDateTime(DateTime.Parse(v.ToString()))));
        case RolloutStrategyAttributeConditional.REGEX:
          return vals.Any(v => Regex.IsMatch(suppliedValue, v));
        case null:
          return false;
        default:
          return false;
      }
    }
  }

  internal class NumberMatcher : IStrategyMatcher
  {
    private RolloutStrategyAttribute _attr;

    private IEnumerable<decimal> DVals =>
      _attr.Values.Where(v => v != null).Select(v => decimal.Parse(v.ToString())).ToList();

    public bool Match(string suppliedValue, RolloutStrategyAttribute attr)
    {
      this._attr = attr;
      var dec = decimal.Parse(suppliedValue);

      switch (attr.Conditional)
      {
        case RolloutStrategyAttributeConditional.EQUALS:
        case RolloutStrategyAttributeConditional.INCLUDES:
          return DVals.Any(v => dec.Equals(v));
        case RolloutStrategyAttributeConditional.ENDSWITH:
          return attr.Values.Where(v => v != null).Any(v => suppliedValue.EndsWith(v.ToString()));
        case RolloutStrategyAttributeConditional.STARTSWITH:
          return attr.Values.Where(v => v != null).Any(v => suppliedValue.StartsWith(v.ToString()));
        case RolloutStrategyAttributeConditional.GREATER:
          return DVals.Any(v => dec.CompareTo(v) > 0);
        case RolloutStrategyAttributeConditional.GREATEREQUALS:
          return DVals.Any(v => dec.CompareTo(v) >= 0);
        case RolloutStrategyAttributeConditional.LESS:
          return DVals.Any(v => dec.CompareTo(v) < 0);
        case RolloutStrategyAttributeConditional.LESSEQUALS:
          return DVals.Any(v => dec.CompareTo(v) <= 0);
        case RolloutStrategyAttributeConditional.NOTEQUALS:
        case RolloutStrategyAttributeConditional.EXCLUDES:
          return !DVals.Any(v => dec.Equals(v));
        case RolloutStrategyAttributeConditional.REGEX:
          break;
        case null:
          return false;
        default:
          return false;
      }

      return false;
    }
  }

  internal class SemanticVersionMatcher : IStrategyMatcher
  {
    public bool Match(string suppliedValue, RolloutStrategyAttribute attr)
    {
      var vals = attr.Values.Where(v => v != null).Select(v => new Version(v.ToString())).ToList();
      var version = new Version(suppliedValue);

      switch (attr.Conditional)
      {
        case RolloutStrategyAttributeConditional.EQUALS:
        case RolloutStrategyAttributeConditional.INCLUDES:
          return vals.Any(v => version.Equals(v));
        case RolloutStrategyAttributeConditional.ENDSWITH:
          break;
        case RolloutStrategyAttributeConditional.STARTSWITH:
          break;
        case RolloutStrategyAttributeConditional.GREATER:
          return vals.Any(v => version.CompareTo(v) > 0);
        case RolloutStrategyAttributeConditional.GREATEREQUALS:
          return vals.Any(v => version.CompareTo(v) >= 0);
        case RolloutStrategyAttributeConditional.LESS:
          return vals.Any(v => version.CompareTo(v) < 0);
        case RolloutStrategyAttributeConditional.LESSEQUALS:
          return vals.Any(v => version.CompareTo(v) <= 0);
        case RolloutStrategyAttributeConditional.NOTEQUALS:
        case RolloutStrategyAttributeConditional.EXCLUDES:
          return !vals.Any(v => version.Equals(v));
        case RolloutStrategyAttributeConditional.REGEX:
          break;
        case null:
          return false;
        default:
          return false;
      }

      return false;
    }
  }

  internal class IPNetworkMatcher : IStrategyMatcher
  {
    public bool Match(string suppliedValue, RolloutStrategyAttribute attr)
    {
      var vals = attr.Values.Where(v => v != null).Select(v => new IPNetworkProxy(v.ToString())).ToList();
      var ip = new IPNetworkProxy(suppliedValue);

      switch (attr.Conditional)
      {
        case RolloutStrategyAttributeConditional.EQUALS:
        case RolloutStrategyAttributeConditional.INCLUDES:
          return vals.Any(v => v.Contains(ip));
        case RolloutStrategyAttributeConditional.NOTEQUALS:
        case RolloutStrategyAttributeConditional.EXCLUDES:
          return !vals.Any(v => v.Contains(ip));
        case RolloutStrategyAttributeConditional.REGEX:
          return false;
        case null:
          return false;
        default:
          return false;
      }
    }
  }

  internal class IPNetworkProxy
  {
    private IPAddress _address;
    private IPNetwork _network;
    private bool _isAddress;

    public IPNetworkProxy(string addr)
    {
      if (addr.Contains("/"))
      {
        // it is a CIDR
        _isAddress = false;
        _network = IPNetwork.Parse(addr);
      }
      else
      {
        _isAddress = true;
        _address = IPAddress.Parse(addr);
      }
    }

    public bool Contains(IPNetworkProxy proxy)
    {
      if (proxy._isAddress && _isAddress)
      {
        return  _address.Equals(proxy._address);
      }

      if (!proxy._isAddress && !_isAddress)
      {
        return _network.Equals(this._network);
      }

      // they are an address and we are a network
      if (proxy._isAddress && !_isAddress)
      {
        return _network.Contains(proxy._address);
      }

      return false; // an ip address cannot contain a network
    }
  }
}
