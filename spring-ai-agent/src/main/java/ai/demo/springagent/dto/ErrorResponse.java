package ai.demo.springagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponse {
    
    @JsonProperty("error")
    private ApiError error;
    
    public ErrorResponse() {}
    
    public ErrorResponse(ApiError error) {
        this.error = error;
    }
    
    public ErrorResponse(String message, String type, String code) {
        this.error = new ApiError(message, type, code);
    }
    
    public ApiError getError() {
        return error;
    }
    
    public void setError(ApiError error) {
        this.error = error;
    }
    
    public static class ApiError {
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("code")
        private String code;
        
        public ApiError() {}
        
        public ApiError(String message, String type, String code) {
            this.message = message;
            this.type = type;
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
    }
    
    // Common error factory methods
    public static ErrorResponse invalidApiKey() {
        return new ErrorResponse(
            "Invalid API key provided. Please check your OPENAI_API_KEY environment variable.",
            "invalid_request_error",
            "invalid_api_key"
        );
    }
    
    public static ErrorResponse modelNotFound(String model) {
        return new ErrorResponse(
            "The model '" + model + "' does not exist or you do not have access to it.",
            "invalid_request_error", 
            "model_not_found"
        );
    }
    
    public static ErrorResponse rateLimitExceeded() {
        return new ErrorResponse(
            "Rate limit reached. Please try again later.",
            "rate_limit_error",
            "rate_limit_exceeded"
        );
    }
    
    public static ErrorResponse internalError(String message) {
        return new ErrorResponse(
            "An internal error occurred: " + message,
            "server_error",
            "internal_error"
        );
    }
    
    public static ErrorResponse invalidRequest(String message) {
        return new ErrorResponse(
            message,
            "invalid_request_error",
            "invalid_request"
        );
    }
}