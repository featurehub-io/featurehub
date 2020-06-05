package io.featurehub.dacha;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 */
public class CacheTimer {
  private Timer timer;

  public void schedule(TimerTask task, int period) {
    if (timer != null) {
      timer.cancel();
    }
    timer = new Timer();
    timer.schedule(task, period);
  }

  public void cancel() {
    if (timer != null) {
      timer.cancel();
    }
    timer = null;
  }
}
