import React, { Fragment } from 'react';
import { Card, Form, Input, Select, Button } from 'antd';
import { withPropsAPI } from 'gg-editor';
import upperFirst from 'lodash/upperFirst';

const { Item } = Form;
const { Option } = Select;
const { TextArea } = Input;

const inlineFormItemLayout = {
  labelCol: {
    sm: { span: 5 },
  },
  wrapperCol: {
    sm: { span: 19 },
  },
};

let lastSelectedItem;

class DetailForm extends React.Component {
  get item() {
    const { propsAPI } = this.props;

    return propsAPI.getSelected()[0] ? propsAPI.getSelected()[0] : lastSelectedItem;
  }

  handleSubmit = (e) => {
    if (e && e.preventDefault) {
      e.preventDefault();
    }

    const { form, propsAPI } = this.props;
    const { executeCommand, update } = propsAPI;

    setTimeout(() => {
      form.validateFieldsAndScroll((err, values) => {
        if (err) {
          return;
        }

        const item = this.item;

        if (!item) {
          return;
        }

        if (values.stateProps) {
          values.stateProps = JSON.parse(values.stateProps);
        }

        lastSelectedItem = item;

        executeCommand(() => {
          update(item, {
            ...values,
          });
        });
      });
    }, 0);
  };

  renderEdgeShapeSelect = () => {
    return (
      <Select onChange={this.handleSubmit}>
        <Option value="flow-smooth">Smooth</Option>
        <Option value="flow-polyline">Polyline</Option>
        <Option value="flow-polyline-round">Polyline Round</Option>
      </Select>
    );
  };

  renderNodeDetail = () => {
    const { form } = this.props;
    const { label, stateId, stateType, stateProps } = this.item.getModel();

    return (
      <Fragment>
        <Item label="Label" {...inlineFormItemLayout}>
          {form.getFieldDecorator('label', {
            initialValue: label,
          })(<Input onBlur={this.handleSubmit} />)}
        </Item>
        <Item label="Id" {...inlineFormItemLayout}>
          {form.getFieldDecorator('stateId', {
            initialValue: stateId,
          })(<Input onBlur={this.handleSubmit} />)}
        </Item>
        <Item label="Type" {...inlineFormItemLayout}>
          {form.getFieldDecorator('stateType', {
            initialValue: stateType,
          })(<Input readOnly={true} />)}
        </Item>
        <Item label="Props" {...inlineFormItemLayout}>
          {form.getFieldDecorator('stateProps', {
            initialValue: JSON.stringify(stateProps, null, 2),
          })(<TextArea onBlur={this.handleSubmit} rows={16} />)}
        </Item>
        <a target="_blank" style={{ float: 'right' }} href="http://seata.io/zh-cn/docs/user/saga.html">How to fill the properties?</a>
      </Fragment >
    );
  };

  renderEdgeDetail = () => {
    const { form } = this.props;
    const { label = '', shape = 'flow-smooth', stateProps } = this.item.getModel();

    return (
      <Fragment>
        <Item label="Label" {...inlineFormItemLayout}>
          {form.getFieldDecorator('label', {
            initialValue: label,
          })(<Input onBlur={this.handleSubmit} />)}
        </Item>
        <Item label="Shape" {...inlineFormItemLayout}>
          {form.getFieldDecorator('shape', {
            initialValue: shape,
          })(this.renderEdgeShapeSelect())}
        </Item>
        <Item label="Props" {...inlineFormItemLayout}>
          {form.getFieldDecorator('stateProps', {
            initialValue: JSON.stringify(stateProps, null, 2),
          })(<TextArea onBlur={this.handleSubmit} rows={16} />)}
        </Item>
        <a target="_blank" style={{ float: 'right' }} href="http://seata.io/zh-cn/docs/user/saga.html">How to fill the properties?</a>
      </Fragment>
    );
  };

  renderGroupDetail = () => {
    const { form } = this.props;
    const { label = 'New Group' } = this.item.getModel();

    return (
      <Item label="Label" {...inlineFormItemLayout}>
        {form.getFieldDecorator('label', {
          initialValue: label,
        })(<Input onBlur={this.handleSubmit} />)}
      </Item>
    );
  };

  render() {
    const { type } = this.props;

    if (!this.item) {
      return null;
    }

    return (
      <Card type="inner" size="small" title={upperFirst(type)} bordered={false}>
        <Form onSubmit={this.handleSubmit}>
          {type === 'node' && this.renderNodeDetail()}
          {type === 'edge' && this.renderEdgeDetail()}
          {type === 'group' && this.renderGroupDetail()}
        </Form>
      </Card>
    );
  }
}

export default Form.create()(withPropsAPI(DetailForm));
