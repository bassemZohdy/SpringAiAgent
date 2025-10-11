# Architecture Documentation

This document provides detailed architectural diagrams and design patterns for the Spring AI Agent project.

## Table of Contents
1. [System Architecture](#system-architecture)
2. [Agent Framework Design](#agent-framework-design)
3. [Data Flow Patterns](#data-flow-patterns)
4. [Component Interactions](#component-interactions)
5. [Deployment Patterns](#deployment-patterns)
6. [Performance Considerations](#performance-considerations)

## System Architecture

### High-Level System View

```mermaid
graph TB
    subgraph "Client Applications"
        WebUI[Web UI<br/>Angular Application]
        API_Client[API Clients<br/>OpenAI Compatible]
        CLI[CLI Tools<br/>curl, postman]
    end

    subgraph "Load Balancer Layer"
        LB[Nginx/Cloudflare<br/>SSL Termination<br/>Request Routing]
    end

    subgraph "Application Layer"
        subgraph "Spring Boot Services"
            API_Gateway[API Gateway<br/>Port 8080]

            subgraph "Controllers"
                ChatCtrl[ChatController]
                ThreadCtrl[ThreadController]
                AgentCtrl[AgentController]
                HealthCtrl[HealthController]
            end

            subgraph "Services"
                ChatSvc[ChatService]
                AgentSvc[AgentChatService]
                ThreadSvc[ThreadService]
                MemorySvc[MemoryService]
            end

            subgraph "Agent Framework"
                AgentFactory[Agent Factory]
                TaskExecutor[Task Executor]
                MetricsCollector[Metrics Collector]
            end
        end
    end

    subgraph "Data Layer"
        Memory[In-Memory<br/>Agent Memory]
        Threads[Thread Storage<br/>In-Memory]
        Cache[Response Cache<br/>Optional Redis]
    end

    subgraph "External Services"
        OpenAI[OpenAI API<br/>GPT Models]
        LMStudio[LM Studio<br/>Local Models]
        Future[Future Providers<br/>Azure, Anthropic]
    end

    WebUI --> LB
    API_Client --> LB
    CLI --> LB

    LB --> API_Gateway

    API_Gateway --> ChatCtrl
    API_Gateway --> ThreadCtrl
    API_Gateway --> AgentCtrl
    API_Gateway --> HealthCtrl

    ChatCtrl --> ChatSvc
    ThreadCtrl --> ThreadSvc
    AgentCtrl --> AgentSvc

    ChatSvc --> MemorySvc
    AgentSvc --> AgentFactory
    AgentSvc --> MemorySvc

    AgentFactory --> TaskExecutor
    AgentFactory --> MetricsCollector

    TaskExecutor --> Memory
    ThreadSvc --> Threads
    MemorySvc --> Memory

    ChatSvc --> OpenAI
    ChatSvc --> LMStudio
    AgentSvc --> OpenAI
    AgentSvc --> LMStudio

    MemorySvc --> Cache
```

## Agent Framework Design

### Agent Interface Hierarchy

```mermaid
classDiagram
    class Agent {
        <<interface>>
        +String getAgentId()
        +String getAgentName()
        +String getVersion()
        +AgentConfiguration getConfiguration()
        +AgentState getState()
        +boolean isRunning()
        +void start()
        +void pause()
        +void stop()
        +void reset()
        +AgentMetrics getMetrics()
        +AgentMemory getMemory()
        +void clearMemory()
        +void compactMemory()
    }

    class TaskAgent {
        <<interface>>
        +CompletableFuture~RESULT~ process(TASK task)
        +void onTaskStarted(TASK task)
        +void onTaskCompleted(TASK task, RESULT result)
        +void onTaskFailed(TASK task, Throwable error)
        +TaskAgentMetrics getMetrics()
    }

    class ChatAgent {
        <<interface>>
        +CompletableFuture~RESPONSE~ chat(REQUEST request)
        +void onConversationStarted(REQUEST request)
        +void onConversationCompleted(REQUEST request, RESPONSE response)
        +void onConversationFailed(REQUEST request, Throwable error)
        +ChatAgentMetrics getMetrics()
    }

    class AiAgent {
        <<interface>>
        +CompletableFuture~RESULT~ process(TASK task)
        +PROMPT createPrompt(TASK task)
        +CHAT_RESPONSE parseResponse(String response)
        +RESULT transformResponse(CHAT_RESPONSE response)
    }

    class BaseAgent {
        <<abstract>>
        -String agentId
        -AgentState state
        -ExecutorService executor
        -AgentMetrics metrics
        -AgentMemory memory
        +CompletableFuture~RESULT~ process(TASK task)
        #abstract RESULT doProcess(TASK task)
        #void doStart()
        #void doStop()
        #void doPause()
        #void doReset()
    }

    Agent <|-- TaskAgent
    Agent <|-- ChatAgent
    Agent <|-- AiAgent

    TaskAgent <|.. BaseAgent
    ChatAgent <|.. BaseAgent
    AiAgent <|.. BaseAgent

    Agent --> AgentConfiguration
    Agent --> AgentState
    Agent --> AgentMetrics
    Agent --> AgentMemory
```

### Agent State Machine Implementation

```mermaid
stateDiagram-v2
    [*] --> CREATED: Constructor

    CREATED --> STARTING: start()
    STARTING --> STARTED: doStart() success
    STARTING --> ERROR: doStart() failure

    STARTED --> PAUSING: pause()
    PAUSING --> PAUSED: doPause() success
    PAUSING --> ERROR: doPause() failure

    PAUSED --> STARTING: start()
    PAUSED --> STOPPING: stop()
    PAUSED --> RESETTING: reset()

    STARTED --> STOPPING: stop()
    STOPPING --> STOPPED: doStop() success
    STOPPING --> ERROR: doStop() failure

    STARTED --> RESETTING: reset()
    PAUSED --> RESETTING: reset()
    STOPPED --> RESETTING: reset()

    RESETTING --> CREATED: doReset() success
    RESETTING --> ERROR: doReset() failure

    ERROR --> RESETTING: reset()

    note right of STARTED
        Active state
        - Accepting tasks
        - Processing requests
        - Recording metrics
    end note

    note right of PAUSED
        Temporarily stopped
        - No new tasks
        - Current tasks complete
        - Resume possible
    end note

    note right of ERROR
        Error state
        - No operations
        - Requires reset
        - Error logged
    end note
```

## Data Flow Patterns

### Request Processing Flow

```mermaid
sequenceDiagram
    participant Client
    participant API_Gateway
    participant Controller
    participant Service
    participant Agent
    participant Provider
    participant Memory
    participant Metrics

    Client->>API_Gateway: HTTP Request
    API_Gateway->>API_Gateway: Authentication/Validation
    API_Gateway->>Controller: Route to Controller

    Controller->>Service: Service Call
    Service->>Service: Pre-processing

    alt Agent Framework Path
        Service->>Agent: createAgent() if needed
        Service->>Agent: process() or chat()

        Agent->>Metrics: recordOperationStarted()
        Agent->>Memory: retrieveContext()

        Agent->>Provider: LLM API call
        Provider-->>Agent: LLM Response

        Agent->>Memory: storeExecution()
        Agent->>Metrics: recordOperationSucceeded()

        Agent-->>Service: Result with metadata
    else Direct Provider Path
        Service->>Provider: Direct LLM call
        Provider-->>Service: LLM Response
    end

    Service->>Service: Post-processing
    Service-->>Controller: Processed Response

    alt Streaming Response
        Controller-->>Client: SSE Stream
    else Standard Response
        Controller-->>Client: JSON Response
    end
```

### Agent Memory Management Flow

```mermaid
flowchart TD
    Start([Agent Operation Start]) --> CheckMemory{Check Memory Context}

    CheckMemory -->|Found| RetrieveContext[Retrieve Relevant Context]
    CheckMemory -->|Not Found| ProcessWithoutContext[Process without Context]

    RetrieveContext --> ProcessWithLLM[Process with LLM + Context]
    ProcessWithoutContext --> ProcessWithLLM

    ProcessWithLLM --> OperationResult[Get Operation Result]
    OperationResult --> RecordExecution[Record in Memory]

    RecordExecution --> CheckSize{Memory Size > Threshold?}
    CheckSize -->|No| Complete([Operation Complete])
    CheckSize -->|Yes| TriggerCompaction[Trigger Memory Compaction]

    TriggerCompaction --> Summarize[Generate Summary of Old Entries]
    Summarize --> StoreSummary[Store Summary]
    StoreSummary --> ClearOld[Clear Old Entries]
    ClearOld --> Complete

    Complete --> NextOperation([Ready for Next Operation])
```

## Component Interactions

### Spring Boot Component Architecture

```mermaid
graph TB
    subgraph "Presentation Layer"
        subgraph "REST Controllers"
            ChatController[ChatController<br/>/v1/chat/completions]
            ThreadController[ThreadController<br/>/v1/threads]
            AgentController[AgentController<br/>/api/v1/agent]
            ModelController[ModelController<br/>/v1/models]
            HealthController[HealthController<br/>/actuator/health]
        end

        subgraph "Request/Response DTOs"
            ChatRequest[ChatRequest]
            ChatResponse[ChatResponse]
            ThreadDTO[ThreadDTO]
            MessageDTO[MessageDTO]
            MetricsDTO[MetricsDTO]
        end
    end

    subgraph "Business Logic Layer"
        subgraph "Services"
            ChatService[ChatService<br/>OpenAI Integration]
            ThreadService[ThreadService<br/>Conversation Management]
            AgentChatService[AgentChatService<br/>Agent Framework]
            MemoryService[MemoryService<br/>Memory Management]
            MetricsService[MetricsService<br/>Metrics Collection]
        end

        subgraph "Advisors"
            MemoryAdvisor[MemoryAdvisor<br/>Context Injection]
            RetryAdvisor[RetryAdvisor<br/>Error Recovery]
            MetricsAdvisor[MetricsAdvisor<br/>Performance Tracking]
        end
    end

    subgraph "Integration Layer"
        subgraph "Providers"
            OpenAIProvider[OpenAIProvider<br/>Spring AI ChatClient]
            LMStudioProvider[LMStudioProvider<br/>Local LLM]
        end

        subgraph "Agent Framework"
            AgentFactory[AgentFactory<br/>Agent Creation]
            BaseAgentImpl[BaseAgent<br/>Core Implementation]
        end
    end

    subgraph "Data Layer"
        AgentMemory[AgentMemory<br/>Execution History]
        ThreadStorage[ThreadStorage<br/>Conversations]
        MetricsStore[MetricsStore<br/>Performance Data]
    end

    %% Connections
    ChatController --> ChatRequest
    ChatController --> ChatService
    ChatController --> ChatResponse

    ThreadController --> ThreadDTO
    ThreadController --> ThreadService
    ThreadController --> MessageDTO

    AgentController --> MetricsDTO
    AgentController --> AgentChatService

    ChatService --> MemoryAdvisor
    ChatService --> RetryAdvisor
    ChatService --> OpenAIProvider
    ChatService --> LMStudioProvider

    AgentChatService --> AgentFactory
    AgentChatService --> BaseAgentImpl
    AgentChatService --> MetricsService

    AgentFactory --> BaseAgentImpl
    BaseAgentImpl --> AgentMemory
    BaseAgentImpl --> MetricsStore

    ThreadService --> ThreadStorage
    MemoryService --> AgentMemory
```

### Configuration and Dependency Injection

```mermaid
graph LR
    subgraph "Configuration Sources"
        EnvVars[Environment Variables]
        Properties[application.properties]
        YAML[application.yml]
        EnvFile[.env/.env.local]
    end

    subgraph "Configuration Classes"
        AgentConfig[@Configuration<br/>AgentProperties]
        ChatConfig[@Configuration<br/>ChatProperties]
        ProviderConfig[@Configuration<br/>ProviderProperties]
        MetricsConfig[@Configuration<br/>MetricsProperties]
    end

    subgraph "Bean Definitions"
        ChatClient[ChatClient Bean]
        Executor[ExecutorService Bean]
        AgentBeans[Agent Factory Beans]
        ProviderBeans[Provider Beans]
    end

    subgraph "Autowired Components"
        Services[Service Classes]
        Controllers[Controller Classes]
        Advisors[Method Advisors]
    end

    EnvVars --> AgentConfig
    Properties --> ChatConfig
    YAML --> ProviderConfig
    EnvFile --> MetricsConfig

    AgentConfig --> AgentBeans
    ChatConfig --> ChatClient
    ProviderConfig --> ProviderBeans
    MetricsConfig --> Executor

    ChatClient --> Services
    AgentBeans --> Services
    ProviderBeans --> Services
    Executor --> Services

    Services --> Controllers
    Services --> Advisors
```

## Deployment Patterns

### Docker Container Architecture

```mermaid
graph TB
    subgraph "Docker Host"
        subgraph "Docker Network"
            subgraph "Frontend Container"
                AngularUI[Angular App<br/>Nginx Server<br/>Port 80]
            end

            subgraph "Backend Container"
                SpringBoot[Spring Boot App<br/>Embedded Tomcat<br/>Port 8080]
            end

            subgraph "Optional Services"
                Redis[Redis Cache<br/>Optional<br/>Port 6379]
            end
        end

        subgraph "Volume Mounts"
            ConfigVolume[Configuration Volume<br/>.env files]
            LogVolume[Log Volume<br/>./logs]
            DataVolume[Data Volume<br/>./data]
        end
    end

    subgraph "External Services"
        OpenAI[OpenAI API<br/>api.openai.com]
        LMStudio[LM Studio<br/>localhost:1234]
    end

    AngularUI --> SpringBoot
    SpringBoot --> Redis
    SpringBoot --> OpenAI
    SpringBoot --> LMStudio

    SpringBoot -.-> ConfigVolume
    SpringBoot -.-> LogVolume
    Redis -.-> DataVolume
```

### Kubernetes Deployment Pattern

```mermaid
graph TB
    subgraph "Kubernetes Cluster"
        subgraph "Ingress Layer"
            Ingress[Ingress Controller<br/>TLS Termination]
        end

        subgraph "Namespace: spring-ai-agent"
            subgraph "Frontend Deployment"
                UI_Deployment[Angular Deployment<br/>Replicas: 3]
                UI_Service[ClusterIP Service<br/>Port 80]
            end

            subgraph "Backend Deployment"
                API_Deployment[Spring Boot Deployment<br/>Replicas: 2]
                API_Service[ClusterIP Service<br/>Port 8080]
            end

            subgraph "Stateful Services"
                Redis_StatefulSet[Redis StatefulSet<br/>Replicas: 1]
                Redis_Service[ClusterIP Service<br/>Port 6379]
            end

            subgraph "ConfigMaps & Secrets"
                ConfigMap[ConfigMap<br/>Application Config]
                Secret[Secret<br/>API Keys & Secrets]
            end
        end

        subgraph "Monitoring"
            Prometheus[Prometheus]
            Grafana[Grafana Dashboard]
        end
    end

    Ingress --> UI_Service
    Ingress --> API_Service

    UI_Service --> UI_Deployment
    API_Service --> API_Deployment
    Redis_Service --> Redis_StatefulSet

    API_Deployment --> Redis_Service
    API_Deployment --> ConfigMap
    API_Deployment --> Secret

    API_Deployment --> Prometheus
    Redis_StatefulSet --> Prometheus
```

## Performance Considerations

### Concurrent Request Processing

```mermaid
sequenceDiagram
    participant Client1 as Client 1
    participant Client2 as Client 2
    participant Client3 as Client 3
    participant API as Spring Boot API
    participant Agent1 as Agent Instance 1
    participant Agent2 as Agent Instance 2
    participant ThreadPool as Thread Pool
    participant Provider as LLM Provider

    par Parallel Requests
        Client1->>API: Request 1
    and
        Client2->>API: Request 2
    and
        Client3->>API: Request 3
    end

    API->>ThreadPool: Submit Task 1
    API->>ThreadPool: Submit Task 2
    API->>ThreadPool: Submit Task 3

    ThreadPool->>Agent1: Process Task 1
    ThreadPool->>Agent2: Process Task 2
    ThreadPool->>Agent1: Process Task 3

    par Agent Processing
        Agent1->>Provider: LLM Call 1
    and
        Agent2->>Provider: LLM Call 2
    and
        Agent1->>Provider: LLM Call 3
    end

    Provider-->>Agent1: Response 1
    Provider-->>Agent2: Response 2
    Provider-->>Agent1: Response 3

    Agent1-->>ThreadPool: Result 1
    Agent2-->>ThreadPool: Result 2
    Agent1-->>ThreadPool: Result 3

    ThreadPool-->>API: Complete 1
    ThreadPool-->>API: Complete 2
    ThreadPool-->>API: Complete 3

    API-->>Client1: Response 1
    API-->>Client2: Response 2
    API-->>Client3: Response 3
```

### Memory Management and Garbage Collection

```mermaid
flowchart TD
    NewAgent([New Agent Created]) --> AllocateMemory[Allocate Memory & Metrics]
    AllocateMemory --> ProcessRequests[Process Requests]

    ProcessRequests --> UpdateMetrics[Update Metrics]
    UpdateMetrics --> StoreInMemory[Store in Agent Memory]

    StoreInMemory --> CheckMemoryPressure{Memory Pressure?}
    CheckMemoryPressure -->|Low| ProcessRequests
    CheckMemoryPressure -->|High| TriggerCompaction

    TriggerCompaction --> AnalyzeMemory[Analyze Memory Usage]
    AnalyzeMemory --> IdentifyOldEntries[Identify Old Entries]
    IdentifyOldEntries --> GenerateSummary[Generate Summary]
    GenerateSummary --> StoreSummary[Store Summary]
    StoreSummary --> ClearOldEntries[Clear Old Entries]
    ClearOldEntries --> UpdateMemoryStats[Update Memory Stats]

    UpdateMemoryStats --> CheckGC{GC Needed?}
    CheckGC -->|No| ProcessRequests
    CheckGC -->|Yes| TriggerGC[Trigger Garbage Collection]

    TriggerGC --> CompactMemory[Compact Memory Structures]
    CompactMemory --> ResetMetrics[Reset Metrics if Needed]
    ResetMetrics --> ProcessRequests

    ProcessRequests --> AgentStopped([Agent Stopped])
    AgentStopped --> CleanupResources[Cleanup Resources]
    CleanupResources --> GCAll[Final Garbage Collection]
```

### Caching Strategy

```mermaid
graph TB
    subgraph "Caching Layers"
        subgraph "L1 Cache - In-Memory"
            ResponseCache[Response Cache<br/>TTL: 5min]
            MetricsCache[Metrics Cache<br/>TTL: 30s]
            ConfigCache[Config Cache<br/>TTL: 1h]
        end

        subgraph "L2 Cache - Redis Optional"
            SessionCache[Session Cache<br/>TTL: 24h]
            ThreadCache[Thread Cache<br/>TTL: 7d]
            PersistentMetrics[Persistent Metrics<br/>TTL: 30d]
        end

        subgraph "L3 Cache - Database"
            HistoryStore[History Store<br/>Permanent]
            AnalyticsStore[Analytics Store<br/>Permanent]
        end
    end

    subgraph "Cache Invalidation"
        MemoryEvents[Memory Events]
        ConfigEvents[Config Changes]
        TimeEvents[Time-based Expiration]
        ManualEvents[Manual Invalidation]
    end

    Request([Incoming Request]) --> CheckL1{Check L1 Cache}
    CheckL1 -->|Hit| ReturnL1[Return from L1]
    CheckL1 -->|Miss| CheckL2{Check L2 Cache}

    CheckL2 -->|Hit| ReturnL2[Return from L2]
    CheckL2 -->|Miss| ProcessRequest[Process Request]

    ProcessRequest --> StoreL1[Store in L1]
    StoreL1 --> StoreL2{Store in L2?}
    StoreL2 -->|Yes| StoreL2Cache[Store in L2]
    StoreL2 -->|No| ReturnResult[Return Result]

    StoreL2Cache --> ReturnResult

    MemoryEvents -->|Invalidate| ResponseCache
    ConfigEvents -->|Invalidate| ConfigCache
    TimeEvents -->|Invalidate| MetricsCache
    ManualEvents -->|Invalidate All| ResponseCache
```

This architecture documentation provides a comprehensive view of the system design, component interactions, and operational patterns. The Mermaid diagrams can be rendered in any Markdown viewer that supports Mermaid, such as GitHub, GitLab, or documentation platforms like MkDocs.