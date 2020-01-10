import React from 'react';
import ReactDOM from 'react-dom';
import { createStore, combineReducers, compose, applyMiddleware } from 'redux';
import { routerReducer } from 'react-router-redux';
import thunk from 'redux-thunk';
import { Provider } from 'react-redux';
import { REDUX_DEVTOOLS } from './contants';
import reducers from './reducers';
import App from './app';
import '@alicloud/console-components/dist/wind.css';
import './index.scss';

const reducer = combineReducers({
    ...reducers,
    routing: routerReducer,
});

const store: any = createStore(
    reducer,
    compose(
        applyMiddleware(thunk),
        (window as any)[REDUX_DEVTOOLS] ? (window as any)[REDUX_DEVTOOLS]() : (f: any) => f
    )
);

ReactDOM.render(
    <Provider store={store}>
      <App />
    </Provider>,
    document.getElementById('root')
);