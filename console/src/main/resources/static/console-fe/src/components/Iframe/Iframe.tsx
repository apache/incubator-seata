/**
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import './index.scss';

export type PropsType = RouteComponentProps & {
  title?: string;
  src?: string;
  srcDoc?: string;
};

type StateType = {};

class Iframe extends React.PureComponent<PropsType, StateType> {
  static displayName = 'Iframe';

  static propTypes = {
    title: PropTypes.string,
    src: PropTypes.string,
    srcDoc: PropTypes.string,
  };

  render() {
    const { title, src, srcDoc } = this.props;
    return (
      <iframe
        title={title}
        src={src}
        srcDoc={srcDoc}
        style={{
          width: '100%',
          border: '0px',
          height: '98%',
          overflow: 'auto',
        }}
        sandbox="allow-same-origin allow-scripts allow-popups allow-forms"
      />
    );
  }
}

export default withRouter(Iframe);
