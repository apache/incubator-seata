import React from 'react';
import { pick } from '@utils';
import { STATUS_CANVAS_SELECTED } from '@common/constants';
import withGGEditorContext from '@common/context/GGEditorContext/withGGEditorContext';
import Panel from './Panel';

class DetailPanel extends React.Component {
  state = {
    status: '',
  }

  constructor(props) {
    super(props);

    this.bindEvent();
  }

  bindEvent() {
    const { onAfterAddPage } = this.props;

    onAfterAddPage(({ page }) => {
      this.setState({
        status: STATUS_CANVAS_SELECTED,
      });

      page.on('statuschange', ({ status }) => {
        this.setState({ status });
      });
    });
  }

  render() {
    const { children } = this.props;
    const { status } = this.state;

    if (!status) {
      return null;
    }

    return (
      <div {...pick(this.props, ['style', 'className'])}>
        {
          React.Children.toArray(children).map(child => React.cloneElement(child, {
            status,
          }))
        }
      </div>
    );
  }
}

export const NodePanel = Panel.create('node');
export const EdgePanel = Panel.create('edge');
export const GroupPanel = Panel.create('group');
export const MultiPanel = Panel.create('multi');
export const CanvasPanel = Panel.create('canvas');

export default withGGEditorContext(DetailPanel);
