package io.confluent.pas.mcp.proxy.frameworks.python;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.mcp.proxy.frameworks.python.exceptions.ConfigurationException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Getter
@Setter
public class AgentConfiguration {

    @Getter
    @Setter
    public static class ToolConfiguration {
        @JsonProperty("name")
        private String name;
        @JsonProperty("output_schema")
        private JsonSchema output_schema;

        public void setOutput_schema(String output_schema) {
            this.output_schema = new JsonSchema(output_schema);
        }

        public void setOutput_schema(JsonSchema output_schema) {
            this.output_schema = output_schema;
        }
    }

    @JsonProperty("command")
    private String command;

    @JsonProperty("arguments")
    private List<String> arguments;

    @JsonProperty("tool")
    private ToolConfiguration tool;

    /**
     * Parses the agent configuration from a file.
     *
     * @param filePath The path to the configuration file.
     * @return The parsed agent configuration.
     * @throws ConfigurationException If there is an issue parsing the configuration.
     */
    public static AgentConfiguration fromFile(String filePath) throws ConfigurationException {
        if (!StringUtils.hasText(filePath)) {
            throw new ConfigurationException("File path cannot be empty");
        }

        final File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new ConfigurationException("File does not exist: " + filePath);
        }

        final String extension = filePath.substring(filePath.lastIndexOf('.')).toLowerCase();
        if (extension.equals(".yaml") || extension.equals(".yml")) {
            return parseYamlConfiguration(file, filePath);
        } else if (extension.equals(".json")) {
            return parseJsonConfiguration(file, filePath);
        } else {
            throw new ConfigurationException("Unsupported configuration file format: " + filePath);
        }


    }

    private static AgentConfiguration parseJsonConfiguration(File file, String filePath) throws ConfigurationException {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(file, AgentConfiguration.class);
        } catch (IOException e) {
            throw new ConfigurationException("Error parsing configuration file: " + filePath, e);
        }
    }

    private static AgentConfiguration parseYamlConfiguration(File file, String filePath) throws ConfigurationException {
        Yaml yaml = new Yaml();
        try {
            return yaml.loadAs(new FileInputStream(file), AgentConfiguration.class);
        } catch (Exception e) {
            throw new ConfigurationException("Error parsing configuration file: " + filePath, e);
        }
    }
}
