const global = {
  trackable: process.env.NODE_ENV === 'production',
  version: process.env.GG_EDITOR_VERSION,
};

export default {
  get(key) {
    return global[key];
  },
  set(key, value) {
    global[key] = value;
  },
};
