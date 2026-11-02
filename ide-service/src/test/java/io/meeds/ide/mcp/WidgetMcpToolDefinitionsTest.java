/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.ide.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

public class WidgetMcpToolDefinitionsTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  @SneakyThrows
  public void theOneToolIsDeclaredForThePublicMethodOfTheTool() {
    JsonNode root = definitions();
    assertEquals(List.of("tools"), List.copyOf(root.propertyNames()));
    assertEquals(1, root.get("tools").size());
    JsonNode tool = root.get("tools").get(0);
    assertEquals("update_widget_code", tool.get("name").asString());
    Method method = WidgetMcpTool.class.getMethod("updateWidgetCode",
                                                  Long.class,
                                                  String.class,
                                                  String.class,
                                                  String.class,
                                                  String.class,
                                                  String.class);
    assertNotNull(method);
    assertEquals(List.of("update", "widget", "code"), List.of(tool.get("name").asString().split("_")));
  }

  @Test
  @SneakyThrows
  public void theSchemaNamesTheSixArgumentsAndRequiresTheApplicationIdOnly() {
    JsonNode tool = definitions().get("tools").get(0);
    JsonNode properties = tool.get("input_schema").get("properties");
    assertEquals(List.of("page_ref", "site_id", "application_id", "html", "css", "js"), List.copyOf(properties.propertyNames()));
    assertEquals("integer", properties.get("site_id").get("type").asString());
    assertEquals("string", properties.get("page_ref").get("type").asString());
    assertEquals(1, tool.get("input_schema").get("required").size());
    assertEquals("application_id", tool.get("input_schema").get("required").get(0).asString());
  }

  @Test
  @SneakyThrows
  public void theWriteToolCarriesNoApprovalAndSaysWhatItDoesNotGuarantee() {
    JsonNode tool = definitions().get("tools").get(0);
    JsonNode annotations = tool.get("annotations");
    assertFalse(annotations.get("readOnlyHint").asBoolean());
    assertFalse(annotations.get("destructiveHint").asBoolean());
    assertFalse(annotations.get("idempotentHint").asBoolean());
    assertFalse(annotations.get("openWorldHint").asBoolean());
    assertNull(tool.get("require_approval"));
    String description = tool.get("description").asString();
    assertTrue(description.contains("Reserved to platform administrators"));
    assertTrue(description.contains("declared exceptions to all-or-nothing"));
    assertTrue(description.contains("the copy is deleted"));
    assertTrue(description.contains("readable by the layout's editors only"));
    assertTrue(description.contains("draft_created"));
    for (String property : Map.of("html", "HTML", "css", "CSS", "js", "JavaScript").keySet()) {
      assertTrue(tool.get("input_schema").get("properties").get(property).get("description").asString().contains("omit to keep"));
    }
  }

  @SneakyThrows
  private JsonNode definitions() {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream("ai-tool-definitions.json")) {
      assertNotNull(stream);
      return MAPPER.readTree(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

}
