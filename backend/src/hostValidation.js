import net from 'node:net';

const INVALID_HOST = { ok: false };

export function validateHostInput(host) {
  if (typeof host !== 'string' || host.length === 0) {
    return INVALID_HOST;
  }

  if (host !== host.trim() || host !== host.toLowerCase()) {
    return INVALID_HOST;
  }

  if (host.length > 253 || host.endsWith('.')) {
    return INVALID_HOST;
  }

  if (net.isIP(host) !== 0) {
    return INVALID_HOST;
  }

  if (/[/:?#@]/.test(host)) {
    return INVALID_HOST;
  }

  const labels = host.split('.');
  if (labels.length < 2) {
    return INVALID_HOST;
  }

  const validLabels = labels.every((label) => (
    label.length >= 1 &&
    label.length <= 63 &&
    /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/.test(label)
  ));

  if (!validLabels) {
    return INVALID_HOST;
  }

  return {
    ok: true,
    host
  };
}
