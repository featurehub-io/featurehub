package io.featurehub.edge

import cd.connect.jersey.common.LoggingConfiguration
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import io.featurehub.jersey.config.CommonConfiguration
import io.featurehub.sse.LocalFeatureService
import io.featurehub.sse.Todo
import io.featurehub.sse.TodoService
import io.featurehub.sse.model.FeatureStateUpdate
import jakarta.ws.rs.client.ClientBuilder
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.proxy.WebResourceFactory

class TodoUtils {
  static final String todoUrl = "http://localhost:${System.getenv('TODO_PORT') ?: '8099'}"

  static final client = ClientBuilder.newClient()
  .register(CommonConfiguration)
  .register(LoggingConfiguration)

  static final TodoService todoService;
  static LocalFeatureService edgeService;

  static {
    client.property(ClientProperties.CONNECT_TIMEOUT, 4000)
    client.property(ClientProperties.READ_TIMEOUT, 4000)
    todoService = WebResourceFactory.newResource(TodoService, client.target(todoUrl))
  }

  List<Todo> postTodo(String id, String title, String user = "kornnaphat") {
    return todoService.addTodo(user, new Todo(id: id, title: title))
  }

  Map<String, String> readTestEnv() {
    String currentDir = new File(".").canonicalPath
    int backendPos = currentDir.indexOf("${File.separator}backend")
    if (backendPos == -1) {
      throw new RuntimeException("cannot find backend directory in ${currentDir}")
    }

    def envFile = new File(currentDir.substring(0, backendPos), "adks/e2e-sdk/test-example.env")

    return envFile.readLines()
      .collect { it.trim() }
      .findAll { it && !it.startsWith("#") && it.contains("=") }
      .collectEntries { line ->
        int equals = line.indexOf("=")
        [(line.substring(0, equals).trim()): line.substring(equals + 1).trim()]
      } as Map<String, String>
  }

  static final ObjectMapper mapper = new ObjectMapper()
    .findAndRegisterModules()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  static void setupEdgeService(Map<String, String> env) {
    String edgeUrl = env['FEATUREHUB_EDGE_URL']
    if (!edgeUrl.startsWith("http")) {
      edgeUrl = "http://${edgeUrl}"
    }
    if (edgeUrl.endsWith("/")) {
      edgeUrl = edgeUrl.substring(0, edgeUrl.length() - 1)
    }

    edgeService = WebResourceFactory.newResource(LocalFeatureService, client.target(edgeUrl))
  }

  void updateFeatureValue(Map<String, String> env, String key, Object value) {
    def update = new FeatureStateUpdate().updateValue(true).value(value)

    def parts = env['FEATUREHUB_CLIENT_API_KEY'].split("/")
    edgeService.setFeatureState(parts[0], parts[1], key, update)
  }

  @CompileStatic
  void waitForFeatureValue(Map<String, String> env, String key, Object value) {
    def deadline = System.currentTimeMillis() + 30_000
    String lastError = "no response"

    while (System.currentTimeMillis() < deadline) {
      try {
        def collections = edgeService.getFeatureStates([env['FEATUREHUB_CLIENT_API_KEY']], "0")

        def feature = collections ? collections[0].features?.find { it.key == key } : null
        if (feature == null) {
          lastError = "feature ${key} not found"
        } else if (feature.value == value) {
          return
        } else {
          lastError = "feature ${key} has value ${feature.value}, expected ${value}"
        }
      } catch (IOException e) {
        lastError = e.toString()
      }

      Thread.sleep(500)
    }

    throw new RuntimeException("feature ${key} did not become ${value} after 30 seconds, last error: ${lastError}")
  }

  @CompileStatic
  List<Todo> getTodos(String user = "kornnaphat") {
    return todoService.listTodos(user)
  }

  @CompileStatic
  void todoLivenessCheck() {
    def deadline = System.currentTimeMillis() + 30_000

    while (System.currentTimeMillis() < deadline) {
      try {
        todoService.readiness()
        println("liveness ok")
        return
      } catch (IOException e) {
        println("liveness failed " + e)
      }

      Thread.sleep(500)
    }

    throw new RuntimeException("todo server at ${todoUrl} not ready after 30 seconds")
  }
}
