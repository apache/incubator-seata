import React from 'react';
import Editor from '@components/Base/Editor';
import { upperFirst } from '@utils';
import withGGEditorContext from '@common/context/GGEditorContext/withGGEditorContext';

class Register extends React.Component {
  static create = function (type) {
    class TypedRegister extends Register {
      constructor(props) {
        super(props, type);
      }
    }

    return withGGEditorContext(TypedRegister);
  }

  constructor(props, type) {
    super(props);

    this.type = type;

    this.bindEvent();
  }

  bindEvent() {
    const { type } = this;
    const { onBeforeAddPage } = this.props;

    onBeforeAddPage(({ className }) => {
      let host = Editor[className];
      let keys = ['name', 'config', 'extend'];

      if (type === 'command') {
        host = Editor;
      }

      if (type === 'behaviour') {
        keys = ['name', 'behaviour', 'dependences'];
      }

      const args = keys.map(key => this.props[key]);

      host[`register${upperFirst(type)}`](...args);
    });
  }

  render() {
    return null;
  }
}

export const RegisterNode = Register.create('node');
export const RegisterEdge = Register.create('edge');
export const RegisterGroup = Register.create('group');
export const RegisterGuide = Register.create('guide');
export const RegisterCommand = Register.create('command');
export const RegisterBehaviour = Register.create('behaviour');
