package io.featurehub.db.password;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Optional;

public class PasswordSalter {
  private static final Logger log = LoggerFactory.getLogger(PasswordSalter.class);
  @ConfigKey("passwordsalt.iterations")
  Integer iterations = 1000;
//  @ConfigKey("passwordsalt.hash-algorithm")
//  String algorithm = "PBKDF2WithHmacSHA1";

  public PasswordSalter() {
    DeclaredConfigResolver.resolve(this);
  }

  public Optional<String> saltPassword(String password, String algorithm) {
    return Optional.ofNullable(saltAnyPassword(password, algorithm));
  }

  public String saltAnyPassword(String password, String algorithm) {
    if (password == null || password.trim().length() == 0) {
      return null;
    }

    int iterations = 1000;
    char[] chars = password.trim().toCharArray();

    try {
      byte[] salt = getSalt();

      PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
      SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
      byte[] hash = skf.generateSecret(spec).getEncoded();
      return iterations + ":" + toHex(salt) + ":" + toHex(hash);
    } catch (Exception e) {
      log.error("Failed to hash password", e);
      return null;
    }
  }

  private byte[] getSalt() throws NoSuchAlgorithmException {
    SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
    byte[] salt = new byte[16];
    sr.nextBytes(salt);
    return salt;
  }

  private String toHex(byte[] array) throws NoSuchAlgorithmException {
    BigInteger bi = new BigInteger(1, array);
    String hex = bi.toString(16);
    int paddingLength = (array.length * 2) - hex.length();
    if (paddingLength > 0) {
      return String.format("%0" + paddingLength + "d", 0) + hex;
    } else {
      return hex;
    }
  }

  public boolean validatePassword(String originalPassword, String storedPassword, String algorithm) {
    String[] parts = storedPassword.split(":");
    int iterations = Integer.parseInt(parts[0]);

    try {
      byte[] salt = fromHex(parts[1]);
      byte[] hash = fromHex(parts[2]);

      PBEKeySpec spec = new PBEKeySpec(originalPassword.toCharArray(), salt, iterations, hash.length * 8);
      SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
      byte[] testHash = skf.generateSecret(spec).getEncoded();
      int diff = hash.length ^ testHash.length;
      for (int i = 0; i < hash.length && i < testHash.length; i++) {
        diff |= hash[i] ^ testHash[i];
      }
      return diff == 0;
    } catch (Exception e) {
      log.error("Failed to validate password", e);
      return false;
    }
  }

  private byte[] fromHex(String hex) throws NoSuchAlgorithmException {
    byte[] bytes = new byte[hex.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
    }
    return bytes;
  }
}

