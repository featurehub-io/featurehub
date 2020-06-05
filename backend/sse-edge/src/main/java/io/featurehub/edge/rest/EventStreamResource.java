package io.featurehub.edge.rest;

import io.featurehub.edge.ServerConfig;
import io.featurehub.edge.bucket.EventOutputBucketService;
import io.featurehub.edge.client.TimedBucketClientConnection;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/features")
public class EventStreamResource {
  private static final Logger log = LoggerFactory.getLogger(EventStreamResource.class);

  private final EventOutputBucketService bucketService;
  private final ServerConfig serverConfig;

  @Inject
  public EventStreamResource(EventOutputBucketService bucketService, ServerConfig serverConfig) {
    this.bucketService = bucketService;
    this.serverConfig = serverConfig;
  }


  @GET
  @Path("{namedCache}/{environmentId}/{apiKey}")
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  public EventOutput features(@PathParam("namedCache") String namedCache, @PathParam("environmentId") String envId, @PathParam("apiKey") String apiKey) {
    EventOutput o = new EventOutput();

    try {
      TimedBucketClientConnection b = new TimedBucketClientConnection.Builder().environmentId(envId).apiKey(apiKey).namedCache(namedCache).output(o).build();

      if (b.discovery()) {
        serverConfig.requestFeatures(b);

        bucketService.putInBucket(b);
      }
    } catch (Exception e) {
      log.error("failed to write feature states");
      throw new InternalServerErrorException(e);
    }

    return o;
  }

//  @PUT
//  @Path("/{feature}/{state}")
//  public Response update(@PathParam("feature") String feature, @PathParam("state") String state) {
//    features.stream().filter(f -> f.name.equals(feature)).findFirst().ifPresent(f -> {
//      f.state = FeatureState.StateEnum.valueOf(state);
//      List<EventOutput> bad = new ArrayList<>();
//      outputs.parallelStream().forEach(o -> {
//        if (o.isClosed()) {
//          bad.add(o);
//        } else {
//          final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
//          eventBuilder.name("one");
//          eventBuilder.data(f.toString());
//          final OutboundEvent event = eventBuilder.build();
//          try {
//            o.write(event);
//          } catch (IOException e) {
//            bad.add(o);
//          }
//        }
//      });
//      outputs.removeAll(bad);
//    });
//
//    return Response.ok().build();
//  }
}
