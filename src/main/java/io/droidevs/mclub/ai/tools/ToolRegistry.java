package io.droidevs.mclub.ai.tools;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Simple registry for tools. */
@Component
@RequiredArgsConstructor
public class ToolRegistry {

    private final List<Tool> tools;
    private final Map<String, Tool> byName = new HashMap<>();

    @PostConstruct
    void init() {
        for (Tool t : tools) {
            byName.put(t.name(), t);
        }
    }

    public Tool get(String name) {
        Tool t = byName.get(name);
        if (t == null) {
            return new Tool() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public ToolResult execute(io.droidevs.mclub.ai.rag.ToolCall call, io.droidevs.mclub.ai.conversation.ConversationContext ctx) {
                    return ToolResult.of("Unknown tool: '" + name + "'. Please try a different request.");
                }
            };
        }
        return t;
    }
}
