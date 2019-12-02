import Editor from '@components/Base/Editor';
import {
  FLOW_CONTAINER,
  FLOW_CLASS_NAME,
  EVENT_BEFORE_ADD_PAGE,
  EVENT_AFTER_ADD_PAGE,
} from '@common/constants';
import Page from '@components/Page';
import withGGEditorContext from '@common/context/GGEditorContext/withGGEditorContext';

class Flow extends Page {
  static defaultProps = {
    data: {
      nodes: [],
      edges: [],
    },
  };

  get pageId() {
    const { editor } = this.props;

    return `${FLOW_CONTAINER}_${editor.id}`;
  }

  initPage() {
    const { editor } = this.props;

    editor.emit(EVENT_BEFORE_ADD_PAGE, { className: FLOW_CLASS_NAME });

    this.page = new Editor.Flow(this.config);

    editor.add(this.page);

    editor.emit(EVENT_AFTER_ADD_PAGE, { page: this.page });
  }
}

export default withGGEditorContext(Flow);
