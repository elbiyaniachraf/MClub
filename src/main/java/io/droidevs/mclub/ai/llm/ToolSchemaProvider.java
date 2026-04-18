package io.droidevs.mclub.ai.llm;

import io.droidevs.mclub.ai.tools.Tool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Provides JSON-Schema-like tool definitions for OpenAI function calling.
 *
 * <p>For now this is intentionally simple and maps known existing tools.
 * In a later step, evolve Tool to expose a formal schema contract.
 */
@Component
public class ToolSchemaProvider {

    public List<Map<String, Object>> buildToolSchemas(List<Tool> tools) {
        List<Map<String, Object>> schemas = new ArrayList<>();
        for (Tool t : tools) {
            schemas.add(switch (t.name()) {
                case "register_event" -> tool("register_event",
                        "Register the linked user to a specific event.",
                        objectSchema(Map.of(
                                "eventId", Map.of("type", "string", "description", "UUID of the event")
                        ), List.of("eventId")));
                case "checkin_event" -> tool("checkin_event",
                        "Check-in the linked user using the event QR token.",
                        objectSchema(Map.of(
                                "qrToken", Map.of("type", "string", "description", "Raw QR token value")
                        ), List.of("qrToken")));
                case "rate_event" -> tool("rate_event",
                        "Rate an event for the linked user.",
                        objectSchema(Map.of(
                                "eventId", Map.of("type", "string", "description", "UUID of the event"),
                                "stars", Map.of("type", "integer", "minimum", 1, "maximum", 5),
                                "comment", Map.of("type", "string")
                        ), List.of("eventId", "stars")));
                case "add_comment" -> tool("add_comment",
                        "Add a top-level comment to EVENT or ACTIVITY.",
                        objectSchema(Map.of(
                                "targetType", Map.of("type", "string", "enum", List.of("EVENT", "ACTIVITY")),
                                "targetId", Map.of("type", "string", "description", "UUID of event/activity"),
                                "text", Map.of("type", "string")
                        ), List.of("targetType", "targetId", "text")));
                case "list_events" -> tool("list_events",
                        "List events (read-only).",
                        objectSchema(Map.of(
                                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 25, "description", "Max items")
                        ), List.of()));
                case "list_clubs" -> tool("list_clubs",
                        "List clubs (read-only).",
                        objectSchema(Map.of(
                                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 25, "description", "Max items")
                        ), List.of()));
                case "list_my_clubs" -> tool("list_my_clubs",
                        "List clubs for the linked user (read-only).",
                        objectSchema(Map.of(), List.of()));
                case "search_semantic" -> tool("search_semantic",
                        "Semantic (vector) search across indexed entities. Use this to find IDs before calling action tools.",
                        objectSchema(Map.of(
                                "query", Map.of("type", "string", "description", "Natural language search query"),
                                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 10)
                        ), List.of("query")));
                case "search_events" -> tool("search_events",
                        "Semantic search restricted to events. Use it to find an eventId.",
                        objectSchema(Map.of(
                                "query", Map.of("type", "string"),
                                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 10)
                        ), List.of("query")));
                case "search_clubs" -> tool("search_clubs",
                        "Semantic search restricted to clubs. Use it to find a clubId.",
                        objectSchema(Map.of(
                                "query", Map.of("type", "string"),
                                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 10)
                        ), List.of("query")));
                case "get_event_details" -> tool("get_event_details",
                        "Fetch event details by eventId (read-only).",
                        objectSchema(Map.of(
                                "eventId", Map.of("type", "string", "description", "UUID of the event")
                        ), List.of("eventId")));
                case "search_events_in_context" -> tool("search_events_in_context",
                        "Search events but automatically scope to the current page context (club/event) if available.",
                        objectSchema(Map.of(
                                "query", Map.of("type", "string"),
                                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 10)
                        ), List.of("query")));
                case "search_clubs_in_context" -> tool("search_clubs_in_context",
                        "Search clubs but automatically use the current page club context if available.",
                        objectSchema(Map.of(
                                "query", Map.of("type", "string"),
                                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 10)
                        ), List.of("query")));
                case "pick_candidate" -> tool("pick_candidate",
                        "Pick a single candidate from a candidates list previously returned by a search tool.",
                        objectSchema(Map.of(
                                "index", Map.of("type", "integer", "minimum", 1, "description", "1-based index"),
                                "candidates", Map.of("type", "array", "description", "The candidates array from a previous tool result")
                        ), List.of("index", "candidates")));
                default -> tool(t.name(), "Tool: " + t.name(), objectSchema(Map.of(), List.of()));
            });
        }
        return schemas;
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "function");
        m.put("function", new LinkedHashMap<>(Map.of(
                "name", name,
                "description", description,
                "parameters", parameters
        )));
        return m;
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "object");
        s.put("properties", properties);
        s.put("required", required);
        s.put("additionalProperties", false);
        return s;
    }
}

