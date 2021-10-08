package io.featurehub.strategies.matchers;

/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Portions from Spring Security Web, Apache Commons Net, and
 * StackOverflow.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Matches a request based on IP Address or subnet mask matching against the remote
 * address.
 * <p>
 * Both IPv6 and IPv4 addresses are supported, but a matcher which is configured with an
 * IPv4 address will never match a request which returns an IPv6 address, and vice-versa.
 *
 * @author Luke Taylor
 * @since 3.0.2
 *
 * Slightly modified by omidzk to have zero dependency to any frameworks other than the JRE.
 * Slightly modified by rvowles to remove unnecessary code for our use case.
 */
public class CIDRMatch {

  public static InetAddress suppliedAddress(String address) throws UnknownHostException {
     return InetAddress.getByName(address);
  }

  /**
   * Takes a specific IP address or a range specified using the IP/Netmask (e.g.
   * 192.168.1.0/24 or 202.24.0.0/14).
   *
   * @param ipAddress the address or range of addresses from which the request must
   * come.
   */
  public static boolean cidrMatch(String ipAddress, InetAddress remoteAddress) {
    int nMaskBits;
    InetAddress requiredAddress;

    if (ipAddress.indexOf('/') > 0) {
      String[] addressAndMask = ipAddress.split("/");
      ipAddress = addressAndMask[0];
      nMaskBits = Integer.parseInt(addressAndMask[1]);
    }
    else {
      nMaskBits = -1;
    }

    try {
      requiredAddress = InetAddress.getByName(ipAddress);

      if (requiredAddress.getAddress().length * 8 >= nMaskBits) {


        if (!requiredAddress.getClass().equals(remoteAddress.getClass())) {
          return false;
        }

        if (nMaskBits < 0) {
          return remoteAddress.equals(requiredAddress);
        }

        byte[] remAddr = remoteAddress.getAddress();
        byte[] reqAddr = requiredAddress.getAddress();

        int nMaskFullBytes = nMaskBits / 8;
        byte finalByte = (byte) (0xFF00 >> (nMaskBits & 0x07));

        for (int i = 0; i < nMaskFullBytes; i++) {
          if (remAddr[i] != reqAddr[i]) {
            return false;
          }
        }

        if (finalByte != 0) {
          return (remAddr[nMaskFullBytes] & finalByte) == (reqAddr[nMaskFullBytes] & finalByte);
        }

        return true;
      }
    } catch (Exception ignored) {
    }

    return false;
  }
}

