/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * Copyright 2016, Joyent, Inc.
 *
 * Converted to Typescript by Anyways Labs Ltd 2021, AddrRange dropped in favour of code limiting to what
 * is required for this library. We converted this so it would run in the browser as well as node, the
 * original only ran in node.
 */

export class ParseError extends Error {
  constructor(readonly input: string, readonly message: string, readonly index?: number) {
    super(index !== undefined ? `$message at index $index` : message);
  }
}

function modulo(a: number, n: number) {
  return (n + (a % n)) % n;
}

function _arrayToOctetString(input: number[]) {
  let out;
  // tslint:disable-next-line:no-bitwise
  out = (input[0] >> 8) + '.' + (input[0] & 0xff) + '.';
  // tslint:disable-next-line:no-bitwise
  out += (input[1] >> 8) + '.' + (input[1] & 0xff);
  return out;
}

function _isAddr(addr: any): boolean {
  return addr instanceof Addr;
}

function _arrayToHex(input: number[], zeroElide: boolean, zeroPad: boolean): string[] {
  let i;
  let elStart = null;
  let elLen = 0;

  if (zeroElide) {
    /* find longest run of zeroes to potentially elide */
    let start = null;
    let len = null;
    for (i = 0; i < input.length; i++) {
      if (input[i] === 0) {
        if (start === null) {
          start = i;
          len = 1;
        } else {
          len++;
        }
      } else if (start !== null) {
        if (len > elLen) {
          elStart = start;
          elLen = len;
        }
        start = null;
      }
    }
    /* capturing last potential zero */
    if (start !== null && len > elLen) {
      elStart = start;
      elLen = len;
    }
  }

  const output: string[] = [];
  let num;

  for (i = 0; i < input.length; i++) {
    if (elStart !== null) {
      if (i === elStart) {
        if (elLen === 8) {
          /* all-zeroes is just '::' */
          return ['::'];
        } else if (elStart === 0 || elStart + elLen === input.length) {
          /*
           * For elided zeroes at the beginning/end of the address, an extra
           * ':' is needed during the join step.
           */
          output.push(':');
        } else {
          output.push('');
        }
      }
      if (i >= elStart && i < elStart + elLen) {
        continue;
      }
    }
    num = input[i].toString(16);
    if (zeroPad && num.length !== 4) {
      num = '0000'.slice(num.length) + num;
    }
    output.push(num);
  }

  return output;
}

function _ipv4Mapped(input: number[]): boolean {
  const comp = [0, 0, 0, 0, 0, 0xffff];

  let i;

  for (i = 0; i < 6; i++) {
    if (input[i] !== comp[i]) {
      return false;
    }
  }

  return true;
}

/**
 * IPv6/IPv4 address representation.
 *
 * It should not be instantiated directly by library consumers.
 */

export type AddrType = 'ipv4' | 'ipv6';
export type AddrFormat = 'v4' | 'auto' | 'v4-mapped' | 'v6';

export interface AddrOpts {
  format?: AddrFormat;
  zeroElide?: boolean;
  zeroPad?: boolean;

}

class AddrAttrs {
  ipv4Bare: boolean = false;
  ipv4Mapped: boolean = false;
}

export class Addr {
  public _fields: number[] = [0, 0, 0, 0, 0, 0, 0, 0];
  public _attrs = new AddrAttrs();

  public getKind(): AddrType {
    if (v4subnet.contains(this)) {
      return 'ipv4';
    } else {
      return 'ipv6';
    }
  }

  public toString(opts: AddrOpts): string {
    const format = opts.format === 'auto' ? this._deriveFormatFromAttrs() : opts.format;

    switch (format) {
      // Print in dotted-quad notation (but only if truly IPv4)
      case 'v4':
        if (!v4subnet.contains(this)) {
          throw new Error('cannot print non-v4 address in dotted quad notation');
        }
        return _arrayToOctetString(this._fields.slice(6));

      // Print as an IPv4-mapped IPv6 address
      case 'v4-mapped':
        if (!v4subnet.contains(this)) {
          throw new Error('cannot print non-v4 address as a v4-mapped address');
        }
        let output = _arrayToHex(this._fields.slice(0, 6),
                                 opts.zeroElide === undefined ? true : opts.zeroElide, opts.zeroPad || false);
        output.push(_arrayToOctetString(this._fields.slice(6)));
        return output.join(':');

      // Print as an IPv6 address
      case 'v6':
        return _arrayToHex(this._fields,
                           opts.zeroElide === undefined ? true : opts.zeroElide, opts.zeroPad || false).join(':');

      // Unrecognized formatting method
      default:
        throw new Error('unrecognized format method "' + format + '"');
    }
  }

  public compare(b: Addr): number {
    let i;
    for (i = 0; i < 8; i++) {
      if (this._fields[i] < b._fields[i]) {
        return -1;
      } else if (this._fields[i] > b._fields[i]) {
        return 1;
      }
    }
    return 0;
  }

  public and(val: Addr | string): Addr {
      const input = _isAddr(val) ? (val as Addr) : Addr.toAddr(val);
      const output = this.clone();
      for (let i = 0; i < 8; i++) {
        // tslint:disable-next-line:no-bitwise
        output._fields[i] = output._fields[i] & input._fields[i];
      }
      return output;
  }

  public clone(): Addr {
    const out = new Addr();
    out._fields = this._fields.slice();

    // tslint:disable-next-line:forin
    for ( let k in this._attrs ) {
      out._attrs[k] = this._attrs[k];
    }

    return out;
  }

  public not(): Addr {
    var output = this.clone();
    for (let i = 0; i < 8; i++) {
      // tslint:disable-next-line:no-bitwise
      output._fields[i] = (~ output._fields[i]) & 0xffff;
    }
    return output;
  }

  public or(input: Addr | string): Addr {
    const addr = Addr.toAddr(input);

    const output = this.clone();

    for (let i = 0; i < 8; i++) {
      // tslint:disable-next-line:no-bitwise
      output._fields[i] = output._fields[i] | addr._fields[i];
    }

    return output;
  }

  public offset(num: number): Addr {
    if (num < -4294967295 || num > 4294967295) {
      throw new Error('offsets should be between -4294967295 and 4294967295');
    }
    const out = this.clone();
    let i, moved;
    for (i = 7; i >= 0; i--) {
      moved = out._fields[i] + num;
      if (moved > 65535) {
        // tslint:disable-next-line:no-bitwise
        num = moved >>> 16;
        // tslint:disable-next-line:no-bitwise
        moved = moved & 0xffff;
      } else if (moved < 0) {
        // tslint:disable-next-line:no-bitwise
        num = Math.floor(moved / (1 << 16));
        // tslint:disable-next-line:no-bitwise
        moved = modulo(moved, 1 << 16);
      } else {
        num = 0;
      }
      out._fields[i] = moved;

      /* Prevent wrap-around for both ipv6 and ipv4-mapped addresses */
      if (num !== 0) {
        if ((i === 0) || (i === 6 && this._attrs.ipv4Mapped)) {
          return null;
        }
      } else {
        break;
      }
    }
    return out;
  }

  public static toAddr(input: Addr | string): Addr {
    if (typeof (input) === 'string') {
      return Addr._ip6AddrParse(input);
    } else if (_isAddr(input)) {
      return input;
    } else {
      throw new Error('Invalid argument: Addr or parsable string expected');
    }
  }

  protected static _ip6AddrParse(input: string): Addr {
    input = input.toLowerCase();
    let result = new Addr();

    let ip6Fields = []; // hold unparsed hex fields
    let ip4Fields = []; // hold unparsed decimal fields
    let expIndex = null; // field index of '::' delimiter
    let value = '';  // accumulate unparsed hex/dec field
    let i, c;

    /*
     * No valid ipv6 is longer than 39 characters.
     * An extra character of leeway is there to tolerate some :: funny business.
     */
    if (input.length > 40) {
      throw new ParseError(input, 'Input too long');
    }

    for (i = 0; i < input.length; i++) {
      c = input[i];
      if (c === ':') {
        if ((i + 1) < input.length && input[i + 1] === ':') {
          /*
           * Variable length '::' delimiter.
           * Multiples would be ambiguous
           */
          if (expIndex !== null) {
            throw new ParseError(input, 'Multiple :: delimiters', i);
          }

          /*
           * The value buffer can be empty for cases where the '::' delimiter is
           * the first portion of the address.
           */
          if (value !== '') {
            ip6Fields.push(value);
            value = '';
          }
          expIndex = ip6Fields.length;
          i++;
        } else {
          /*
           * Standard ':' delimiter
           * The value buffer cannot be empty since that would imply an illegal
           * pattern such as ':::' or ':.'.
           */
          if (value === '') {
            throw new ParseError(input, 'illegal delimiter', i);
          }
          ip6Fields.push(value);
          value = '';
        }
      } else if (c === '.') {
        /*
         * Handle dotted quad notation for ipv4 and ipv4-mapped addresses.
         */
        ip4Fields.push(value);
        value = '';
      } else {
        value = value + c;
      }
    }
    /* Handle the last stashed value */
    if (value !== '') {
      if (ip4Fields.length !== 0) {
        ip4Fields.push(value);
      } else {
        ip6Fields.push(value);
      }
    } else {
      /* With no stashed value, the address must end with '::'. */
      if (expIndex !== ip6Fields.length || ip4Fields.length > 0) {
        throw new ParseError(input, 'Cannot end with delimiter besides ::');
      }
    }

    /* With values collected, ensure we don't have too many/few */
    if (ip4Fields.length === 0) {
      if (ip6Fields.length > 8) {
        throw new ParseError(input, 'Too many fields');
      } else if (ip6Fields.length < 8 && expIndex === null) {
        throw new ParseError(input, 'Too few fields');
      }
    } else {
      if (ip4Fields.length !== 4) {
        throw new ParseError(input, 'IPv4 portion must have 4 fields');
      }
      /* If this is a bare IP address, implicitly convert to IPv4 mapped */
      if (ip6Fields.length === 0 && expIndex === null) {
        result._attrs.ipv4Bare = true;
        ip6Fields = ['ffff'];
        expIndex = 0;
      }

      if (ip6Fields.length > 6) {
        throw new ParseError(input, 'Too many fields');
      } else if (ip6Fields.length < 6 && expIndex === null) {
        throw new ParseError(input, 'Too few fields');
      }
    }

    /* Parse integer values */
    let field, num;
    for (i = 0; i < ip6Fields.length; i++) {
      field = ip6Fields[i];
      num = parseInt(field, 16);
      if (num instanceof Error || num < 0 || num > 65535) {
        throw new ParseError(input, 'Invalid field value: ' + field);
      }
      ip6Fields[i] = num;
    }
    for (i = 0; i < ip4Fields.length; i++) {
      field = ip4Fields[i];
      num = parseInt(field, 10);
      if (num instanceof Error || num < 0 || num > 255) {
        throw new ParseError(input, 'Invalid field value: ' + field);
      }
      ip4Fields[i] = num;
    }

    /* Collapse IPv4 portion, if necessary */
    if (ip4Fields.length !== 0) {
      ip6Fields.push((ip4Fields[0] * 256) + ip4Fields[1]);
      ip6Fields.push((ip4Fields[2] * 256) + ip4Fields[3]);
    }

    /* Expand '::' delimiter into implied 0s */
    if (ip6Fields.length < 8 && expIndex !== null) {
      let filler = [];
      for (i = 0; i < (8 - ip6Fields.length); i++) {
        filler.push(0);
      }
      ip6Fields = Array.prototype.concat(
        ip6Fields.slice(0, expIndex),
        filler,
        ip6Fields.slice(expIndex)
      );
    }

    /*
     * If dotted-quad notation was used, ensure the input was either a bare ipv4
     * address or a valid ipv4-mapped address.
     */
    if (ip4Fields.length !== 0) {
      if (!_ipv4Mapped(ip6Fields)) {
        throw new ParseError(input, 'invalid dotted-quad notation');
      } else {
        result._attrs.ipv4Mapped = true;
      }
    }

    result._fields = ip6Fields;

    return result;
  }

  private _deriveFormatFromAttrs(): AddrFormat {
    // Try to print the address the way it was originally formatted
    if (this._attrs.ipv4Bare) {
      return 'v4';
    } else if (this._attrs.ipv4Mapped) {
      return 'v4-mapped';
    }

    return 'v6';
  }
}

/**
 * CIDR Block
 * @param addr CIDR network address
 * @param prefixLen Length of network prefix
 *
 * The addr parameter can be an Addr object or a parseable string.
 * If prefixLen is omitted, then addr must contain a parseable string in the
 * form '<address>/<prefix>'.
 */
export class CIDR {
  private readonly _prefix: number;
  private readonly _addr: Addr;
  private readonly _mask: Addr;

  constructor(addr: string, prefixLen?: number) {
    if (prefixLen === undefined) {
      /* OK to pass pass string of "<addr>/<prefix>" */
      let fields = addr.match(/^([a-fA-F0-9:.]+)\/([0-9]+)$/);

      if (fields === null) {
        throw new Error('Invalid argument: <addr>/<prefix> expected');
      }

      addr = fields[1];

      prefixLen = parseInt(fields[2], 10);
    }

    prefixLen = prefixLen || 0;
    this._addr = Addr.toAddr(addr);

    /* Expand prefix to ipv6 length of bare ipv4 address provided */
    if (this._addr._attrs.ipv4Bare) {
      prefixLen += 96;
    }

    if (prefixLen < 0 || prefixLen > 128) {
      throw new Error('Invalid prefix length');
    }

    this._prefix = prefixLen;
    this._mask = this._prefixToAddr(prefixLen);
    this._addr = this._addr.and(this._mask);
  }

  public contains(input: Addr | string): boolean {
    const addr = Addr.toAddr(input);
    return (this._addr.compare(addr.and(this._mask)) === 0);
  }

  public first(): Addr {
    if (this._prefix >= 127) {
      /* Support single-address and point-to-point networks */
      return this._addr;
    } else {
      return this._addr.offset(1);
    }
  }

  public last(): Addr {
    let ending = this._addr.or(this._mask.not());
    if (this._prefix >= 127) {
      /* Support single-address and point-to-point networks */
      return ending;
    } else {
      if (this._addr._attrs.ipv4Mapped) {
        /* don't include the broadcast for ipv4 */
        return ending.offset(-1);
      } else {
        return ending;
      }
    }
  }

  public compare(b: CIDR) {
    /*
     * We compare first on the address component, and then on the prefix length,
     * such that the network with the smaller prefix length (the larger subnet)
     * is greater than the network with the larger prefix (the smaller subnet).
     * This is the same ordering used in Postgres.
     */
    const cmp = this._addr.compare(b._addr);

    return cmp === 0 ? b._prefix - this._prefix : cmp;
  }

  public prefixLength(format?: AddrFormat): number {
    if (format === undefined || format === 'auto') {
      format = this._addr._attrs.ipv4Bare ? 'v4' : 'v6';
    }

    switch (format) {
      case 'v4':
        if (!v4subnet.contains(this._addr)) {
          throw new Error('cannot return v4 prefix length for non-v4 address');
        }
        return this._prefix - 96;
      case 'v6':
        return this._prefix;
      default:
        throw new Error('unrecognized format method "' + format + '"');
    }
  }

  public get address(): Addr {
    return this._addr;
  }

  public toString(opts?: AddrOpts): string {
    let format = opts?.format ?? 'auto';
    if (format === 'v4-mapped') {
      format = 'v6';
    }

    return this._addr.toString(opts) + '/' + this.prefixLength(format);
  }

  private _prefixToAddr(len: number): Addr {
    if (len > 128 || len < 0) {
      throw new Error('$len is out of bounds 0-128');
    }

    let output = new Addr();
    let i;
    for (i = 0; len > 16; i++, len -= 16) {
      output._fields[i] = 0xffff;
    }
    if (len > 0) {
      // tslint:disable-next-line:no-bitwise
      output._fields[i] = 0xffff - ((1 << (16 - len)) - 1);
    }
    return output;
  }

}

const v4subnet = new CIDR('::ffff:0:0', 96);
