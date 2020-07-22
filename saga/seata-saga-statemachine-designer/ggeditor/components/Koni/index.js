import Editor from '@components/Base/Editor';
import {
  KONI_CONTAINER,
  KONI_CLASS_NAME,
  EVENT_BEFORE_ADD_PAGE,
  EVENT_AFTER_ADD_PAGE,
} from '@common/constants';
import Page from '@components/Page';
import withGGEditorContext from '@common/context/GGEditorContext/withGGEditorContext';

class Koni extends Page {
  static defaultProps = {
    data: {
      nodes: [],
      edges: [],
    },
  };

  get pageId() {
    const { editor } = this.props;

    return `${KONI_CONTAINER}_${editor.id}`;
  }

  initPage() {
    const { editor } = this.props;

    editor.emit(EVENT_BEFORE_ADD_PAGE, { className: KONI_CLASS_NAME });

    this.page = new Editor.Koni(this.config);

    editor.add(this.page);

    editor.emit(EVENT_AFTER_ADD_PAGE, { page: this.page });
  }
}

export default withGGEditorContext(Koni);
