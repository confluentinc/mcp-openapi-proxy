package io.confluent.pas.mcp.proxy.frameworks.python;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Handles tool-related operations for the MCP (Model Context Protocol) agent.
 * This record encapsulates a tool's metadata and provides matching functionality.
 *
 * @param tool     The MCP tool definition containing the tool's specifications
 * @param toolName The name identifier for the tool
 */
public record AgentToolHandler(McpSchema.Tool tool, String toolName) {
    /**
     * Checks if the tool matches the given name.
     *
     * @param name The name to match against the tool's name
     * @return true if the tool exists and its name matches the given name, false otherwise
     */
    public boolean matches(String name) {
        return tool != null && tool.name().equals(name);
    }
}