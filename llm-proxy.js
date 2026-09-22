const http = require('http');
const https = require('https');

const TARGET = 'maas-llm-aiplatform-hcm.api.vngcloud.vn';
const PORT = 8081;

const server = http.createServer((req, res) => {
  const chunks = [];
  req.on('data', c => chunks.push(c));
  req.on('end', () => {
    const body = Buffer.concat(chunks);
    const options = {
      hostname: TARGET,
      port: 443,
      path: req.url,
      method: req.method,
      headers: {
        ...req.headers,
        host: TARGET,
        'content-length': body.length,
      },
    };
    const proxyReq = https.request(options, proxyRes => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res);
    });
    proxyReq.on('error', e => {
      console.error('Proxy error:', e.message);
      res.writeHead(502);
      res.end('Proxy error: ' + e.message);
    });
    proxyReq.end(body);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`LLM proxy running on http://0.0.0.0:${PORT} -> https://${TARGET}`);
});
