import React from 'react';

class Command extends React.Component {
  render() {
    const { name, children } = this.props;

    return (
      <div className="command" data-command={name}>
        {children}
      </div>
    );
  }
}

export default Command;
