import dgram from 'node:dgram';
import crypto from 'node:crypto';

const QUAD9_SERVERS = [
  { address: '9.9.9.9', port: 53 },
  { address: '149.112.112.112', port: 53 }
];

const DNS_CLASS_IN = 1;
const DNS_TYPE_A = 1;
const DNS_TYPE_AAAA = 28;
const DNS_RCODE_NXDOMAIN = 3;

export function createQuad9Resolver({
  timeoutMs = 800,
  servers = QUAD9_SERVERS
} = {}) {
  return {
    async lookupHost(host) {
      for (const server of servers) {
        try {
          const [aResponse, aaaaResponse] = await Promise.all([
            queryDns(host, DNS_TYPE_A, server, timeoutMs),
            queryDns(host, DNS_TYPE_AAAA, server, timeoutMs)
          ]);

          const dnsBlocked =
            isQuad9Blocked(aResponse) ||
            isQuad9Blocked(aaaaResponse);

          const resolvedIpCount = dnsBlocked
            ? 0
            : countUniqueAddresses([aResponse, aaaaResponse]);

          return { dnsBlocked, resolvedIpCount };
        } catch {
          continue;
        }
      }

      throw new Error('quad9_lookup_failed');
    }
  };
}

function isQuad9Blocked(response) {
  return response.rcode === DNS_RCODE_NXDOMAIN && response.authorityCount === 0;
}

function countUniqueAddresses(responses) {
  const addresses = new Set();

  responses.forEach((response) => {
    response.addresses.forEach((address) => addresses.add(address));
  });

  return addresses.size;
}

async function queryDns(host, type, server, timeoutMs) {
  const socket = dgram.createSocket('udp4');
  const queryId = crypto.randomInt(0, 65536);
  const packet = encodeQuestion(queryId, host, type);

  try {
    const responseBuffer = await new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        socket.close();
        reject(new Error('quad9_timeout'));
      }, timeoutMs);

      socket.once('error', (error) => {
        clearTimeout(timer);
        socket.close();
        reject(error);
      });

      socket.on('message', (message) => {
        const responseId = message.readUInt16BE(0);
        if (responseId !== queryId) return;

        clearTimeout(timer);
        socket.close();
        resolve(message);
      });

      socket.send(packet, server.port, server.address, (error) => {
        if (!error) return;

        clearTimeout(timer);
        socket.close();
        reject(error);
      });
    });

    return parseDnsResponse(responseBuffer);
  } finally {
    if (socket.connecting || socket.bound) {
      socket.close();
    }
  }
}

function encodeQuestion(id, host, type) {
  const qname = encodeQName(host);
  const buffer = Buffer.alloc(12 + qname.length + 4);

  buffer.writeUInt16BE(id, 0);
  buffer.writeUInt16BE(0x0100, 2);
  buffer.writeUInt16BE(1, 4);
  buffer.writeUInt16BE(0, 6);
  buffer.writeUInt16BE(0, 8);
  buffer.writeUInt16BE(0, 10);

  qname.copy(buffer, 12);
  let offset = 12 + qname.length;
  buffer.writeUInt16BE(type, offset);
  offset += 2;
  buffer.writeUInt16BE(DNS_CLASS_IN, offset);

  return buffer;
}

function encodeQName(host) {
  const labels = host.split('.');
  const parts = labels.map((label) => {
    const bytes = Buffer.from(label, 'ascii');
    return Buffer.concat([Buffer.from([bytes.length]), bytes]);
  });

  return Buffer.concat([...parts, Buffer.from([0])]);
}

function parseDnsResponse(buffer) {
  const questionCount = buffer.readUInt16BE(4);
  const answerCount = buffer.readUInt16BE(6);
  const authorityCount = buffer.readUInt16BE(8);
  const flags = buffer.readUInt16BE(2);
  const rcode = flags & 0x000f;

  let offset = 12;

  for (let index = 0; index < questionCount; index += 1) {
    offset = skipQuestion(buffer, offset);
  }

  const addresses = [];
  for (let index = 0; index < answerCount; index += 1) {
    const record = parseRecord(buffer, offset);
    offset = record.nextOffset;

    if (record.type === DNS_TYPE_A && record.rdLength === 4) {
      addresses.push(Array.from(record.data).join('.'));
    }

    if (record.type === DNS_TYPE_AAAA && record.rdLength === 16) {
      const parts = [];
      for (let partOffset = 0; partOffset < 16; partOffset += 2) {
        parts.push(record.data.readUInt16BE(partOffset).toString(16));
      }
      addresses.push(parts.join(':'));
    }
  }

  return {
    rcode,
    authorityCount,
    addresses
  };
}

function skipQuestion(buffer, offset) {
  const { nextOffset } = decodeName(buffer, offset);
  return nextOffset + 4;
}

function parseRecord(buffer, offset) {
  const { nextOffset: nameOffset } = decodeName(buffer, offset);
  const type = buffer.readUInt16BE(nameOffset);
  const rdLength = buffer.readUInt16BE(nameOffset + 8);
  const dataOffset = nameOffset + 10;
  const nextOffset = dataOffset + rdLength;

  return {
    type,
    rdLength,
    data: buffer.subarray(dataOffset, nextOffset),
    nextOffset
  };
}

function decodeName(buffer, offset) {
  let cursor = offset;
  let nextOffset = offset;
  let jumped = false;

  while (true) {
    const length = buffer[cursor];

    if (length === 0) {
      if (!jumped) {
        nextOffset = cursor + 1;
      }
      break;
    }

    if ((length & 0xc0) === 0xc0) {
      if (!jumped) {
        nextOffset = cursor + 2;
      }
      const pointer = ((length & 0x3f) << 8) | buffer[cursor + 1];
      cursor = pointer;
      jumped = true;
      continue;
    }

    cursor += length + 1;
    if (!jumped) {
      nextOffset = cursor;
    }
  }

  return { nextOffset };
}
