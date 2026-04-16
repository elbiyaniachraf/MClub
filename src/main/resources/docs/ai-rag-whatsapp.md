# MClub — RAG Conversational System (WhatsApp) — Design & Implementation Guide

This document describes the **RAG-based conversational layer** integrated into the existing **MClub** Spring Boot application.

Goals:
- Let users chat via WhatsApp to **query** the system (clubs/events/registrations/comments/ratings)
- Let users **execute actions** (register, check-in, rate, comment, reply, like) using natural language
- Reuse existing business logic (Controller → Service → Repository → Domain)

---

## 1) High-level architecture (text diagram)

```
WhatsApp Provider (Twilio / Meta Cloud API)
        |
        |  Webhook POST /webhooks/whatsapp
        v
+---------------------------+
| WhatsAppWebhookController |
+---------------------------+
        |
        v
+---------------------+
| ConversationService |
| - session memory    |
| - identity context  |
| - calls RagService  |
+---------------------+
        |
        v
+------------------+         +------------------+
|    RagService    |-------> | RetrievalService |
| - prompt build   |         | - structured DB  |
| - LLM decision   |         | - (optional) vec |
| - tool execution |         +------------------+
+------------------+
        |
        v
+-----------------------------+
| ToolRegistry + Tool (SPI)   |
| - Strategy/Command pattern  |
| - calls existing Services    |
+-----------------------------+
        |
        v
Existing MClub Service Layer (RegistrationService, AttendanceService, CommentService, RatingService, ...)

        |
        v
WhatsAppSender (Adapter) -> provider API (send message)
```

---

## 2) Code package structure

All AI/chat components live under:

- `io.droidevs.mclub.ai.conversation`
  - `ConversationService`
  - `ConversationStore` (+ `InMemoryConversationStore`)
  - `ConversationSession`, `ConversationMessage`
  - `WhatsappIdentityService`
  - `ConversationContext`

- `io.droidevs.mclub.ai.rag`
  - `RagService`
  - `RetrievalContext`
  - `RetrievalService` (interface lives in `ai.retrieval`)
  - `PromptBuilder` (Builder pattern)
  - `LlmClient`, `LlmDecision`, `ToolCall`
  - `StubLlmClient` (dev only)

- `io.droidevs.mclub.ai.tools`
  - `Tool` (Command/Strategy interface)
  - `ToolRegistry`
  - `ToolResult`
  - concrete tools, e.g. `RegisterToEventTool`

- `io.droidevs.mclub.ai.webhook.whatsapp`
  - `WhatsAppWebhookController` (Adapter pattern)
  - `WhatsAppSender` + `LoggingWhatsAppSender`
  - `dto/WhatsAppWebhookRequest`

---

## 3) Current implemented flow (what exists today)

1. WhatsApp provider sends inbound message
2. `WhatsAppWebhookController.receive()` validates and forwards to `ConversationService`
3. `ConversationService`:
   - loads session (in-memory store)
   - appends the user message
   - builds `ConversationContext` from WhatsApp phone using `WhatsappIdentityService`
   - calls `RagService.handle(session, ctx)`
   - appends assistant response + sends response using `WhatsAppSender`

4. `RagService`:
   - calls `RetrievalService.retrieve(ctx, userMessage)`
   - builds prompt via `PromptBuilder`
   - calls `LlmClient.decide(prompt)`
   - if a tool call exists: runs tool via `ToolRegistry` and returns tool output
   - else returns normal answer

---

## 4) Tool / Action layer (Command + Strategy)

### 4.1 Tool contract

Each action is a `Tool`:
- `name()` → stable tool identifier used by the LLM
- `execute(ToolCall, ConversationContext)` → performs the action using existing services

### 4.2 Example tool: RegisterToEventTool

Already implemented:
- Reads `eventId` from tool arguments
- Uses `RegistrationService.register(eventId, email)`

---

## 5) Retrieval layer

### 5.1 Structured retrieval (recommended baseline)

`RetrievalService` should query the system through existing services/repositories to fetch:
- upcoming events / event details by free-text
- clubs a user belongs to
- registration status
- attendance windows
- ratings summary
- comments summary

Returned as a **compact** `RetrievalContext` that is injected into the prompt.

### 5.2 Semantic retrieval (optional)

If you later add embeddings:
- Use **pgvector** (PostgreSQL extension)
- Store embeddings for event titles/descriptions, club names, help docs, policies
- Hybrid search: structured filters (clubId, date range) + semantic similarity

---

## 6) Intent detection & routing strategy

We use the LLM to return either:
- direct answer text
- OR a JSON tool call:

```json
{ "toolName": "register_event", "arguments": { "eventId": "..." } }
```

Important production best practices:
- enforce a strict JSON schema (fail closed)
- validate all IDs, time windows, permissions in the tool/service layer

---

## 7) WhatsApp integration

### 7.1 Webhook endpoint

- `POST /webhooks/whatsapp`
- Payload is normalized into `WhatsAppWebhookRequest` (provider-agnostic)

### 7.2 Outbound

`WhatsAppSender` is an adapter.
- In dev: `LoggingWhatsAppSender` (logs to console)
- In prod: implement `TwilioWhatsAppSender` or `MetaCloudWhatsAppSender`

---

## 8) Security & identity mapping

WhatsApp messages are authenticated by provider webhook validation.

Inside MClub we still need to map:

`fromPhoneE164` → `User`

`WhatsappIdentityService.buildContext()` should:
- lookup user by phone (or a link table)
- return `ConversationContext(userId, userEmail, linked=true/false)`

Tools must enforce authorization:
- student-only actions: `@PreAuthorize` at REST controllers doesn’t apply here; you must validate in service/tool logic.
- safest approach: treat WhatsApp user as equivalent to an authenticated principal + check roles from DB.

---

## 9) What to implement next (recommended order)

1) Add core tools:
- `CheckInEventTool`
- `RateEventTool`
- `AddCommentTool`
- `ReplyToCommentTool`
- `LikeCommentTool`

2) Improve retrieval:
- find events by keyword/date
- show upcoming events list
- show "my registrations" / "my upcoming" results

3) Implement a real LLM client:
- OpenAI Chat Completions / Responses API (JSON mode)
- Or Azure OpenAI

4) Persistence for conversations (optional but recommended):
- Redis for short TTL
- PostgreSQL for audit

---

## 10) Example conversation flows

### Register
User: “Register me for the AI workshop tomorrow.”
- Retriever returns top matching events
- LLM selects eventId
- Tool executes registration and returns confirmation

### Comments
User: “Show comments for event X.”
- Retriever fetches comment summary + event id
- LLM answers or requests clarification

### Check-in
User: “I’m at the event, check me in.”
- Tool validates:
  - user is registered
  - current time within attendance window
  - not already checked in

---

## 11) Notes for Production hardening

- Make webhook processing async + idempotent
- Add rate limiting per WhatsApp number
- Add structured audit logs for tool calls
- Add error normalization (never leak stack traces)
- Add unit tests for tools (later)

