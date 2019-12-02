import React from 'react';
import { Tooltip } from 'antd';
import { Command } from 'gg-editor';
import upperFirst from 'lodash/upperFirst';
import IconFont from '../../common/IconFont';
import { IconFontExt } from '../../common/IconFont';
import styles from './index.less';

const ToolbarButton = (props) => {
  const { command, icon, text, onClick } = props;

  if (command == 'switchWorkspace') {
    return (
      <Tooltip className={styles.buttonExt}
        title={text || upperFirst(command)}
        onClick={onClick}
      >
        <IconFontExt type={`${icon || command}`} />
      </Tooltip>
    );
  }

  return (
    <Command name={command}>
      <Tooltip
        title={text || upperFirst(command)}
        placement="bottom"
        overlayClassName={styles.tooltip}
      >
        <IconFont type={`icon-${icon || command}`} />
      </Tooltip>
    </Command>
  );
};

export default ToolbarButton;
