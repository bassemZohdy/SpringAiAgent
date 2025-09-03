const express = require('express');
const cors = require('cors');

const app = express();
const port = 8080;

// Middleware
app.use(cors());
app.use(express.json());

// Mock chat completions endpoint
app.post('/v1/chat/completions', (req, res) => {
  console.log('Received chat request:', JSON.stringify(req.body, null, 2));
  
  const response = {
    id: 'chatcmpl-' + Math.random().toString(36).substring(2, 15),
    object: 'chat.completion',
    created: Math.floor(Date.now() / 1000),
    model: req.body.model || 'gpt-3.5-turbo',
    choices: [{
      index: 0,
      message: {
        role: 'assistant',
        content: '🤖 Hello! This is a mock response from the Spring AI Agent. The actual OpenAI integration would go here. You said: "' + 
                (req.body.messages && req.body.messages.length > 0 ? 
                 req.body.messages[req.body.messages.length - 1].content : 'nothing') + '"'
      },
      finish_reason: 'stop'
    }],
    usage: {
      prompt_tokens: 50,
      completion_tokens: 100,
      total_tokens: 150
    }
  };
  
  res.json(response);
});

// Mock models endpoint
app.get('/v1/models', (req, res) => {
  res.json({
    object: 'list',
    data: [{
      id: 'gpt-3.5-turbo',
      object: 'model',
      owned_by: 'spring-ai-agent-mock'
    }]
  });
});

// Mock health endpoint
app.get('/actuator/health', (req, res) => {
  res.json({
    status: 'UP',
    components: {
      diskSpace: { status: 'UP' },
      ping: { status: 'UP' }
    }
  });
});

app.listen(port, () => {
  console.log(`🚀 Mock Spring AI Agent server running at http://localhost:${port}`);
  console.log(`📱 Test the chat UI at http://localhost:4200`);
  console.log(`🔧 API endpoints:`);
  console.log(`   - POST /v1/chat/completions`);
  console.log(`   - GET /v1/models`);
  console.log(`   - GET /actuator/health`);
});