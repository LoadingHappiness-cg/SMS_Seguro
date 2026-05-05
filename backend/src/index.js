import { createApp } from './server.js';

const port = Number.parseInt(process.env.PORT ?? '8787', 10);

const app = createApp();

app.listen(port, '0.0.0.0', () => {
  console.info(`sms-seguro link enrichment backend listening on :${port}`);
});
