/// <reference types="react" />
// tslint:disable

declare const __mock__: boolean;


declare module '*.svg' {
  const SvgIcon: React.ComponentClass<any>;
  export default SvgIcon;
}

declare module 'lodash';