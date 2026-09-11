import React, { useState } from 'react';
import { Row, Col } from 'antd';
import GGEditor from 'gg-editor';
import EditorMinimap from '../components/EditorMinimap';
import { FlowContextMenu } from '../components/EditorContextMenu';
import { FlowToolbar } from '../components/EditorToolbar';
import { FlowItemPanel } from '../components/EditorItemPanel';
import { FlowDetailPanel } from '../components/EditorDetailPanel';
import styles from './index.less';
import { WorkSpace } from './WorkSpace';

const FlowPage = () => {

  const [showJson, setShowJson] = useState(false);
  const [flowData, setFlowData] = useState({});

  return (
    <GGEditor className={styles.editor}>
      <Row type="flex" className={styles.editorHd}>
        <Col span={24}>
          <FlowToolbar setShowJson={setShowJson} />
        </Col>
      </Row>
      <Row type="flex" className={styles.editorBd}>
        <Col span={3} className={styles.editorSidebar}>
          <FlowItemPanel />
        </Col>
        <Col span={16} className={styles.editorContent}>
          <WorkSpace showJson={showJson} flowData={flowData} setFlowData={setFlowData} />
        </Col>
        <Col span={5} className={styles.editorSidebar}>
          <FlowDetailPanel />
          <EditorMinimap />
        </Col>
      </Row>
      <FlowContextMenu />
    </GGEditor>
  );
};

export default FlowPage;
