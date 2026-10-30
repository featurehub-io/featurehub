package cd.connect.jersey.common;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

@Path("/metrics")
@Produces(MediaType.TEXT_PLAIN)
public class JerseyPrometheusResource {
  private CollectorRegistry registry;

  public JerseyPrometheusResource(CollectorRegistry registry) {
    this.registry = registry;
  }

  public JerseyPrometheusResource() {
    this(CollectorRegistry.defaultRegistry);
  }

  @GET
  public String metrics(@QueryParam("name") Set<String> metrics) {
    StringWriter sMetrics = new StringWriter();
    PrintWriter writer =  new PrintWriter(sMetrics);

    try {
      TextFormat.write004(writer, this.registry.filteredMetricFamilySamples(metrics));
    } catch (IOException e) {
      throw new BadRequestException("Unable to handle metric request.");
    }

    return sMetrics.toString();
  }
}
