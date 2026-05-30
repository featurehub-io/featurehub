package cd.connect.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.slf4j.MDC;

@Plugin(
  name = "DisableLogsFilter",
  category = Node.CATEGORY,
  elementType = Filter.ELEMENT_TYPE,
  printObject = true
)
public class DisableLogsFilter extends AbstractFilter {

  private Result action() {
    if ("true".equals(MDC.get("connect.disable-logs"))) {
      return Result.DENY;
    } else {
      return Result.NEUTRAL;
    }
  }

  @Override
  public Result filter(final LogEvent event) {
    return event.getContextData().containsKey("connect.disable-logs") ? Result.DENY : Result.NEUTRAL;
  }

  @Override
  public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                       final Throwable t) {
    return action();
  }

  @Override
  public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                       final Throwable t) {
    return action();
  }

  @Override
  public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                       final Object... params) {
    return action();
  }

  @PluginBuilderFactory
  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder extends AbstractFilterBuilder<Builder>
    implements org.apache.logging.log4j.core.util.Builder<DisableLogsFilter> {
    @Override
    public DisableLogsFilter build() {
      return new DisableLogsFilter();
    }
  }
}
