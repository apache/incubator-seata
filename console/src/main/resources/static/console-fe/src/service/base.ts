import request from '@/utils/request';

export async function fetchData() {
  return await request('/api/fetchData', {
    method: 'get',
  });
}
