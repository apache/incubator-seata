import React from 'react';
import { Divider } from 'antd';
import { Toolbar } from 'gg-editor';
import ToolbarButton from './ToolbarButton';
import styles from './index.less';

const FlowToolbar = ({ setShowJson }) => {
  return (
    <Toolbar className={styles.toolbar}>
      <ToolbarButton command="undo" />
      <ToolbarButton command="redo" />
      <Divider type="vertical" />
      <ToolbarButton command="copy" />
      <ToolbarButton command="paste" />
      <ToolbarButton command="delete" />
      <Divider type="vertical" />
      <ToolbarButton command="zoomIn" icon="zoom-in" text="Zoom In" />
      <ToolbarButton command="zoomOut" icon="zoom-out" text="Zoom Out" />
      <ToolbarButton command="autoZoom" icon="fit-map" text="Fit Map" />
      <ToolbarButton command="resetZoom" icon="actual-size" text="Actual Size" />
      <Divider type="vertical" />
      <ToolbarButton command="toBack" icon="to-back" text="To Back" />
      <ToolbarButton command="toFront" icon="to-front" text="To Front" />
      <Divider type="vertical" />
      <ToolbarButton command="multiSelect" icon="multi-select" text="Multi Select" />
      <Divider type="vertical" />
      <ToolbarButton command="switchWorkspace" icon="iconjson" text="Json View" onClick={() => setShowJson(true)} />
      <ToolbarButton command="switchWorkspace" icon="icondesign" text="Designer View" onClick={() => setShowJson(false)} />
    </Toolbar>
  );
};

export default FlowToolbar;
