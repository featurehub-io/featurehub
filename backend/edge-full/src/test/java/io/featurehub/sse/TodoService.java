package io.featurehub.sse;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import java.util.List;

public interface TodoService {
  @GET
  @Path("/health/readiness")
  void readiness();

  @GET
  @Path("/todo/{user}")
  @Produces({ "application/json" })
  List<Todo> listTodos(@PathParam("user") @org.jetbrains.annotations.NotNull String user
  );

  @POST
  @Path("/todo/{user}")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  List<Todo> addTodo( @PathParam("user") @org.jetbrains.annotations.NotNull String user
    ,
                             @org.jetbrains.annotations.NotNull
                             Todo todo);

}
