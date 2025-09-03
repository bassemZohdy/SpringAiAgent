# LM Studio Local Development Setup

This guide explains how to run Spring AI Agent with LM Studio for local testing with models like `gpt-oss-mini`.

## Prerequisites

1. **Install LM Studio**: Download from [https://lmstudio.ai/](https://lmstudio.ai/)
2. **Java 21**: Make sure Java 21 is installed
3. **Maven**: For building the Spring Boot application

## Setup Steps

### 1. Configure LM Studio

1. **Start LM Studio** and go to the **Models** tab
2. **Download a compatible model**:
   - Search for models like "gpt-oss-mini", "llama-2-7b-chat", or any OpenAI-compatible model
   - Download the model you want to test with
3. **Load the model** in the **Chat** tab
4. **Start Local Server**:
   - Go to **Local Server** tab
   - Click **Start Server**
   - Ensure it's running on `http://localhost:1234`
   - Enable **OpenAI Compatible** mode if available

### 2. Configure Spring AI Agent

1. **Copy the local environment file**:
   ```bash
   cp .env.local.example .env.local
   ```

2. **Edit .env.local** to match your LM Studio setup:
   ```bash
   # LM Studio Configuration  
   OPENAI_API_KEY=lm-studio
   OPENAI_BASE_URL=http://localhost:1234/v1
   
   # Model Configuration (use the exact model name from LM Studio)
   AI_MODEL=gpt-oss-mini
   
   # Application Configuration
   SPRING_PROFILES_ACTIVE=dev
   SERVER_PORT=8080
   UI_PORT=4200
   ```

### 3. Run the Application

1. **Build the agent library** (required dependency):
   ```bash
   cd agent
   mvn clean install
   ```

2. **Start the Spring Boot application**:
   ```bash
   cd spring-ai-agent
   mvn spring-boot:run
   ```

3. **Start the Angular UI** (in another terminal):
   ```bash
   cd ui
   npm install
   npm start
   ```

### 4. Test the Setup

1. **Access the UI**: http://localhost:4200
2. **Send a test message** to verify the local model is responding
3. **Check logs** for any connection issues

## Troubleshooting

### Common Issues

1. **Connection Refused**:
   - Make sure LM Studio Local Server is running on port 1234
   - Check that the base URL in .env.local matches LM Studio's server address

2. **Model Not Found**:
   - Verify the `AI_MODEL` name in .env.local matches the model name in LM Studio
   - Some models may have different names than expected

3. **Authentication Errors**:
   - LM Studio usually doesn't require real API keys
   - Try using "lm-studio" or "not-needed" as the API key

4. **Slow Responses**:
   - Local models depend on your hardware (CPU/GPU)
   - Consider using smaller models for faster responses

### Verification Commands

Test the LM Studio API directly:
```bash
curl -X POST http://localhost:1234/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-oss-mini",
    "messages": [{"role": "user", "content": "Hello!"}],
    "temperature": 0.7
  }'
```

## Model Recommendations

- **gpt-oss-mini**: Lightweight, good for testing
- **llama-2-7b-chat**: More capable, requires more resources
- **mistral-7b**: Good balance of speed and quality

## Benefits of Local Testing

- ✅ **No API costs** - Test without OpenAI charges
- ✅ **Privacy** - Data stays on your machine  
- ✅ **Offline development** - Work without internet
- ✅ **Model experimentation** - Try different local models
- ✅ **Performance testing** - Test with various model sizes

## Switching Back to OpenAI

To switch back to OpenAI API:

1. **Rename .env.local** to .env.local.backup (to preserve your local config)
2. **Use .env** file with your OpenAI configuration:
   ```bash
   OPENAI_API_KEY=your-actual-openai-key
   OPENAI_BASE_URL=https://api.openai.com
   AI_MODEL=gpt-3.5-turbo
   ```

The application will automatically use the online OpenAI API instead of your local LM Studio instance.