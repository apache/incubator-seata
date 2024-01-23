/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import {
  useState,
  useMemo,
  useEffect,
} from '@bpmn-io/properties-panel/preact/hooks';

import {
  find,
  isArray,
  reduce,
} from 'min-dash';

import { PropertiesPanel as BasePropertiesPanel } from '@bpmn-io/properties-panel';

import PropertiesPanelContext from './PropertiesPanelContext';

import PanelHeaderProvider from './PanelHeaderProvider';
import PanelPlaceholderProvider from './PanelPlaceholderProvider';

// helpers //////////////////////////

function isImplicitRoot(element) {
  // Backwards compatibility for diagram-js<7.4.0, see https://github.com/bpmn-io/bpmn-properties-panel/pull/102
  return element && (element.isImplicit || element.id === '__implicitroot');
}

function findElement(elements, element) {
  return find(elements, (e) => e === element);
}

function elementExists(element, elementRegistry) {
  return element && elementRegistry.get(element.id);
}

/**
 * @param {Object} props
 * @param {djs.model.Base|Array<djs.model.Base>} [props.element]
 * @param {Injector} props.injector
 * @param { (djs.model.BaseSpec) => Array<PropertiesProvider> } props.getProviders
 * @param {Object} props.layoutConfig
 * @param {Object} props.descriptionConfig
 */
export default function PropertiesPanel(props) {
  const {
    element,
    injector,
    getProviders,
    layoutConfig,
    descriptionConfig,
  } = props;

  const canvas = injector.get('canvas');
  const elementRegistry = injector.get('elementRegistry');
  const eventBus = injector.get('eventBus');

  const [state, setState] = useState({
    selectedElement: element,
  });

  const { selectedElement } = state;

  /**
   * @param {djs.model.Base | Array<djs.model.Base>} e
   */
  const update = (e) => {
    if (!e) {
      return;
    }

    const newSelectedElement = e;

    setState({
      ...state,
      selectedElement: newSelectedElement,
    });

    // notify interested parties on property panel updates
    eventBus.fire('propertiesPanel.updated', {
      element: newSelectedElement,
    });
  };

  // (2) react on element changes

  // (2a) selection changed
  useEffect(() => {
    const onSelectionChanged = (e) => {
      const { newSelection = [] } = e;

      if (newSelection.length > 1) {
        return update(newSelection);
      }

      const newElement = newSelection[0];

      const rootElement = canvas.getRootElement();

      if (isImplicitRoot(rootElement)) {
        // TODO
      }

      update(newElement || rootElement);
      return null;
    };

    eventBus.on('selection.changed', onSelectionChanged);

    return () => {
      eventBus.off('selection.changed', onSelectionChanged);
    };
  }, []);

  // (2b) selected element changed
  useEffect(() => {
    const onElementsChanged = (e) => {
      const { elements } = e;

      const updatedElement = findElement(elements, selectedElement);

      if (updatedElement && elementExists(updatedElement, elementRegistry)) {
        update(updatedElement);
      }
    };

    eventBus.on('elements.changed', onElementsChanged);

    return () => {
      eventBus.off('elements.changed', onElementsChanged);
    };
  }, [selectedElement]);

  // (2c) root element changed
  useEffect(() => {
    const onRootAdded = (e) => {
      const { element: root } = e;

      if (isImplicitRoot(root)) {
        return;
      }

      update(root);
    };

    eventBus.on('root.added', onRootAdded);

    return () => {
      eventBus.off('root.added', onRootAdded);
    };
  }, [selectedElement]);

  // (2d) provided entries changed
  useEffect(() => {
    const onProvidersChanged = () => {
      update(selectedElement);
    };

    eventBus.on('propertiesPanel.providersChanged', onProvidersChanged);

    return () => {
      eventBus.off('propertiesPanel.providersChanged', onProvidersChanged);
    };
  }, [selectedElement]);

  // (3) create properties panel context
  const propertiesPanelContext = useMemo(() => ({
    selectedElement,
    injector,
    getService(type, strict) { return injector.get(type, strict); },
  }), [selectedElement, injector]);

  // (4) retrieve groups for selected element
  const providers = getProviders(selectedElement);

  const groups = useMemo(() => {
    return reduce(providers, (g, provider) => {
      // do not collect groups for multi element state
      if (isArray(selectedElement)) {
        return [];
      }
      const updater = provider.getGroups(selectedElement);

      return updater(g);
    }, []);
  }, [providers, selectedElement]);

  // (5) notify layout changes
  const onLayoutChanged = (layout) => {
    eventBus.fire('propertiesPanel.layoutChanged', {
      layout,
    });
  };

  // (6) notify description changes
  const onDescriptionLoaded = (description) => {
    eventBus.fire('propertiesPanel.descriptionLoaded', {
      description,
    });
  };

  return (
    <PropertiesPanelContext.Provider value={propertiesPanelContext}>
      <BasePropertiesPanel
        element={selectedElement}
        headerProvider={PanelHeaderProvider}
        placeholderProvider={PanelPlaceholderProvider}
        groups={groups}
        layoutConfig={layoutConfig}
        layoutChanged={onLayoutChanged}
        descriptionConfig={descriptionConfig}
        descriptionLoaded={onDescriptionLoaded}
        eventBus={eventBus}
      />
    </PropertiesPanelContext.Provider>
  );
}
