import * as React from 'react';
import { Configuration, DefaultApi, Todo } from './api';
import './App.css';
import globalAxios from 'axios';
import {
  FeatureContext,
  featureHubRepository,
  // GoogleAnalyticsCollector,
  Readyness,
  FeatureUpdater,
  FeatureHubPollingClient
} from 'featurehub-repository/dist';
import { FeatureHubEventSourceClient } from 'featurehub-eventsource-sdk/dist';

declare global {
  interface Window {
    FeatureUpdater: any;
    PollingService: any;
    Repository: any;
  }
}

window.FeatureUpdater = FeatureUpdater;
window.PollingService = FeatureHubPollingClient;
window.Repository = featureHubRepository;

let todoApi: DefaultApi;
// const todoApi = new DefaultApi(new Configuration({basePath:
//     baseUrl === 'UNKNOWN_BASE' ? 'http://localhost:8099' : baseUrl }));

class TodoData {
  todos: Array<Todo>;
  buttonColour: string | undefined;
  ready: boolean = false;
  featuresUpdated: boolean = false;

  constructor(todos?: Array<Todo>, buttonColour?: string, ready?: boolean) {
    this.todos = todos || [];
    this.buttonColour = buttonColour || 'blue';
    this.ready = ready || false;
  }

  changeColor(colour: string | undefined): TodoData {
    return new TodoData(this.todos, colour, true);
  }

  changeTodos(todos: Array<Todo>): TodoData {
    return new TodoData(todos, this.buttonColour, this.ready);
  }

  changeFeaturesUpdated(fu: boolean): TodoData {
    const td = new TodoData(this.todos, this.buttonColour, this.ready);
    td.featuresUpdated = fu;
    return td;
  }
}

class ConfigData {
  baseUrl: string;
  sdkUrl: string;
}

class App extends React.Component<{}, { todos: TodoData }> {
  private titleInput: HTMLInputElement;
  private eventSource: FeatureHubEventSourceClient;

  constructor() {
    super([]);

    this.state = {
      todos: new TodoData(),
    };
  }

  async initializeFeatureHub() {
    featureHubRepository.addReadynessListener((readyness) => {
      console.log('readyness', readyness);
      if (readyness === Readyness.Ready) {
        const color = featureHubRepository.getFeatureState('SUBMIT_COLOR_BUTTON').getString();
        this.setState({todos: this.state.todos.changeColor(color)});
      }
    });

    featureHubRepository
      .addPostLoadNewFeatureStateAvailableListener((_) =>
                                                   this.setState(
                                                     {todos: this.state.todos.changeFeaturesUpdated(true)}) );

    featureHubRepository.catchAndReleaseMode = true; // don't allow feature updates to come through

    // load the config from the config json file
    const config = (await globalAxios.request({url: 'featurehub-config.json'})).data as ConfigData;
    // setup the api
    todoApi = new DefaultApi(new Configuration({basePath: config.baseUrl }));
    this._loadInitialData(); // let this happen in background

    // listen for reatures from configured source
    this.eventSource = new FeatureHubEventSourceClient(config.sdkUrl);
    this.eventSource.init();

    // alternative if you want to ready to incoming feature changes and not wait for a page refresh
    // featureHubRepository.getFeatureState('SUBMIT_COLOR_BUTTON').addListener((fs: FeatureStateHolder) => {
    //   this.setState({todos: this.state.todos.changeColor(fs.getString())});
    // });

    // featureHubRepository.addAnalyticCollector(new GoogleAnalyticsCollector('UA-1234', '1234-5678-abcd-1234'));
  }

  async componentDidMount() {
    this.initializeFeatureHub();
  }

  async _loadInitialData() {
    const todoResult = (await todoApi.listTodos({})).data;
    this.setState({todos: this.state.todos.changeTodos(todoResult)});
  }

  componentWillUnmount(): void {
    if (this.eventSource) {
      this.eventSource.close();
    }
  }

  async addTodo(title: string) {
    const todo: Todo = {
      id: '',
      title,
      resolved: false,
    };

    FeatureContext.logAnalyticsEvent('todo-add', new Map([['gaValue', '10']])); // no cid
    const todoResult = (await todoApi.addTodo(todo)).data;
    this.setState({todos: this.state.todos.changeTodos(todoResult)});
  }

  async removeToDo(id: string) {
   FeatureContext.logAnalyticsEvent('todo-remove', new Map([['gaValue', '5']]));
    const todoResult = (await todoApi.removeTodo(id)).data;
    this.setState({todos: this.state.todos.changeTodos(todoResult)});
  }

  async doneToDo(id: string) {
    const todoResult = (await todoApi.resolveTodo(id)).data;
    this.setState({todos: this.state.todos.changeTodos(todoResult)});
  }

  render() {
    if (!this.state.todos.ready) {
      return (
        <div className="App">
          <h1>Waiting for initial features.</h1>
        </div>
      );
    }
    let buttonStyle = {
      color: this.state.todos.buttonColour
    };
    return (
      <div className="App">
        {this.state.todos.featuresUpdated &&
        (<div className="updatedFeatures">There are updated features available.
          <button onClick={() => window.location.reload()}>REFRESH</button></div>)}
        <h1>Todo List</h1>
        <form
          onSubmit={e => {
            e.preventDefault();
            this.addTodo(this.titleInput.value);
            this.titleInput.value = '';
          }}
        >
          <input
            ref={node => {
              if (node !== null) {
                this.titleInput = node;
              }
            }}
          />
          <button type="submit" style={buttonStyle}>Add</button>
        </form>
        <ul>
          {this.state.todos.todos.map((todo, index) => {
            return (
              <li
                className="qa-main"
                key={index}
                style={{
                  textDecoration: todo.resolved ? 'line-through' : 'none',
                }}
              >
                {!todo.resolved && (
                  <button onClick={() => this.doneToDo(todo.id || '')} className="qa-done-button">Done</button>
                )}
                <button onClick={() => this.removeToDo(todo.id || '')} className="qa-delete-button">
                  Delete
                </button>
                {' '}
                {todo.title}
              </li>
            );
          })}
        </ul>
      </div>
    );
  }
}

export default App;
