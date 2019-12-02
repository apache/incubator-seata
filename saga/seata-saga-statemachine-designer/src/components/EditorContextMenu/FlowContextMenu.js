import React from 'react';
import { NodeMenu, EdgeMenu, GroupMenu, MultiMenu, CanvasMenu, ContextMenu } from 'gg-editor';
import MenuItem from './MenuItem';
import styles from './index.less';

const FlowContextMenu = () => {
  return (
    <ContextMenu className={styles.contextMenu}>
      <NodeMenu>
        <MenuItem command="copy" />
        <MenuItem command="delete" />
      </NodeMenu>
      <EdgeMenu>
        <MenuItem command="delete" />
      </EdgeMenu>
      <GroupMenu>
        <MenuItem command="copy" />
        <MenuItem command="delete" />
      </GroupMenu>
      <MultiMenu>
        <MenuItem command="copy" />
        <MenuItem command="paste" />
        <MenuItem command="delete" />
      </MultiMenu>
      <CanvasMenu>
        <MenuItem command="undo" />
        <MenuItem command="redo" />
        <MenuItem command="pasteHere" icon="paste" text="Paste Here" />
      </CanvasMenu>
    </ContextMenu>
  );
};

export default FlowContextMenu;
