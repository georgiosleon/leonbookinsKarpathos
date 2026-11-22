import { csrfHeaders, request } from './bookingApi.js';

export async function fetchUsers() {
  const response = await request('/api/v1/users');
  return response.json();
}

export async function fetchMe() {
  const response = await request('/api/v1/users/me');
  return response.json();
}

export async function createUser(user) {
  const csrf = await csrfHeaders();
  const response = await request('/api/v1/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...csrf },
    body: JSON.stringify({
      username: user.username,
      password: user.password,
      email: user.email,
      role: user.role,
      active: user.active,
    }),
  });
  return response.json();
}

export async function updateUser(user) {
  const csrf = await csrfHeaders();
  const payload = {
    username: user.username,
    email: user.email,
    role: user.role,
    active: user.active,
  };
  if (user.password && user.password.trim() !== '') {
    payload.password = user.password;
  }
  const response = await request(`/api/v1/users/${user.id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...csrf },
    body: JSON.stringify(payload),
  });
  return response.json();
}

export async function deleteUser(id) {
  const csrf = await csrfHeaders();
  await request(`/api/v1/users/${id}`, { method: 'DELETE', headers: csrf });
}
