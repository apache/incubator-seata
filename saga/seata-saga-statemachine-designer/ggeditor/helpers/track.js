import Global from '@common/Global';
import { toQueryString } from '@utils';

const BASE_URL = 'http://gm.mmstat.com/fsp.1.1';

const track = (options) => {
  const trackable = Global.get('trackable');
  const version = Global.get('version');

  if (!trackable) {
    return;
  }

  const { location, navigator } = window;
  const image = new Image();
  const params = toQueryString({
    pid: 'ggeditor',
    code: '11',
    msg: 'syslog',
    page: `${location.protocol}//${location.host}${location.pathname}`,
    hash: location.hash,
    ua: navigator.userAgent,
    rel: version,
    ...options,
  });

  image.src = `${BASE_URL}?${params}`;
};

export default (options) => {
  setTimeout(() => {
    track(options);
  }, 1000);
};
