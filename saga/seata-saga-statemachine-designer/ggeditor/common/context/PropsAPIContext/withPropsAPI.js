import React from 'react';
import PropsAPIContext from '@common/context/PropsAPIContext';

export default function (WrappedComponent) {
  class InjectPropsAPI extends React.Component {
    render() {
      const { forwardRef, ...rest } = this.props;

      return (
        <PropsAPIContext.Consumer>
          {propsAPI => <WrappedComponent ref={forwardRef} {...rest} propsAPI={propsAPI} />}
        </PropsAPIContext.Consumer>
      );
    }
  }

  return React.forwardRef((props, ref) => <InjectPropsAPI {...props} forwardRef={ref} />);
}
