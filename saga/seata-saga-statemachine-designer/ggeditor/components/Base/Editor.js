import GGEditorCore from 'gg-editor-core';
import { EVENT_BEFORE_ADD_PAGE } from '@common/constants';
import track from '@helpers/track';
import { uniqueId } from '@utils';

export default class Editor extends GGEditorCore {
  constructor(options) {
    super(options);

    this.id = uniqueId();

    this.on(EVENT_BEFORE_ADD_PAGE, ({ className }) => {
      track({ c1: className });
    });
  }
}
