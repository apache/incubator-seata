import React from 'react';
import Editor from '@components/Base/Editor';
import {
  EDITOR_EVENTS,
  EDITOR_REACT_EVENTS,
  EVENT_BEFORE_ADD_PAGE,
  EVENT_AFTER_ADD_PAGE,
} from '@common/constants';
import { pick } from '@utils';
import Global from '@common/Global';
import GGEditorContext from '@common/context/GGEditorContext';
import PropsAPIContext from '@common/context/PropsAPIContext';
import PropsAPI from '@common/context/PropsAPIContext/propsAPI';

class GGEditor extends React.Component {
  static setTrackable(value) {
    Global.set('trackable', Boolean(value));
  }

  editor = null;

  get currentPage() {
    return this.editor.getCurrentPage();
  }

  constructor(props) {
    super(props);

    this.init();
    this.bindEvent();
  }

  addListener = (target, eventName, handler) => {
    if (typeof handler === 'function') target.on(eventName, handler);
  };

  handleBeforeAddPage = (func) => {
    this.editor.on(EVENT_BEFORE_ADD_PAGE, func);
  };

  handleAfterAddPage = (func) => {
    const { currentPage: page } = this;

    if (page) {
      func({ page });
      return;
    }

    this.editor.on(EVENT_AFTER_ADD_PAGE, func);
  };

  init() {
    this.editor = new Editor();
    this.ggEditor = {
      editor: this.editor,
      onBeforeAddPage: this.handleBeforeAddPage,
      onAfterAddPage: this.handleAfterAddPage,
    };
    this.propsAPI = new PropsAPI(this.editor);
  }

  bindEvent() {
    EDITOR_EVENTS.forEach((event) => {
      this.addListener(this.editor, [event], this.props[EDITOR_REACT_EVENTS[event]]);
    });
  }

  componentWillUnmount() {
    this.editor.destroy();
  }

  render() {
    const { children } = this.props;

    return (
      <GGEditorContext.Provider value={this.ggEditor}>
        <PropsAPIContext.Provider value={this.propsAPI}>
          <div {...pick(this.props, ['style', 'className'])}>{children}</div>
        </PropsAPIContext.Provider>
      </GGEditorContext.Provider>
    );
  }
}

export default GGEditor;
