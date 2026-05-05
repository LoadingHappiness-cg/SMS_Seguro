import test from 'node:test';
import assert from 'node:assert/strict';

import { validateHostInput } from '../src/hostValidation.js';

test('accepts a lowercase registrable host', () => {
  assert.equal(validateHostInput('example.com').ok, true);
  assert.equal(validateHostInput('sub.example.co.uk').ok, true);
  assert.equal(validateHostInput('xn--pple-43d.com').ok, true);
});

test('rejects uppercase, schemes, paths, ports, ips, and malformed labels', () => {
  const invalidHosts = [
    '',
    'Example.com',
    'http://example.com',
    'example.com/path',
    'example.com:443',
    '192.168.0.1',
    'localhost',
    '-bad.example',
    'bad-.example',
    'bad..example',
    ' example.com '
  ];

  invalidHosts.forEach((host) => {
    assert.equal(validateHostInput(host).ok, false, host);
  });
});
