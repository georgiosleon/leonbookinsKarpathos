import test from 'node:test';
import assert from 'node:assert/strict';
import config from '../vite.config.js';

test('dev proxy covers the backend paths the SPA actually uses', () => {
  const proxy = config.server.proxy;

  for (const path of ['/api', '/images', '/login', '/logout', '/css', '/js']) {
    assert.equal(proxy[path], 'http://localhost:8080', `missing proxy for ${path}`);
  }
  assert.equal(proxy['/booking'], undefined);
  assert.equal(proxy['/csrf'], undefined);
});
