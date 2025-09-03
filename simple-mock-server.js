const http = require('http');
const url = require('url');

const port = 8080;

const server = http.createServer((req, res) => {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  
  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }
  
  const parsedUrl = url.parse(req.url, true);
  const path = parsedUrl.pathname;
  
  // Set content type to JSON
  res.setHeader('Content-Type', 'application/json');
  
  if (req.method === 'POST' && path === '/v1/chat/completions') {
    let body = '';
    req.on('data', chunk => {
      body += chunk.toString();
    });
    
    req.on('end', () => {
      let requestData;
      try {
        requestData = JSON.parse(body);
      } catch (e) {
        requestData = {};
      }
      
      console.log('📨 Received chat request:', JSON.stringify(requestData, null, 2));
      
      const lastMessage = requestData.messages && requestData.messages.length > 0 
        ? requestData.messages[requestData.messages.length - 1].content 
        : 'nothing';
      
      const response = {
        id: 'chatcmpl-' + Math.random().toString(36).substring(2, 15),
        object: 'chat.completion',
        created: Math.floor(Date.now() / 1000),
        model: requestData.model || 'gpt-3.5-turbo',
        choices: [{
          index: 0,
          message: {
            role: 'assistant',
            content: `🤖 Hello! This is a mock response from the Spring AI Agent API. 

I received your message: "${lastMessage}"

Since this is a mock server, I'm not actually calling OpenAI, but the real implementation would:
1. Process your conversation history
2. Call OpenAI's API with your message
3. Return the AI's response

The Angular UI is working correctly and communicating with the backend API! 🎉`
          },
          finish_reason: 'stop'
        }],
        usage: {
          prompt_tokens: 50,
          completion_tokens: 100,
          total_tokens: 150
        }
      };
      
      res.writeHead(200);
      res.end(JSON.stringify(response));
    });
    
  } else if (req.method === 'GET' && path === '/v1/models') {
    const response = {
      object: 'list',
      data: [{
        id: 'gpt-3.5-turbo',
        object: 'model',
        owned_by: 'spring-ai-agent-mock'
      }]
    };
    
    res.writeHead(200);
    res.end(JSON.stringify(response));
    
  } else if (req.method === 'GET' && path === '/actuator/health') {
    const response = {
      status: 'UP',
      components: {
        diskSpace: { status: 'UP' },
        ping: { status: 'UP' },
        mockServer: { status: 'UP', note: 'This is a mock server for testing' }
      }
    };
    
    res.writeHead(200);
    res.end(JSON.stringify(response));
    
  } else {
    res.writeHead(404);
    res.end(JSON.stringify({ error: 'Not Found', path: path }));
  }
});

server.listen(port, () => {
  console.log(`🚀 Mock Spring AI Agent server running at http://localhost:${port}`);
  console.log(`📱 Test the chat UI at http://localhost:4200`);
  console.log(`🔧 API endpoints:`);
  console.log(`   - POST /v1/chat/completions`);
  console.log(`   - GET /v1/models`);
  console.log(`   - GET /actuator/health`);
  console.log('');
  console.log('💡 This is a mock server for testing purposes.');
  console.log('   The real Spring Boot application requires Maven to build.');
  console.log('');
});