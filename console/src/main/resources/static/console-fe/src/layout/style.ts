import styled from 'styled-components';

export const Bodyer = styled.div`
    display: flex;
    height: calc(100vh - 66px);
    margin-top: 66px;
`;

export const NavBar = styled.div`
    height: 100%;
    width: 208px;
    min-width: 208px;
    position: relative;
    z-index: 100;
    flex: 0 1 auto;
    transition: width 0.3s ease-in-out 0s, min-width 0.3s ease-in-out 0s;
`;

export const Content = styled.div`
    height: 100%;
    overflow-y: auto;
    flex: 1 1 auto;
    padding: 0px 24px;
`;