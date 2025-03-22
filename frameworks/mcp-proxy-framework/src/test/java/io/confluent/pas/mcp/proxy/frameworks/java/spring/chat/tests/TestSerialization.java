package io.confluent.pas.mcp.proxy.frameworks.java.spring.chat.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.pas.mcp.common.utils.JsonUtils;
import io.confluent.pas.mcp.proxy.frameworks.java.spring.chat.KafkaChatMemory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public class TestSerialization {

    final static String MESSAGES = """
            [
              {
                "metadata": {
                  "messageType": "user"
                },
                "messageType": "user",
                "media": [],
                "text": "# Kids Nutrition Expert Meal Planner\\nYou are a pediatric nutritionist specializing in designing nutritionally balanced meals for children aged 4-12 years.\\n## Objective Generate 2 unique, nutrient-dense meals supporting child growth and development.\\n## Meal Planning Guidelines 1. Prioritize balanced nutrition for children's health 2. Ensure meals are: - Age-appropriate - Nutrient-rich - Visually appealing - Enjoyable for children 3. Prepare in 45 minutes or less\\n## Output Requirements Provide meals in this exact JSON format:\\n```json { \\"meals\\": [ \\"Meal 1 Description\\", \\"Meal 2 Description\\", \\"Meal 3 Description\\", \\"...\\" (repeat for number of meals specified) ] } ```\\n## Speed Optimization Instructions: - Generate meals using minimal computational steps - Use lightweight, pre-computed nutritional mappings - Implement quick substitution algorithms - Minimize ingredient cross-referencing - Use rapid template matching - Avoid complex nutritional calculations\\n## Critical Instructions - Generate EXACTLY 2 unique meals - Support active growth and learning needs - Consider potential allergies - Balance nutrition and kid-friendly flavors - If unable to generate meals, return an empty JSON object - MUST return a JSON object with \\"meals\\" array of strings - DO NOT ask any questions or seek clarification - DO NOT include ANY introductory text, explanation, or commentary before the JSON response.\\n## Meal Diversity Criteria - Vary protein sources - Explore different cuisines - Use distinct cooking methods - Prevent ingredient repetition - DO NOT include ANY introductory text before the JSON response"
              },
              {
                "metadata": {
                  "messageType": "ASSISTANT"
                },
                "messageType": "ASSISTANT",
                "media": [],
                "text": "{\\n    \\"meals\\": [\\n        \\"Rainbow Veggie Chicken Quesadilla: Whole wheat tortilla filled with diced grilled chicken, bell peppers (red, yellow, green), low-fat cheese, served with mild salsa. Provides lean protein, complex carbohydrates, and colorful vegetables for balanced nutrition.\\",\\n        \\"Ocean Explorer Fish Plate: Baked salmon fingers with quinoa pilaf, steamed broccoli trees, and a side of yogurt-dill dipping sauce. Packed with omega-3 fatty acids, protein, and essential minerals to support brain development and growth.\\"\\n    ]\\n}",
                "toolCalls": []
              }
            ]
            """;

    @Test
    public void testSerialization() throws JsonProcessingException {
        final KafkaChatMemory.Memory messages = JsonUtils.toObject(MESSAGES, KafkaChatMemory.Memory.class);

        final List<Message> msg = messages.stream().map(KafkaChatMemory.MemoryMessage::toMessage).toList();

        Assert.assertEquals(messages.size(), msg.size());
    }

}
