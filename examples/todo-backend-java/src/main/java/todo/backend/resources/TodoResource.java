package todo.backend.resources;

import io.featurehub.client.StaticFeatureContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import todo.Features;
import todo.api.TodoService;
import todo.model.Todo;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class TodoResource implements TodoService {
  private static final Logger log = LoggerFactory.getLogger(TodoResource.class);
  Map<String, Todo> todos = new ConcurrentHashMap();

  public TodoResource() {
    log.info("created");
  }

  public List<Todo> addTodo(Todo body) {
    body.id(UUID.randomUUID().toString());

    todos.put(body.getId(), body);

    return getTodoList();
  }

  public List<Todo> listTodos() {
    return getTodoList();
  }

  public List<Todo> removeTodo(String id) {
    todos.remove(id);
    return getTodoList();
  }

  public List<Todo> resolveTodo(String id) {
    Todo todo = todos.get(id);
    if (todo != null) {
      todo.resolved(true);
    }
    return getTodoList();
  }

  private List<Todo> getTodoList() {

    if (Features.FEATURE_TITLE_TO_UPPERCASE.isActive()) {
      StaticFeatureContext.getInstance().logAnalyticsEvent("list-by-uppercase");
      return new ArrayList<Todo>(todos.values().stream().map(t -> t.copy().title(t.getTitle().toUpperCase()))
        .collect(Collectors.toList()));
    }

    StaticFeatureContext.getInstance().logAnalyticsEvent("list-by-mixedcase");
    return new ArrayList<Todo>(todos.values());
  }
}
