import React from 'react';
import ReactDOM from 'react-dom';
import { HashRouter as Router, Route } from 'react-router-dom';
import FlowPage from './Flow';

ReactDOM.render(
  <Router>
    <Route path="/" component={FlowPage} />
  </Router>,
  document.getElementById('root'),
);
