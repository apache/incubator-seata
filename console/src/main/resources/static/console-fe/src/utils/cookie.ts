function getValue(key: string) {
  if (!document.cookie) return null;
  const list = document.cookie.split(';') || [];
  for (const item of list) {
    const [k = '', v = ''] = item.split('=');
    if (k.trim() === key) return v;
  }
  return null;
}

function setValue(key: string, value: string) {
  document.cookie = `${key}=${value}`;
}

export default { getValue, setValue };
