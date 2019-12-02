import React from 'react';
import G6 from '@antv/g6';
import { pick } from '@utils';
import { MINIMAP_CONTAINER } from '@common/constants';
import withGGEditorContext from '@common/context/GGEditorContext/withGGEditorContext';

require('@antv/g6/build/plugin.tool.minimap');

const { Minimap: G6Minimap } = G6.Components;

class Minimap extends React.Component {
  minimap = null;

  get containerId() {
    const { editor } = this.props;

    return `${MINIMAP_CONTAINER}_${editor.id}`;
  }

  get currentPage() {
    const { editor } = this.props;

    return editor.getCurrentPage();
  }

  constructor(props) {
    super(props);

    this.bindEvent();
  }

  componentDidMount() {
    this.init();
    this.bindPage();
  }

  init() {
    const {
      container = this.containerId,
      width,
      height,
      viewportWindowStyle,
      viewportBackStyle,
    } = this.props;

    const { clientWidth, clientHeight } = document.getElementById(container);

    this.minimap = new G6Minimap({
      container,
      width: width || clientWidth,
      height: height || clientHeight,
      viewportWindowStyle,
      viewportBackStyle,
    });

    this.minimap.getGraph = () => this.currentPage.getGraph();
  }

  bindPage() {
    if (!this.minimap || !this.currentPage) {
      return;
    }

    const graph = this.currentPage.getGraph();

    this.minimap.bindGraph(graph);
    this.minimap.debounceRender();
  }

  bindEvent() {
    const { onAfterAddPage } = this.props;

    onAfterAddPage(() => {
      this.bindPage();
    });
  }

  render() {
    const { container } = this.props;

    if (container) {
      return null;
    }

    return <div id={this.containerId} {...pick(this.props, ['style', 'className'])} />;
  }
}

export default withGGEditorContext(Minimap);
