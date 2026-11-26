package io.featurehub.sse;


import io.featurehub.sse.model.FeatureEnvironmentCollection;
import io.featurehub.sse.model.FeatureStateUpdate;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import java.util.List;

@Path("/features")
public interface LocalFeatureService {

  /**
   *
   * Requests all features for this sdkurl and disconnects
   * @param apiKey A list of API keys to retrieve information about (required)
   * @param contextSha A SHA of the context in string form designed to break any cache if the client changes context. It is not used by the server in any way, but if Fastly support is turned on it must be present. (optional)

   * @return List&lt;FeatureEnvironmentCollection&gt;

   */
  @GET
  @Path("/")
  @Produces({ "application/json" })
  public List<FeatureEnvironmentCollection> getFeatureStates(@org.jetbrains.annotations.NotNull @jakarta.validation.constraints.NotNull
                                                             @QueryParam("apiKey") List<String> apiKey
    , @org.jetbrains.annotations.Nullable
                                                             @QueryParam("contextSha") String contextSha
  );
  /**
   *
   * Updates the feature state if allowed.
   * @param featureKey The key you wish to update/action (required)
   * @param featureStateUpdate  (required)

   */
  @PUT
  @Path("/{env}/{apiKey}/{featureKey}")
  @Consumes({ "application/json" })
  public void setFeatureState( @PathParam("env")
                               String envId, @PathParam("apiKey") String apiKey
    ,  @PathParam("featureKey")  @org.jetbrains.annotations.NotNull @jakarta.validation.constraints.NotNull
                               String featureKey
    ,
                               @org.jetbrains.annotations.NotNull @jakarta.validation.constraints.NotNull
                               @jakarta.validation.Valid
                               FeatureStateUpdate featureStateUpdate);
}
