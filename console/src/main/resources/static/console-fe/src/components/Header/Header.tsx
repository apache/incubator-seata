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
import { withRouter, RouteComponentProps } from 'react-router-dom';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';
import { ConfigProvider, Dropdown, Menu } from '@alicloud/console-components';
import siteConfig from '../../config';
import {
  changeLanguage,
  IChangeLanguage,
  LocaleStateModel,
  getCurrentLanguage,
  zhCnKey,
  enUsKey,
} from '@/reducers/locale';
import { GlobalStateModel } from '@/reducers';
import { AUTHORIZATION_HEADER } from '@/contants';

import './index.scss';

type StateToPropsType = LocaleStateModel;

type DispathToPropsType = {
  changeLanguage: (lang: string) => void;
};

export type PropsType = StateToPropsType &
  DispathToPropsType &
  RouteComponentProps & {
    locale: any;
  };

type StateType = {};

class Header extends React.Component<PropsType, StateType> {
  static displayName = 'Header';

  static propTypes = {
    locale: PropTypes.object,
    history: PropTypes.object,
    location: PropTypes.object,
    language: PropTypes.string,
    changeLanguage: PropTypes.func,
  };

  switchLang = () => {
    const { changeLanguage } = this.props;
    const currentLanguage: string = getCurrentLanguage();
    let lang: string = currentLanguage === enUsKey ? zhCnKey : enUsKey;
    changeLanguage(lang);
  };

  logout = () => {
    window.localStorage.clear();
    this.props.history.push('/login');
  };

  getUsername = () => {
    const token = window.localStorage.getItem(AUTHORIZATION_HEADER);
    if (token) {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace('-', '+').replace('_', '/');
      const parsedToken = JSON.parse(window.atob(base64));
      return parsedToken.sub;
    }
    return '';
  };

  render() {
    const {
      locale = {},
      language = enUsKey,
      location: { pathname },
    } = this.props;
    console.log('props:', this.props);
    const {
      home,
      docs,
      blog,
      community,
      download,
      sagaStatemachineDesigner,
      languageSwitchButton,
    } = locale;
    const BASE_URL =
      language === enUsKey ? 'https://seata.apache.org/' : 'https://seata.apache.org/zh-cn/';
    const NAV_MENU = [
      { id: 1, title: home, link: BASE_URL },
      { id: 2, title: docs, link: `${BASE_URL}docs/overview/what-is-seata/` },
      { id: 3, title: blog, link: `${BASE_URL}blog` },
      { id: 4, title: community, link: `${BASE_URL}community` },
      { id: 5, title: download, link: `${BASE_URL}unversioned/download/seata-server` },
    ];
    return (
      <header className="header-container header-container-primary">
        <div className="header-body">
          <a href={BASE_URL} target="_blank" rel="noopener noreferrer">
            <img
              src="img/seata_logo.png"
              className="logo"
              alt={siteConfig.projectName}
              title={siteConfig.projectName}
            />
          </a>
          {/* if is login page, we will show logout */}
          {pathname !== '/login' && (
            <Dropdown align="tc bc" trigger={<div className="logout">{this.getUsername()}</div>}>
              <Menu>
                <Menu.Item style={{ textAlign: 'center' }} onClick={this.logout}>
                  {locale.logout}
                </Menu.Item>
              </Menu>
            </Dropdown>
          )}
          <span className="language-switch language-switch-primary" onClick={this.switchLang}>
            {languageSwitchButton}
          </span>
          <div className="header-menu header-menu-open">
            <ul>
              {NAV_MENU.map(item => (
                <li key={item.id} className="menu-item menu-item-primary">
                  <a href={item.link} target="_blank" rel="noopener noreferrer">
                    {item.title}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </header>
    );
  }
}

const mapStateToProps = (state: GlobalStateModel): StateToPropsType => ({
  ...state.locale,
});

const mapDispatchToProps = (dispatch: Dispatch): DispathToPropsType => ({
  changeLanguage: lang => {
    changeLanguage(lang)(dispatch);
  },
});

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(ConfigProvider.config(Header, {}))
);
