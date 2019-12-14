import React from 'react';
import { withPropsAPI, Flow } from 'gg-editor';
import styles from './index.less';
import CodeMirror from 'react-codemirror';
import 'codemirror/lib/codemirror.css'
import 'codemirror/mode/javascript/javascript'

const nodeIndexes = {
  Start: 1,
  ServiceTask: 1,
  Compensation: 1,
  Choice: 1,
  Succeed: 1,
  Fail: 1,
  Catch: 1,
  CompensationTrigger: 1,
  SubStateMachine: 1
}

class WorkSpaceBase extends React.Component {

  toGGEditorData(dataMap) {
    const data = { nodes: [], edges: [] };
    Object.values(dataMap).map(value => {

      if (value.type && value.type == 'node') {
        data.nodes[data.nodes.length] = value;
      }
      else if (value.source && value.target) {
        data.edges[data.edges.length] = value;
      }
    });
    return data;
  };

  changeFlowData(param) {
    const { propsAPI, setFlowData } = this.props;
    const { executeCommand, update } = propsAPI;

    if (param.action == 'add' && param.item.type == 'edge') {
      if (param.item.target && param.item.target.model && param.item.target.model.stateType == 'Compensation') {
        executeCommand(() => {
          update(param.item, {
            style: {
              lineDash: "4",
            }
          });
        });
      }
      else if (param.item.source && param.item.source.model) {
        if (param.item.source.model.stateType == 'Choice') {
          const choiceLinePropsTemplate = {
            "Expression": "",
            "Default": false
          };
          executeCommand(() => {
            update(param.item, {
              stateProps: choiceLinePropsTemplate
            });
          });
        }
        else if (param.item.source.model.stateType == 'Catch') {
          const catchLinePropsTemplate = {
            "Exceptions": [""]
          };
          executeCommand(() => {
            update(param.item, {
              stateProps: catchLinePropsTemplate
            });
          });
        }
      }
    }

    if (param.action == 'add' && param.item.type == 'node' && param.item.model) {
      param.item.model.stateId = param.item.model.stateId + nodeIndexes[param.item.model.stateType]++;
      if (param.item.model.stateType == 'ServiceTask'
        || param.item.model.stateType == 'Compensation'
        || param.item.model.stateType == 'SubStateMachine') {
        param.item.model.label = param.item.model.stateId;
      }
    }

    param.item && setFlowData(this.toGGEditorData(param.item.dataMap));
  };

  render() {
    const { showJson, flowData, setFlowData } = this.props;

    return <>
      {showJson && <CodeMirror value={JSON.stringify(flowData, null, 2)} options={{
        lineNumbers: true,
        mode: 'javascript'
      }} onChange={(newValue) => {
        setFlowData(JSON.parse(newValue));
      }}></CodeMirror>}
      <Flow className={styles.flow + (showJson ? ' ' + styles.hidden : '')} data={flowData} onAfterChange={this.changeFlowData.bind(this)} />
    </>
  }
}

export const WorkSpace = withPropsAPI(WorkSpaceBase)