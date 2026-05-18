/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package io.meeds.ide.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.meeds.ide.service.StaticResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

import io.meeds.ide.model.Widget;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;
import lombok.SneakyThrows;

@SpringBootTest(classes = { StaticResourceRest.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class StaticResourceRestTest {

  private static final String REST_PATH     = "/static/resources"; // NOSONAR

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

  @MockitoBean
  private StaticResourceService staticResourceService;

  @Autowired
  private SecurityFilterChain   filterChain;

  @Autowired
  private WebApplicationContext context;

  @Mock
  private Widget                widget;

  private MockMvc               mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filterChain.getFilters().toArray(new Filter[0])).build();
  }

  RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD).authorities(new SimpleGrantedAuthority("users"));
  }

  RequestPostProcessor testAdministrator() {
    return user(SIMPLE_USER).password(TEST_PASSWORD).authorities(new SimpleGrantedAuthority("administrators"));
  }

  @Test
  void getStaticResourcesAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH).param("siteName", "siteName"));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getStaticResourcesWithUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH).param("siteName", "siteName").with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getStaticResourcesWithAdministrator() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH).param("siteName", "siteName").with(testAdministrator()));
    response.andExpect(status().isOk());

    doThrow(new IllegalAccessException()).when(staticResourceService).getStaticResources("siteName", SIMPLE_USER);

    response = mockMvc.perform(get(REST_PATH).param("siteName", "siteName").with(testAdministrator()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void createStaticResourceAnonymously() throws Exception {
    ResultActions response =
                           mockMvc.perform(post(REST_PATH).content(asJsonString(widget)).contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void createStaticResourceWithUser() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH).with(testSimpleUser())
                                                            .content(asJsonString(widget))
                                                            .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void createStaticResourceWithAdministrator() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH).with(testAdministrator())
                                                            .content(asJsonString(widget))
                                                            .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isOk());

    doThrow(new IllegalAccessException()).when(staticResourceService).createStaticResource(any(Widget.class), anyString());

    response = mockMvc.perform(post(REST_PATH).with(testAdministrator())
                                              .content(asJsonString(widget))
                                              .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @SneakyThrows
  String asJsonString(final Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

}
