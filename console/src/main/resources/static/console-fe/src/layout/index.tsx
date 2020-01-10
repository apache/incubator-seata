import React from 'react';
import Header from '@/components//Header';
import { Bodyer, NavBar, Content } from './style';
import './index.scss';

export type PropsType = {
    children: any
    nav: any;
    location?: any;
}

export default class Layout extends React.PureComponent<PropsType> {
    render() {
        console.log('this.props: Layout: ', this.props);
        const { children, nav, location } = this.props;
        return (
            <div>
                <Header />
                <Bodyer>
                    <NavBar className="navbar">{nav({location})}</NavBar>
                    <Content>{children}</Content>
                </Bodyer>
            </div>
        )
    }
}