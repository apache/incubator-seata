/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';
import { Router, Route, Switch, Redirect, RouteComponentProps } from 'react-router-dom';
import { ConfigProvider, Loading } from '@alicloud/console-components';
import { createHashHistory, History } from 'history';
import CCConsoleMenu from '@alicloud/console-components-console-menu';
import { GlobalStateModel } from '@/reducers';
import { changeLanguage, LocaleStateModel, getCurrentLanguage } from '@/reducers/locale';
import Layout from '@/layout';
import Login from '@/pages/Login';
import router from '@/router';
import Iframe from './components/Iframe';

export const history: History = createHashHistory();
(window as any).globalHistory = history;

export type OwnProps = any;

export type StateToPropsType = LocaleStateModel;

export type DispathToPropsType = {
  changeLanguage: (lang: string) => void;
};

export type AppPropsType = StateToPropsType & DispathToPropsType & RouteComponentProps & OwnProps;

export type AppStateType = {
  loading: object;
  version: string;
};

class App extends React.Component<AppPropsType, AppStateType> {
  static propTypes = {
    locale: PropTypes.object,
    changeLanguage: PropTypes.func,
  };

  state: AppStateType = {
    loading: {},
    version: '',
  };

  constructor(props: AppPropsType) {
    super(props);
  }

  componentDidMount() {
    console.log('this.props: ', this.props, history);
    const language: string = getCurrentLanguage();
    this.props.changeLanguage(language);
    this.getVersion();
  }

  getVersion = () => {
    fetch('version.json').then(response =>
      response.json().then(json => this.setState({ ...this.state, version: json.version }))
    );
  };

  get menu() {
    const { locale }: AppPropsType = this.props;
    const { MenuRouter = {} } = locale;
    const { overview, transactionInfo, globalLockInfo, sagaStatemachineDesigner } = MenuRouter;
    return {
      items: [
        // {
        //     key: '/Overview',
        //     label: overview,
        // },
        {
          key: '/transaction/list',
          label: transactionInfo,
        },
        {
          key: '/globallock/list',
          label: globalLockInfo,
        },
        {
          key: '/sagastatemachinedesigner',
          label: sagaStatemachineDesigner,
        },
      ],
      header: 'Seata',
      onItemClick: (key: string) => history.push(key),
    };
  }

  get router() {
    return (
      <Router history={history}>
        <Switch>
          <Route path="/login" component={Login} />
          <Layout
            nav={({ location }: any) => (
              <>
                <div
                  style={{
                    height: 'calc(100% - 100px)',
                    minHeight: '300px',
                  }}
                >
                  <CCConsoleMenu {...this.menu} activeKey={location.pathname} />
                </div>
                <div
                  style={{
                    backgroundColor: '#c2ccd0',
                    height: '100px',
                    textAlign: 'center',
                    paddingTop: '20px',
                    paddingBottom: '20px',
                  }}
                >
                  <span>Apache Seata (Incubating)</span>
                  <br />
                  <br />
                  <span>Version:{this.state.version}</span>
                </div>
              </>
            )}
          >
            <Route path={'/'} exact render={() => <Redirect to="/transaction/list" />} />
            <Route
              path={'/sagastatemachinedesigner'}
              render={() => (
                <Iframe title={'Seata'} src={'./saga-statemachine-designer/designer.html'} />
              )}
            />
            {router.map((item) => (
              <Route key={item.path} {...item} />
            ))}
          </Layout>
        </Switch>
      </Router>
    );
  }

  render() {
    const { locale } = this.props;
    const { loading } = this.state;
    return (
      <Loading tip="loading..." visible={false} fullScreen {...loading}>
        <ConfigProvider locale={locale}>{this.router}</ConfigProvider>
      </Loading>
    );
  }
}

const mapStateToProps = (state: GlobalStateModel, ownProps: OwnProps): StateToPropsType => ({
  ...state.locale,
});

const mapDispatchToProps = (dispatch: Dispatch): DispathToPropsType => ({
  changeLanguage: (lang) => changeLanguage(lang)(dispatch),
});

export default connect(mapStateToProps, mapDispatchToProps)(App as any);
