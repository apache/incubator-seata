import merge from 'lodash/merge';
import pick from 'lodash/pick';
import uniqueId from 'lodash/uniqueId';
import upperFirst from 'lodash/upperFirst';

const toQueryString = obj => Object.keys(obj).map(key => `${encodeURIComponent(key)}=${encodeURIComponent(obj[key])}`).join('&');

export {
  merge,
  pick,
  toQueryString,
  uniqueId,
  upperFirst,
};
