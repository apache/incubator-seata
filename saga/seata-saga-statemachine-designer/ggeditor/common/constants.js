export const FLOW_CONTAINER = 'J_FlowContainer';
export const MIND_CONTAINER = 'J_MindContainer';
export const KONI_CONTAINER = 'J_KoniContainer';
export const TOOLBAR_CONTAINER = 'J_ToolbarContainer';
export const MINIMAP_CONTAINER = 'J_MinimapContainer';
export const CONTEXT_MENU_CONTAINER = 'J_ContextMenuContainer';

export const FLOW_CLASS_NAME = 'Flow';
export const MIND_CLASS_NAME = 'Mind';
export const KONI_CLASS_NAME = 'Koni';

export const EVENT_BEFORE_ADD_PAGE = 'beforeAddPage';
export const EVENT_AFTER_ADD_PAGE = 'afterAddPage';

export const STATUS_CANVAS_SELECTED = 'canvas-selected';
export const STATUS_NODE_SELECTED = 'node-selected';
export const STATUS_EDGE_SELECTED = 'edge-selected';
export const STATUS_GROUP_SELECTED = 'group-selected';
export const STATUS_MULTI_SELECTED = 'multi-selected';

export const GRAPH_MOUSE_REACT_EVENTS = {
  click: 'Click',
  contextmenu: 'ContextMenu',
  dblclick: 'DoubleClick',
  drag: 'Drag',
  dragend: 'DragEnd',
  dragenter: 'DragEnter',
  dragleave: 'DragLeave',
  dragstart: 'DragStart',
  drop: 'Drop',
  mousedown: 'MouseDown',
  mouseenter: 'MouseEnter',
  mouseleave: 'MouseLeave',
  mousemove: 'MouseMove',
  mouseup: 'MouseUp',
};

export const GRAPH_OTHER_REACT_EVENTS = {
  afterchange: 'onAfterChange',
  afterchangesize: 'onAfterChangeSize',
  afterviewportchange: 'onAfterViewportChange',
  beforechange: 'onBeforeChange',
  beforechangesize: 'onBeforeChangeSize',
  beforeviewportchange: 'onBeforeViewportChange',
  keydown: 'onKeyDown',
  keyup: 'onKeyUp',
  mousewheel: 'onMouseWheel',
};

export const PAGE_REACT_EVENTS = {
  afteritemactived: 'onAfterItemActived',
  afteriteminactivated: 'onAfterItemInactivated',
  afteritemselected: 'onAfterItemSelected',
  afteritemunactived: 'onAfterItemInactivated',
  afteritemunselected: 'onAfterItemUnselected',
  beforeitemactived: 'onBeforeItemActived',
  beforeiteminactivated: 'onBeforeItemInactivated',
  beforeitemselected: 'onBeforeItemSelected',
  beforeitemunactived: 'onBeforeItemInactivated',
  beforeitemunselected: 'onBeforeItemUnselected',
  keyUpEditLabel: 'onKeyUpEditLabel',
};

export const EDITOR_REACT_EVENTS = {
  aftercommandexecute: 'onAfterCommandExecute',
  beforecommandexecute: 'onBeforeCommandExecute',
};

export const GRAPH_MOUSE_EVENTS = Object.keys(GRAPH_MOUSE_REACT_EVENTS);
export const GRAPH_OTHER_EVENTS = Object.keys(GRAPH_OTHER_REACT_EVENTS);
export const PAGE_EVENTS = Object.keys(PAGE_REACT_EVENTS);
export const EDITOR_EVENTS = Object.keys(EDITOR_REACT_EVENTS);
