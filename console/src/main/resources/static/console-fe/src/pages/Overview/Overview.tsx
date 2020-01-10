import React from 'react';
import { ConfigProvider, Table, Input, Select, Button } from '@alicloud/console-components';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';
import { withRouter } from 'react-router-dom';
import request from 'utils/request';
import PropTypes from 'prop-types';
import { IGlobalStateType } from '@/reducers';
import { IOverviewStateType, getData } from '@/reducers/overview'
import './index.scss';
import Page from '@/components/Page';

type StateToPropsType = IOverviewStateType;

type DispathToPropsType = {
  getData: () => void
};

type PropsType = {
  locale: any
} & StateToPropsType & DispathToPropsType;

class Overview extends React.Component<PropsType> {
  static displayName = 'Overview';

  constructor(props: PropsType) {
    super(props);
  }
  componentDidMount() {
    const { getData } = this.props;
    getData();
  }
  render() {
    const { locale = {}, getData } = this.props;
    const { title, subTitle, search } = locale;
    return (
      <Page title={title} breadcrumbs={[
        {
          link: '/',
          text: title,
        },
        {
          text: subTitle,
        }
      ]}>
        <div>
          <Input />
          <Button type="primary" className="ml-8" onClick={getData}>{search}</Button>
        </div>
        <Table className="mt-16" dataSource={[{id: 1, name: 'seata'}, {id: 2, name: 'gts'}]}>
          <Table.Column title="id" dataIndex="id"/>
          <Table.Column title="name" dataIndex="name"/>
        </Table>
      </Page>
    );
  }
}

const mapStateToProps = (state: IGlobalStateType): StateToPropsType => ({
  ...state.overview
});

const mapDispatchToProps = (dispatch: Dispatch): DispathToPropsType => ({
  getData: () => (getData()(dispatch))
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(ConfigProvider.config(Overview, {})));