import React from 'react';
import { pick } from '@utils';

class Panel extends React.Component {
  static create = function (type) {
    return class TypedPanel extends Panel {
      constructor(props) {
        super(props, type);
      }
    };
  }

  constructor(props, type) {
    super(props);

    this.type = type;
  }

  render() {
    const { status, children } = this.props;
    const { type } = this;

    if (`${type}-selected` !== status) {
      return null;
    }

    return (
      <div {...pick(this.props, ['style', 'className'])}>
        {children}
      </div>
    );
  }
}

export default Panel;
