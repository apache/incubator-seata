declare module 'gg-editor' {
  
  export interface Align {
    line?: any
    item?: boolean | 'horizontal' | 'vertical' | 'center'
    grid?: boolean | 'cc' | 'tl'
  }

  export interface Grid {
    cell?: number
    line?: any
  }

  export interface Shortcut {
    clear?: boolean
    selectAll?: boolean
    undo?: boolean
    redo?: boolean
    delete?: boolean
    zoomIn?: boolean
    zoomOut?: boolean
    autoZoom?: boolean
    resetZoom?: boolean
    toFront?: boolean
    toBack?: boolean
    copy?: boolean
    paste?: boolean
    multiSelect?: boolean
    addGroup?: boolean
    unGroup?: boolean
    append?: boolean
    appendChild?: boolean
    collaspeExpand?: boolean
  }

  interface ReactProps {
    style?: React.CSSProperties
    className?: string
  }

  export interface BasicProps extends ReactProps {
    /** 初始数据 */
    data?: any
    /** G6的配置项 @see https://www.yuque.com/antv/g6/graph  */
    graph?: any
    /** 快捷键配置，内置命令 */
    shortcut?: Shortcut
  }

  export interface FlowProps extends BasicProps {
    /** 对齐配置 */
    align?: Align
    /** 网格线配置 */
    grid?: Grid
    /** default: true */
    noEndEdge?: boolean
  }

  export interface MindProps extends BasicProps {

  }

  export interface KoniProps extends BasicProps {

  }

  export interface CommondProps {
    name: string
  }

  export interface MninimapProps extends ReactProps {
    /** 容器 id */
    container?: string
    width?: number
    height?: number
    /** 视窗样式，参考 G 绘图属性 */
    viewportWindowStyle?: any
    /** 背景样式，参考 G 绘图属性 */
    viewportBackStyle?: any
  }

  export interface ItemPanelProps extends ReactProps {
    /** 元素类型，可选类型：node edge */
    type: string
    /** 元素尺寸，书写格式：50*50 */
    size: string
    /** 元素形状，内置形状：node、edge */
    shape: string
    /** 元素初始 model */
    model?: any
    /** 元素概览 src */
    src: string
  }

  export interface RegisterProps extends ReactProps{
    /** 节点名称 */
    name?: string
    /** 节点配置 */
    config?: any
    /** 继承图形 */
    extend?: string
  }

  export interface RegisterBehaviourProps extends ReactProps{
    /** 行为名称 */
    name?: string
    /** 行为配置  function(page) */
    behaviour?: any
    /** 继承行为 */
    dependences?: string[]
  }

  export interface GGEditorEvent {
    action: 'add' | 'update' | 'remove' | 'changeData'
    item?: any
    shape?: any
    x?: number
    y?: number
    domX?: number
    domY?: number
    /** DOM 原生事件 */
    domeEvent?: any
    /** drag 拖动图项 */
    currentIem?: any
    /** drag 拖动图形 */
    currentShape?: any
    /** mouseleave dragleave 到达的图形 */
    toShape?: any
    /** mouseleave dragleave 到达的图项 */
    toItem?: any
  }

  /** 此类事件可以结合前缀 node、edge、group、guide、anchor 组合使用，例如： */
  export interface GraphMouseReactEventsProps {
    /** 鼠标左键点击事件 */
    onClick?: (e: GGEditorEvent) => void
    /** 鼠标左键双击事件 */
    onDoubleClick?: (e: GGEditorEvent) => void
    /** 鼠标移入事件 */
    onMouseEnter?: (e: GGEditorEvent) => void
    /** 鼠标移除事件 */
    onMouseLeave?: (e: GGEditorEvent) => void
    /** 鼠标按下事件 */
    onMouseDown?: (e: GGEditorEvent) => void
    /** 鼠标抬起事件 */
    onMouseUp?: (e: GGEditorEvent) => void
    /** 鼠标移动事件 */
    onMouseMove?: (e: GGEditorEvent) => void
    /** 鼠标开始拖拽事件 */
    onDragStart?: (e: GGEditorEvent) => void
    /** 鼠标拖拽事件 */
    onDrag?: (e: GGEditorEvent) => void
    /** 鼠标拖拽结束事件 */
    onDragEnd?: (e: GGEditorEvent) => void
    /** 鼠标拖拽进入事件 */
    onDragEnter?: (e: GGEditorEvent) => void
    /** 鼠标拖拽移出事件 */
    onDragLeave?: (e: GGEditorEvent) => void
    /** 鼠标拖拽放置事件 */
    onDrop?: (e: GGEditorEvent) => void
    /** 鼠标右键菜单事件 */
    onContextMenu?: (e: GGEditorEvent) => void
  }

  export interface GraphOtherReactEventsProps {
    /** 鼠标滚轮事件 */
    onMouseWheel?: (e: GGEditorEvent) => void
    /** 键盘按键按下事件 */
    onKeyDown?: (e: GGEditorEvent) => void
    /** 键盘按键抬起事件 */
    onKeyUp?: (e: GGEditorEvent) => void
    /** 子项数据变化前 */
    onBeforeChange?: (e: GGEditorEvent) => void
    /** 子项数据变化后 */
    onAfterChange?: (e: GGEditorEvent) => void
    /** 画布尺寸变化前 */
    onBeforeChangeSize?: (e: GGEditorEvent) => void
    /** 画布尺寸变化后 */
    onAfterChangeSize?: (e: GGEditorEvent) => void
    /** 视口变化前 */
    onBeforeViewportChange?: (e: GGEditorEvent) => void
    /** 视口变化后 */
    onAfterViewportChange?: (e: GGEditorEvent) => void
    /** 激活前 */
    onBeforeItemActived?: (e: GGEditorEvent) => void
  }

  export interface PageReactEventsProps {
    /** 激活后 */
    onAfterItemActived?: (e: GGEditorEvent) => void
    /** 取消激活前 */
    onBeforeItemInactivated?: (e: GGEditorEvent) => void
    /** 取消激活后 */
    onAfterItemInactivated?: (e: GGEditorEvent) => void
    /** 选中前 */
    onBeforeItemSelected?: (e: GGEditorEvent) => void
    /** 选中后 */
    onAfterItemSelected?: (e: GGEditorEvent) => void
    /** 取消选中前 */
    onBeforeItemUnselected?: (e: GGEditorEvent) => void
    /** 取消选中后 */
    onAfterItemUnselected?: (e: GGEditorEvent) => void
    /** 键盘按键抬起事件（节点编辑 */
    onKeyUpEditLabel?: (e: GGEditorEvent) => void
  }

  export interface EditorCommand {
    name: string
    queue: boolean
  }

  export interface PropsApi {
    propsApi: {
      executeCommand?(command: EditorCommand)
      read?(data: any)
      save?(): any
      add?(type: any, model: any)
      find?(id: any)
      update?(item: any, model: any)
      remove?(item: any)
      getSelected?()
    }
  }

  export interface EditorReactEventsProps {
    onAfterCommandExecute?: (e: EditorCommand) => void
    onBeforeCommandExecute?: (e: EditorCommand) => void
  }
  export default class GGEditor extends React.Component<ReactProps & EditorReactEventsProps, any> {
    static setTrackable(state: boolean)
  }

  /** 流程图  @see http://ggeditor.com/docs/api/flow.zh-CN.html */
  export const Flow: React.ComponentClass<FlowProps & GraphMouseReactEventsProps & GraphOtherReactEventsProps & PageReactEventsProps, any>

  /** 思维导图  @see http://ggeditor.com/docs/api/mind.zh-CN.html */
  export const Mind: React.ComponentClass<MindProps & GraphMouseReactEventsProps & GraphOtherReactEventsProps & PageReactEventsProps, any>

  /** 脑图 */
  export const Koni: React.ComponentClass<KoniProps & GraphMouseReactEventsProps & GraphOtherReactEventsProps & PageReactEventsProps, any>

  /** 此组件只能嵌套在 <Toolbar /> 或 <ContextMenu /> 组件内使用： @see http://ggeditor.com/docs/api/command.zh-CN.html */
  export const Command: React.ComponentClass<CommondProps, any>

  /** 不指定宽高的情况下则自动适应容器尺寸 @see http://ggeditor.com/docs/api/minimap.zh-CN.html */
  export const Minimap: React.ComponentClass<MninimapProps, any>

  /** 右键菜单，负责菜单显示隐藏，命令按钮绑定与可用禁用状态控制。 @see http://ggeditor.com/docs/api/contextMenu.zh-CN.html */
  export const ContextMenu: React.ComponentClass<BasicProps, any>

  /** 工具栏，负责命令按钮绑定与可用禁用状态控制。 @see http://ggeditor.com/docs/api/toolbar.zh-CN.html */
  export const Toolbar: React.ComponentClass<BasicProps, any>

  /** 元素面板栏  必需配合 <Item /> 组件使用，如果 <Item /> 包含 src 属性则自动显示元素概览图片。 @see http://ggeditor.com/docs/api/itemPanel.zh-CN.html */
  export const ItemPanel: React.ComponentClass<ReactProps, any>
  export const Item: React.ComponentClass<ItemPanelProps, any>

  /** 属性栏会自动根据不同页面状态显示对应面板，例如：选中节点时则只会显示 NodePanel @see http://ggeditor.com/docs/api/detailPanel.zh-CN.html */
  export const DetailPanel: React.ComponentClass<ReactProps, any>
  
  export const RegisterNode: React.ComponentClass<RegisterProps, any>
  export const RegisterEdge: React.ComponentClass<RegisterProps, any>
  export const RegisterGroup: React.ComponentClass<RegisterProps, any>
  export const RegisterCommand: React.ComponentClass<RegisterProps, any>
  export const RegisterBehaviour: React.ComponentClass<RegisterBehaviourProps, any>

  export const CanvasMenu: React.ComponentClass<ReactProps, any>
  export const EdgeMenu: React.ComponentClass<ReactProps, any>
  export const GroupMenu: React.ComponentClass<ReactProps, any>
  export const MultiMenu: React.ComponentClass<ReactProps, any>
  export const NodeMenu: React.ComponentClass<ReactProps, any>

  export const CanvasPanel: React.ComponentClass<ReactProps, any>
  export const EdgePanel: React.ComponentClass<ReactProps, any>
  export const GroupPanel: React.ComponentClass<ReactProps, any>
  export const MultiPanel: React.ComponentClass<ReactProps, any>
  export const NodePanel: React.ComponentClass<ReactProps, any>
  
  export const KoniCustomNode: React.ComponentClass<ReactProps & GraphMouseReactEventsProps & GraphOtherReactEventsProps & PageReactEventsProps, any>
  
  /** 这里会带一个 Props 属性 @see http://ggeditor.com/docs/api/propsAPI.zh-CN.html */
  export function withPropsAPI(com: React.ComponentClass<ReactProps, any>): React.ComponentClass<any, any>
}
