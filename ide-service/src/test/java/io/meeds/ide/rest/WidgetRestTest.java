/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.ide.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.ide.model.Widget;
import io.meeds.ide.service.WidgetService;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;
import lombok.SneakyThrows;

@SpringBootTest(classes = { WidgetRest.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class WidgetRestTest {

  private static final String REST_PATH     = "/widgets";    // NOSONAR

  private static final String HTML_CONTENT  = "...html...";

  private static final String CSS_CONTENT   = "...css...";

  private static final String JS_CONTENT    = "...js...";

  private static final String SIMPLE_USER   = "simple";

  private static final String TEST_PASSWORD = "testPassword";

  static final ObjectMapper   OBJECT_MAPPER;

  static {
    // Workaround when Jackson is defined in shared library with different
    // version and without artifact jackson-datatype-jsr310
    OBJECT_MAPPER = JsonMapper.builder()
                              .configure(JsonReadFeature.ALLOW_MISSING_VALUES, true)
                              .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                              .build();
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
  }

  @MockBean
  private WidgetService         widgetService;

  @Autowired
  private SecurityFilterChain   filterChain;

  @Autowired
  private WebApplicationContext context;

  @Mock
  private Widget                widget;

  private MockMvc               mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .addFilters(filterChain.getFilters().toArray(new Filter[0]))
                             .build();
  }

  RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("users"));
  }

  RequestPostProcessor testAdministrator() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("administrators"));
  }

  @Test
  void getWidgetAnonymouslyWhenNotFound() throws Exception {
    when(widgetService.getWidget(2l)).thenThrow(ObjectNotFoundException.class);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2"));
    response.andExpect(status().isNotFound());
  }

  @Test
  void getWidgetAnonymously() throws Exception {
    when(widgetService.getWidget(2l)).thenReturn(widget);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2"));
    response.andExpect(status().isOk());
  }

  @Test
  void getWidgetHtmlAnonymouslyWhenNotFound() throws Exception {
    when(widgetService.getWidget(2l)).thenThrow(ObjectNotFoundException.class);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2/html"));
    response.andExpect(status().isNotFound());
  }

  @Test
  void getWidgetHtmlAnonymously() throws Exception {
    when(widgetService.getWidget(2l)).thenReturn(widget);
    when(widget.getHtml()).thenReturn(HTML_CONTENT);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2/html"));
    response.andExpect(status().isOk())
            .andExpect(content().string(HTML_CONTENT))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML.toString()));
  }

  @Test
  void getWidgetCssAnonymouslyWhenNotFound() throws Exception {
    when(widgetService.getWidget(2l)).thenThrow(ObjectNotFoundException.class);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2/css"));
    response.andExpect(status().isNotFound());
  }

  @Test
  void getWidgetCssAnonymously() throws Exception {
    when(widgetService.getWidget(2l)).thenReturn(widget);
    when(widget.getCss()).thenReturn(CSS_CONTENT);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2/css"));
    response.andExpect(status().isOk())
            .andExpect(content().string(CSS_CONTENT))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/css"));
  }

  @Test
  void getWidgetJsAnonymouslyWhenNotFound() throws Exception {
    when(widgetService.getWidget(2l)).thenThrow(ObjectNotFoundException.class);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2/js"));
    response.andExpect(status().isNotFound());
  }

  @Test
  void getWidgetJsAnonymously() throws Exception {
    when(widgetService.getWidget(2l)).thenReturn(widget);
    when(widget.getJs()).thenReturn(JS_CONTENT);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/2/js"));
    response.andExpect(status().isOk())
            .andExpect(content().string(JS_CONTENT))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/javascript"));
  }

  @Test
  void updateWidgetAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").content(asJsonString(widget))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(widgetService);
  }

  @Test
  void updateWidgetWithUser() throws Exception {
    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").with(testSimpleUser())
                                                                  .content(asJsonString(widget))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
    verifyNoInteractions(widgetService);
  }

  @Test
  void updateWidgetWithAdministrator() throws Exception {
    Widget widgetToUpdate = new Widget();
    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").with(testAdministrator())
                                                                  .content(asJsonString(widgetToUpdate))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isOk());
    widgetToUpdate.setId(1l);
    verify(widgetService).updateWidget(widgetToUpdate, SIMPLE_USER);
  }

  @Test
  void updateWidgetWithAdministratorNotFound() throws Exception {
    when(widgetService.updateWidget(any(), any())).thenThrow(ObjectNotFoundException.class);

    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").with(testAdministrator())
                                                                  .content(asJsonString(widget))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isNotFound());
  }

  @Test
  void updateWidgetWithAdministratorForbidden() throws Exception {
    when(widgetService.updateWidget(any(), any())).thenThrow(IllegalAccessException.class);
    ResultActions response = mockMvc.perform(put(REST_PATH + "/1").with(testAdministrator())
                                                                  .content(asJsonString(widget))
                                                                  .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @SneakyThrows
  String asJsonString(final Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

}
