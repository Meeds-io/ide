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
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.meeds.ide.mcp.model.SiteStaticResourcesResult;
import io.meeds.ide.mcp.model.StaticResourceResult;
import io.modelcontextprotocol.spec.McpSchema;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

public class StaticResourceMcpToolDefinitionsTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  @SneakyThrows
  public void eachToolIsDeclaredForAPublicMethodOfTheTool() {
    assertNotNull(StaticResourceMcpTool.class.getMethod("getSiteStaticResources", Long.class));
    assertNotNull(StaticResourceMcpTool.class.getMethod("setSiteStaticResource",
                                                        Long.class,
                                                        Long.class,
                                                        String.class,
                                                        String.class,
                                                        String.class,
                                                        String.class,
                                                        Boolean.class));
    assertNotNull(tool("get_site_static_resources"));
    assertNotNull(tool("set_site_static_resource"));
    Set<String> publicMethods = Arrays.stream(StaticResourceMcpTool.class.getDeclaredMethods())
                                      .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                                      .map(java.lang.reflect.Method::getName)
                                      .collect(Collectors.toSet());
    assertEquals(Set.of("getSiteStaticResources", "setSiteStaticResource"), publicMethods);
  }

  @Test
  public void theReadSchemaNamesTheSiteIdOnlyAndTheToolIsReadOnly() {
    JsonNode tool = tool("get_site_static_resources");
    JsonNode schema = tool.get("input_schema");
    assertEquals(List.of("site_id"), List.copyOf(schema.get("properties").propertyNames()));
    assertEquals("integer", schema.get("properties").get("site_id").get("type").asString());
    assertEquals(List.of("site_id"), required(schema));
    JsonNode annotations = tool.get("annotations");
    assertTrue(annotations.get("readOnlyHint").asBoolean());
    assertFalse(annotations.get("destructiveHint").asBoolean());
    assertTrue(annotations.get("idempotentHint").asBoolean());
    assertFalse(annotations.get("openWorldHint").asBoolean());
    assertNull(tool.get("require_approval"));
    String description = tool.get("description").asString();
    assertTrue(description.contains("Reserved to platform administrators"));
    assertTrue(description.contains("it is not a refusal"));
    assertTrue(description.contains("a group site, a space's site or a space's public site is refused"));
  }

  @Test
  public void theWriteSchemaNamesTheSevenArgumentsInTheMethodsOrderAndRequiresTheSiteIdOnly() {
    JsonNode schema = tool("set_site_static_resource").get("input_schema");
    JsonNode properties = schema.get("properties");
    assertEquals(List.of("site_id", "resource_id", "type", "name", "content", "position", "enabled"),
                 List.copyOf(properties.propertyNames()));
    assertEquals("integer", properties.get("site_id").get("type").asString());
    assertEquals("integer", properties.get("resource_id").get("type").asString());
    assertEquals("boolean", properties.get("enabled").get("type").asString());
    for (String property : List.of("type", "name", "content", "position")) {
      assertEquals("string", properties.get(property).get("type").asString());
    }
    assertEquals(List.of("site_id"), required(schema));
  }

  /**
   * The tool refuses a type, a position, a name or a content with a text
   * that says what to send; a bound in the schema would refuse the call
   * before the tool runs, with the validator's message
   */
  @Test
  public void noArgumentOfTheWriteCarriesABoundInItsSchema() {
    JsonNode properties = tool("set_site_static_resource").get("input_schema").get("properties");
    for (String property : properties.propertyNames()) {
      for (String bound : List.of("enum", "minimum", "maximum", "pattern", "maxLength", "minLength")) {
        assertNull(properties.get(property).get(bound), property + " carries " + bound);
      }
    }
  }

  @Test
  public void theWriteIsAnApprovedWriteThatSaysItIsLiveAtOnce() {
    JsonNode tool = tool("set_site_static_resource");
    JsonNode annotations = tool.get("annotations");
    assertFalse(annotations.get("readOnlyHint").asBoolean());
    assertTrue(annotations.get("destructiveHint").asBoolean());
    assertFalse(annotations.get("idempotentHint").asBoolean());
    assertFalse(annotations.get("openWorldHint").asBoolean());
    assertTrue(tool.get("require_approval").asBoolean());
    String description = tool.get("description").asString();
    assertTrue(description.contains("Reserved to platform administrators"));
    assertTrue(description.contains("live on the site at once"));
    assertTrue(description.contains("a group site, a space's site or a space's public site is refused"));
    assertTrue(description.contains("not part of a draft"));
    assertTrue(description.contains("one omitted is kept"));
    assertTrue(description.contains("correct the call and try again"));
  }

  @Test
  public void everyAnnotationKeyOfTheFileIsOneTheProtocolReads() {
    Set<String> known = Arrays.stream(McpSchema.ToolAnnotations.class.getRecordComponents())
                              .map(StaticResourceMcpToolDefinitionsTest::jsonName)
                              .collect(Collectors.toSet());
    assertTrue(known.contains("readOnlyHint"));
    for (JsonNode tool : definitions().get("tools")) {
      for (String key : tool.get("annotations").propertyNames()) {
        assertTrue(known.contains(key), tool.get("name").asString() + " carries the unknown annotation " + key);
      }
    }
  }

  @Test
  public void theResultsNameTheirKeysAsTheDescriptionsDo() {
    assertEquals(List.of("resource_id", "type", "name", "content", "position", "enabled"),
                 Arrays.stream(StaticResourceResult.class.getRecordComponents())
                       .map(StaticResourceMcpToolDefinitionsTest::jsonName)
                       .toList());
    assertEquals(List.of("site_id", "resources"),
                 Arrays.stream(SiteStaticResourcesResult.class.getRecordComponents())
                       .map(StaticResourceMcpToolDefinitionsTest::jsonName)
                       .toList());
  }

  private static String jsonName(RecordComponent component) {
    JsonProperty property = component.getAccessor().getAnnotation(JsonProperty.class);
    if (property == null) {
      try {
        property = component.getDeclaringRecord().getDeclaredField(component.getName()).getAnnotation(JsonProperty.class);
      } catch (NoSuchFieldException e) {
        property = null;
      }
    }
    return property == null ? component.getName() : property.value();
  }

  private List<String> required(JsonNode schema) {
    return schema.get("required").valueStream().map(JsonNode::asString).toList();
  }

  private JsonNode tool(String name) {
    for (JsonNode tool : definitions().get("tools")) {
      if (name.equals(tool.get("name").asString())) {
        return tool;
      }
    }
    return null;
  }

  @SneakyThrows
  private JsonNode definitions() {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream("ai-tool-definitions.json")) {
      assertNotNull(stream);
      return MAPPER.readTree(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

}
