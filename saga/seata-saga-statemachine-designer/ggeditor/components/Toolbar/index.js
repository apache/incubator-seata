import React from 'react';
import Editor from '@components/Base/Editor';
import { pick } from '@utils';
import { TOOLBAR_CONTAINER } from '@common/constants';
import withGGEditorContext from '@common/context/GGEditorContext/withGGEditorContext';

class Toolbar extends React.Component {
  toolbar = null;

  get containerId() {
    const { editor } = this.props;

    return `${TOOLBAR_CONTAINER}_${editor.id}`;
  }

  constructor(props) {
    super(props);

    const { editor, onAfterAddPage } = props;

    onAfterAddPage(() => {
      this.toolbar = new Editor.Toolbar({
        container: this.containerId,
      });

      editor.add(this.toolbar);
    });
  }

  render() {
    const { children } = this.props;

    return (
      <div id={this.containerId} {...pick(this.props, ['style', 'className'])}>
        {children}
      </div>
    );
  }
}

export default withGGEditorContext(Toolbar);
