import React from 'react';

class Menu extends React.Component {
  static create = function (type) {
    return class TypedMenu extends Menu {
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
    const { children } = this.props;
    const { type } = this;

    return (
      <div className="menu" data-status={`${type}-selected`}>
        {children}
      </div>
    );
  }
}

export default Menu;
