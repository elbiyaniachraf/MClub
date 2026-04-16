# MClub – RAG Conversational Assistant (WhatsApp)


This document describes the **RAG-based conversational system** integrated into the existing **MClub** Spring Boot application.

## 1) Goal

Allow users to chat via WhatsApp (natural language) and:

- Ask questions (read-only): clubs, events, schedules
- Perform actions (write): register for event, check-in attendance, post comments, rate events

The assistant uses **Retrieval-Augmented Generation (RAG)**:

1. Retrieve relevant, factual context from MClub (DB/services)
2. Ask an LLM to decide whether to answer directly or invoke a **tool/action**
3. Execute tools via existing Service layer
4. Return a safe, human-readable response

## 2) High-level architecture

Text diagram:

```
WhatsApp Provider (Twilio / WhatsApp Business)
            |
            v
POST /webhooks/whatsapp  (Adapter)
            |
            v
ConversationService  (session + identity + history)
            |
            v
RagService (Orchestrator)
   |                 |
   |                 +--> ToolRegistry -> Tool -> Existing Services
   |
   +--> RetrievalService -> (existing services / repositories)
            |
            v
WhatsAppSender (Adapter)
```

Design patterns applied:

- Adapter Pattern: WhatsApp inbound/outbound integration
- Builder Pattern: PromptBuilder
- Strategy/Command Pattern: Tool interface implementations
- Dependency Injection: Spring-managed components

## 3) Package structure

Key packages:

- `io.droidevs.mclub.ai.webhook.whatsapp`
  - `WhatsAppWebhookController` – inbound webhook endpoint
  - `WhatsAppSender` + `LoggingWhatsAppSender` – outbound adapter

- `io.droidevs.mclub.ai.conversation`
  - `ConversationService` – entry point for inbound chat messages
  - `ConversationStore` + `InMemoryConversationStore` – session memory
  - `WhatsappIdentityService` – maps phone number -> user identity (linking)
  - DTOs: `ConversationSession`, `ConversationMessage`, `ConversationContext`, `RagResponse`

- `io.droidevs.mclub.ai.rag`
  - `RagService` – orchestrates retrieval + LLM + tools
  - `PromptBuilder` – builds a prompt with rules + retrieved context + history
  - `RetrievalContext` – retrieval output injected into prompts
  - `LlmClient`, `LlmDecision`, `ToolCall` – LLM abstraction
  - `StubLlmClient` – deterministic dev LLM implementation

- `io.droidevs.mclub.ai.retrieval`
  - `RetrievalService` – retrieval abstraction
  - `StructuredRetrievalService` – current structured retrieval (no embeddings)

- `io.droidevs.mclub.ai.tools`
  - `Tool` – command interface
  - `ToolRegistry` – registry of tools
  - `ToolResult` – tool execution result
  - Example tool: `RegisterToEventTool`

## 4) Current implementation status (what works now)

- End-to-end wiring is functional:
  - WhatsApp webhook -> ConversationService -> RagService
  - Retrieval is executed
  - LLM decision layer exists (currently `StubLlmClient`)
  - Tools can be triggered (example tool implemented)

Notes:

- Identity linking is **not implemented yet** (`WhatsappIdentityService` returns `linked=false`).
  - Therefore, write actions return “please link first”.

## 5) How tool invocation works

The assistant never directly calls domain services.

Instead:

1. `LlmClient.decide(prompt)` returns `LlmDecision`:
   - either `answer(text)`
   - or `tool(ToolCall)`
2. `RagService` checks `ctx.linked()`
3. `ToolRegistry.get(toolName)` finds the requested tool
4. Tool executes and calls existing service layer

### Tool input schema

Each tool accepts a `ToolCall`:

- `toolName`: registry key (e.g., `register_event`)
- `arguments`: `Map<String,Object>` (tool-specific)

In production we will enforce JSON Schemas per tool.

## 6) WhatsApp integration

Inbound:

- `POST /webhooks/whatsapp`
- Request DTO: `WhatsAppWebhookRequest { conversationId, fromPhoneE164, text }`

Outbound:

- `WhatsAppSender.sendText(toPhone, text)`
- Current implementation is `LoggingWhatsAppSender` (logs instead of sending)

## 7) Next steps (recommended)

1. **Implement WhatsApp ⇄ User linking**
   - Add DB table: `user_whatsapp_links(user_id, phone_e164, verified_at)`
   - Add a verification flow (OTP or code from logged-in web UI)

2. **Replace StubLlmClient** with a real OpenAI-compatible client
   - Add structured output enforcing tool calls

3. Add more tools:
   - `checkin_event`
   - `rate_event`
   - `comment`

4. Improve retrieval:
   - Structured: my upcoming events, my clubs, event by name/date
   - Optional semantic: pgvector embeddings for docs + FAQs

5. Observability:
   - Persist conversation history in DB/Redis
   - Add audit logs per tool execution


