const LEVELS = {
  error: 0,
  warn: 1,
  info: 2
};

export function createLogger({ level = 'info', sink = console } = {}) {
  const threshold = LEVELS[level] ?? LEVELS.info;

  return {
    error(message, fields = {}) {
      if (threshold < LEVELS.error) return;
      sink.error?.(format(message, fields));
    },
    warn(message, fields = {}) {
      if (threshold < LEVELS.warn) return;
      sink.warn?.(format(message, fields));
    },
    info(message, fields = {}) {
      if (threshold < LEVELS.info) return;
      sink.info?.(format(message, fields));
    }
  };
}

function format(message, fields) {
  const extras = Object.entries(fields)
    .filter(([, value]) => value !== undefined)
    .map(([key, value]) => `${key}=${String(value)}`)
    .join(' ');

  return extras.length > 0 ? `${message} ${extras}` : message;
}
