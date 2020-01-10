import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';
import { Router, Route, Switch, Redirect, RouteComponentProps } from 'react-router-dom';
import { ConfigProvider, Loading } from '@alicloud/console-components';
import { createHashHistory, History } from "history";
import CCConsoleMenu from '@alicloud/console-components-console-menu';
import { IGlobalStateType } from '@/reducers';
import { changeLanguage, ILocaleState, getCurrentLanguage } from '@/reducers/locale';
import Layout from '@/layout';
import Login from '@/pages/Login';
import router from '@/router';
import { ILocale } from '@/locales';

export const history: History = createHashHistory();

type OwnProps = RouteComponentProps;

type StateToPropsType = ILocaleState;

type DispathToPropsType = {
    changeLanguage: (lang: string) => void
};

type AppPropsType = StateToPropsType & DispathToPropsType & OwnProps;

type AppStateType = {
    loading: object;
}

class App extends React.Component<AppPropsType, AppStateType> {
    static propTypes = {
        locale: PropTypes.object,
        changeLanguage: PropTypes.func,
    };

    state: AppStateType = {
        loading: {},
    };

    constructor(props: AppPropsType) {
        super(props);
    }

    componentDidMount() {
        console.log('this.props: ', this.props, history);
        const language: string = getCurrentLanguage();
        this.props.changeLanguage(language);
    }

    get menu() {
        const { locale }: { locale: ILocale } = this.props;
        const { MenuRouter = {} } = locale;
        const { overview } = MenuRouter;
        return {
            items: [
                {
                    key: '/Overview',
                    label: overview,
                },
            ],
            header: 'Seata',
            onItemClick: (key: string) => history.push(key)
        }
    }

    get router() {
        return (
            <Router history={history}>
                <Switch>
                    <Route path="/login" component={Login} />
                    <Layout nav={({ location }: any) => <CCConsoleMenu  {...this.menu} activeKey={location.pathname} />}>
                        <Route path={'/'} exact render={() => <Redirect to="/Overview" />} />
                        {router.map(item => (
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
            <Loading
                tip="loading..."
                visible={false}
                fullScreen
                {...loading}
            >
                <ConfigProvider locale={locale}>
                    {this.router}
                </ConfigProvider>
            </Loading>
        );
    }
}


const mapStateToProps = (state: IGlobalStateType, ownProps: OwnProps): StateToPropsType => ({
    ...state.locale
});

const mapDispatchToProps = (dispatch: Dispatch): DispathToPropsType => ({
    changeLanguage: (lang) => (changeLanguage(lang)(dispatch))
});

export default connect(mapStateToProps, mapDispatchToProps)(App as any);