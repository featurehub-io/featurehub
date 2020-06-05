package todo.backend;

import bathe.BatheBooter;
import org.junit.Test;

import java.io.IOException;

public class AppRunner {
  @Test
  public void run() throws IOException {
    new BatheBooter().run(new String[]{"-R" + Application.class.getName(), "-Pclasspath:/application.properties", "-P${user.home}/.featurehub/example-java.properties"});
  }
}
