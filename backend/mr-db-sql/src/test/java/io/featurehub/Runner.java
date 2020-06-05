package io.featurehub;

import io.ebean.DB;
import io.ebean.Database;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {
  private static final Logger log = LoggerFactory.getLogger(Runner.class);

  @Test
  public void run() throws Exception {
    log.info("here i am");
//    ApplicationHandler ah = new ApplicationHandler(new ResourceConfig().register(new DBServiceModule()));

//    ah.getInjectionManager().getInstance(EbeanHolder.class);

      Database database = DB.getDefault();
  }
}
