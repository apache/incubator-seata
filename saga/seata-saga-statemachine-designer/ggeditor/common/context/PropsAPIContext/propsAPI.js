class PropsAPI {
  editor = null;

  constructor(editor) {
    this.editor = editor;

    ['executeCommand'].forEach((key) => {
      this[key] = (...params) => this.editor[key](...params);
    });

    ['read', 'save', 'add', 'find', 'update', 'remove', 'getSelected'].forEach((key) => {
      this[key] = (...params) => this.currentPage[key](...params);
    });
  }

  get currentPage() {
    return this.editor.getCurrentPage();
  }
}

export default PropsAPI;
