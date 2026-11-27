import com.github.luben.zstd.Zstd;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.channel.epoll.Epoll;
import io.grpc.netty.shaded.io.netty.handler.ssl.OpenSsl;
import io.nats.client.NKey;
import net.jpountz.lz4.LZ4Factory;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.xerial.snappy.Snappy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads every native-backed library on the FeatureHub server runtime classpath through its public API, inside the
 * pinned Alpine (musl) Temurin image. Run with the same JVM flags as the images. Exits non-zero if any check fails.
 *
 * java -Dorg.bouncycastle.native.cpu_variant=java -cp 'lib/*' MuslSmoke.java
 */
public class MuslSmoke {
  interface Check { String run() throws Throwable; }

  private static final byte[] PAYLOAD =
    "FeatureHub musl smoke test payload, repeated so it compresses. ".repeat(64).getBytes(StandardCharsets.UTF_8);

  public static void main(String[] args) {
    Map<String, Check> checks = new LinkedHashMap<>();

    checks.put("grpc OpenSsl (BoringSSL) or JDK TLS fallback", () -> {
      boolean native_ = OpenSsl.isAvailable();
      GrpcSslContexts.forClient().build();
      return native_ ? "native " + OpenSsl.versionString() : "JDK TLS fallback (" + OpenSsl.unavailabilityCause() + ")";
    });

    checks.put("grpc Epoll", () -> {
      if (!Epoll.isAvailable()) throw new IllegalStateException("epoll unavailable", Epoll.unavailabilityCause());
      return "available";
    });

    checks.put("Bouncy Castle native status + Ed25519", () -> {
      String status = CryptoServicesRegistrar.getNativeServices().getStatusMessage();
      Ed25519KeyPairGenerator gen = new Ed25519KeyPairGenerator();
      gen.init(new Ed25519KeyGenerationParameters(new SecureRandom()));
      AsymmetricCipherKeyPair kp = gen.generateKeyPair();
      Ed25519Signer signer = new Ed25519Signer();
      signer.init(true, kp.getPrivate());
      signer.update(PAYLOAD, 0, PAYLOAD.length);
      byte[] sig = signer.generateSignature();
      Ed25519Signer verifier = new Ed25519Signer();
      verifier.init(false, kp.getPublic());
      verifier.update(PAYLOAD, 0, PAYLOAD.length);
      if (!verifier.verifySignature(sig)) throw new IllegalStateException("Ed25519 verify failed");
      return "native status: " + status;
    });

    checks.put("jnats NKey create/sign/verify", () -> {
      NKey key = NKey.createUser(new SecureRandom());
      byte[] sig = key.sign(PAYLOAD);
      if (!key.verify(PAYLOAD, sig)) throw new IllegalStateException("NKey verify failed");
      return "ok";
    });

    checks.put("zstd round trip", () -> {
      byte[] c = Zstd.compress(PAYLOAD);
      byte[] d = Zstd.decompress(c, PAYLOAD.length);
      if (!Arrays.equals(PAYLOAD, d)) throw new IllegalStateException("zstd mismatch");
      return PAYLOAD.length + " -> " + c.length;
    });

    checks.put("snappy round trip", () -> {
      byte[] c = Snappy.compress(PAYLOAD);
      if (!Arrays.equals(PAYLOAD, Snappy.uncompress(c))) throw new IllegalStateException("snappy mismatch");
      return Snappy.getNativeLibraryVersion() + " " + PAYLOAD.length + " -> " + c.length;
    });

    checks.put("lz4 fastest instance", () -> {
      LZ4Factory f = LZ4Factory.fastestInstance();
      byte[] c = f.fastCompressor().compress(PAYLOAD);
      byte[] d = f.fastDecompressor().decompress(c, PAYLOAD.length);
      if (!Arrays.equals(PAYLOAD, d)) throw new IllegalStateException("lz4 mismatch");
      return f.toString();
    });

    int failures = 0;
    System.out.printf("arch=%s os=%s java=%s%n", System.getProperty("os.arch"), System.getProperty("os.name"),
      System.getProperty("java.version"));
    for (Map.Entry<String, Check> e : checks.entrySet()) {
      try {
        System.out.printf("PASS %-48s %s%n", e.getKey(), e.getValue().run());
      } catch (Throwable t) {
        failures++;
        System.out.printf("FAIL %-48s %s%n", e.getKey(), t);
        t.printStackTrace(System.out);
      }
    }

    System.out.println(failures == 0 ? "all musl checks passed" : failures + " musl check(s) failed");
    System.exit(failures == 0 ? 0 : 1);
  }
}
