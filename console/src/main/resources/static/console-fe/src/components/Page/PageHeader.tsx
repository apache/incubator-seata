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

import React, { PureComponent } from 'react';
import styled, { css } from 'styled-components';
import { Breadcrumb } from '@alicloud/console-components';
import _ from 'lodash';

const PageHeaderWrapper = styled.div`
  margin: 16px 0;
`;

const Title = styled.h3`
  font-size: 28px;
  font-weight: 400;
  margin: 0px;
  margin-top: 16px;
  vertical-align: middle;
`;

export default class PageHeader extends PureComponent<any> {
  render() {
    const { title, breadcrumbs = [], separator = '/' } = this.props;
    return (
      <PageHeaderWrapper>
        <Breadcrumb separator={separator}>
          {_.map(breadcrumbs, ({ link, text }: {link: string, text: string}) => (
            <Breadcrumb.Item key={text} link={link}>
              {text}
            </Breadcrumb.Item>
          ))}
        </Breadcrumb>
        <Title>{title}</Title>
      </PageHeaderWrapper>
    );
  }
}
