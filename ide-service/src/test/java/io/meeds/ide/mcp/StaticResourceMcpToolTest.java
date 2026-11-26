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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.ide.constant.WidgetType;
import io.meeds.ide.mcp.model.SiteStaticResourcesResult;
import io.meeds.ide.mcp.model.StaticResourceResult;
import io.meeds.ide.model.Widget;
import io.meeds.ide.service.StaticResourceService;
import io.meeds.ide.service.WidgetService;
import io.meeds.layout.service.LayoutAclService;

import lombok.SneakyThrows;

@SpringBootTest(classes = { StaticResourceMcpTool.class })
@ActiveProfiles("mcp-server")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StaticResourceMcpToolTest {

  private static final String             USER    = "john";

  private static final long               SITE_ID = 7L;

  private static final String             SITE    = "intranet";

  @MockitoBean
  private StaticResourceService           staticResourceService;

  @MockitoBean
  private LayoutService                   layoutService;

  @MockitoBean
  private LayoutAclService                aclService;

  @Autowired
  private StaticResourceMcpTool           tool;

  private MockedStatic<ConversationState> conversation;

  @BeforeEach
  @SneakyThrows
  public void setUp() {
    ConversationState state = mock(ConversationState.class);
    when(state.getIdentity()).thenReturn(new Identity(USER));
    conversation = mockStatic(ConversationState.class);
    conversation.when(ConversationState::getCurrent).thenReturn(state);
    when(aclService.isAdministrator(USER)).thenReturn(true);
    PortalConfig site = mock(PortalConfig.class);
    when(site.getName()).thenReturn(SITE);
    when(site.getType()).thenReturn(PortalConfig.PORTAL_TYPE);
    when(layoutService.getPortalConfig(SITE_ID)).thenReturn(site);
    ReflectionTestUtils.setField(tool, "contentMaxBytes", 0L);
  }

  @AfterEach
  public void tearDown() {
    conversation.close();
  }

  @Test
  public void aNonAdministratorIsRefusedBeforeAnyRead() {
    when(aclService.isAdministrator(USER)).thenReturn(false);
    assertEquals(StaticResourceMcpTool.ADMINISTRATORS_ONLY,
                 assertThrows(IllegalAccessException.class, () -> tool.getSiteStaticResources(SITE_ID)).getMessage());
    assertEquals(StaticResourceMcpTool.ADMINISTRATORS_ONLY,
                 assertThrows(IllegalAccessException.class,
                              () -> tool.setSiteStaticResource(SITE_ID, null, "CSS", "main", "a{}", null, null)).getMessage());
    assertEquals(StaticResourceMcpTool.ADMINISTRATORS_ONLY,
                 assertThrows(IllegalAccessException.class,
                              () -> tool.setSiteStaticResource(SITE_ID, 4L, null, null, "x()", null, null)).getMessage());
    verifyNoInteractions(layoutService, staticResourceService);
  }

  @Test
  @SneakyThrows
  public void theServiceRefusalOfANonAdministratorIsAnsweredByTheFixedText() {
    when(staticResourceService.getStaticResources(SITE, USER)).thenThrow(new IllegalAccessException(WidgetService.NOT_ADMINISTRATOR_USER));
    when(staticResourceService.createStaticResource(any(), eq(USER))).thenThrow(new IllegalAccessException(WidgetService.NOT_ADMINISTRATOR_USER));
    when(staticResourceService.getStaticResource(4L, SITE, USER)).thenThrow(new IllegalAccessException(WidgetService.NOT_ADMINISTRATOR_USER));
    assertEquals(StaticResourceMcpTool.ADMINISTRATORS_ONLY,
                 assertThrows(IllegalAccessException.class, () -> tool.getSiteStaticResources(SITE_ID)).getMessage());
    assertEquals(StaticResourceMcpTool.ADMINISTRATORS_ONLY,
                 assertThrows(IllegalAccessException.class,
                              () -> tool.setSiteStaticResource(SITE_ID, null, "JS", "main", "x()", null, null)).getMessage());
    assertEquals(StaticResourceMcpTool.ADMINISTRATORS_ONLY,
                 assertThrows(IllegalAccessException.class,
                              () -> tool.setSiteStaticResource(SITE_ID, 4L, null, null, "x()", null, null)).getMessage());
  }

  @Test
  public void aSiteIdThatNamesNoSiteIsRefused() {
    assertEquals(StaticResourceMcpTool.SITE_ID_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> tool.getSiteStaticResources(null)).getMessage());
    assertEquals(StaticResourceMcpTool.SITE_ID_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> tool.getSiteStaticResources(0L)).getMessage());
    assertEquals(StaticResourceMcpTool.SITE_ID_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class,
                              () -> tool.setSiteStaticResource(-1L, null, "CSS", "main", "a{}", null, null)).getMessage());
    assertEquals(StaticResourceMcpTool.SITE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> tool.getSiteStaticResources(99L)).getMessage());
    assertEquals(StaticResourceMcpTool.SITE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> tool.setSiteStaticResource(99L, null, "CSS", "main", "a{}", null, null)).getMessage());
    when(layoutService.getPortalConfig(98L)).thenThrow(new IllegalStateException("JDBC: connection refused for user root"));
    assertEquals(StaticResourceMcpTool.SITE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> tool.getSiteStaticResources(98L)).getMessage());
    verifyNoInteractions(staticResourceService);
  }

  /**
   * Site Management lists the portal sites whose space-public-site mark is
   * unset or empty, whatever its value otherwise
   */
  @Test
  public void aSiteThatSiteManagementDoesNotListIsRefusedBeforeAnyReadOrWrite() {
    PortalConfig spaceSite = mock(PortalConfig.class);
    when(spaceSite.getName()).thenReturn("/spaces/marketing");
    when(spaceSite.getType()).thenReturn(PortalConfig.GROUP_TYPE);
    when(layoutService.getPortalConfig(20L)).thenReturn(spaceSite);
    PortalConfig userSite = mock(PortalConfig.class);
    when(userSite.getName()).thenReturn(USER);
    when(userSite.getType()).thenReturn(PortalConfig.USER_TYPE);
    when(layoutService.getPortalConfig(21L)).thenReturn(userSite);
    PortalConfig spacePublicSite = mock(PortalConfig.class);
    when(spacePublicSite.getName()).thenReturn("marketing");
    when(spacePublicSite.getType()).thenReturn(PortalConfig.PORTAL_TYPE);
    when(spacePublicSite.getProperty("IS_SPACE_PUBLIC_SITE")).thenReturn("true");
    when(layoutService.getPortalConfig(22L)).thenReturn(spacePublicSite);
    PortalConfig markedSite = mock(PortalConfig.class);
    when(markedSite.getName()).thenReturn("sales");
    when(markedSite.getType()).thenReturn(PortalConfig.PORTAL_TYPE);
    when(markedSite.getProperty("IS_SPACE_PUBLIC_SITE")).thenReturn("false");
    when(layoutService.getPortalConfig(24L)).thenReturn(markedSite);
    for (long siteId : new long[] { 20L, 21L, 22L, 24L }) {
      assertEquals(StaticResourceMcpTool.SITE_ID_NOT_ACCEPTED,
                   assertThrows(IllegalArgumentException.class, () -> tool.getSiteStaticResources(siteId)).getMessage());
      assertEquals(StaticResourceMcpTool.SITE_ID_NOT_ACCEPTED,
                   assertThrows(IllegalArgumentException.class,
                                () -> tool.setSiteStaticResource(siteId, null, "JS", "main", "x()", null, null)).getMessage());
      assertEquals(StaticResourceMcpTool.SITE_ID_NOT_ACCEPTED,
                   assertThrows(IllegalArgumentException.class,
                                () -> tool.setSiteStaticResource(siteId, 4L, null, null, "x()", null, null)).getMessage());
    }
    verifyNoInteractions(staticResourceService);
  }

  @Test
  @SneakyThrows
  public void aPortalSiteWithAnEmptyPublicSiteMarkIsAccepted() {
    PortalConfig site = mock(PortalConfig.class);
    when(site.getName()).thenReturn("extranet");
    when(site.getType()).thenReturn(PortalConfig.PORTAL_TYPE);
    when(site.getProperty("IS_SPACE_PUBLIC_SITE")).thenReturn("");
    when(layoutService.getPortalConfig(23L)).thenReturn(site);
    when(staticResourceService.getStaticResources("extranet", USER)).thenReturn(List.of());
    assertEquals(List.of(), tool.getSiteStaticResources(23L).resources());
  }

  @Test
  @SneakyThrows
  public void theReadAnswersTheCssAndJsResourcesOfTheSiteOnly() {
    Widget css = resource(3L, WidgetType.CSS, "brand", "a{color:red}", "START_OF_HEAD", "true");
    Widget js = resource(4L, WidgetType.JS, "tracker", "track()", "", "false");
    Widget app = resource(5L, WidgetType.APP, "banner", null, "START_OF_BODY", "true");
    app.setHtml("<b/>");
    when(staticResourceService.getStaticResources(SITE, USER)).thenReturn(List.of(css, js, app));

    SiteStaticResourcesResult result = tool.getSiteStaticResources(SITE_ID);

    assertEquals(SITE_ID, result.siteId());
    assertEquals(List.of(new StaticResourceResult(3L, "CSS", "brand", "a{color:red}", "START_OF_HEAD", true),
                         new StaticResourceResult(4L, "JS", "tracker", "track()", "END_OF_BODY", false)),
                 result.resources());
  }

  @Test
  @SneakyThrows
  public void aSiteWithNoResourceAnswersAnEmptyList() {
    when(staticResourceService.getStaticResources(SITE, USER)).thenReturn(List.of());
    assertEquals(List.of(), tool.getSiteStaticResources(SITE_ID).resources());
  }

  @Test
  public void theArgumentsOfACreationAreCheckedBeforeAnyRead() {
    assertEquals(StaticResourceMcpTool.TYPE_NOT_ACCEPTED, refusal(null, null, "main", "a{}", null));
    assertEquals(StaticResourceMcpTool.TYPE_NOT_ACCEPTED, refusal(null, "APP", "main", "a{}", null));
    assertEquals(StaticResourceMcpTool.TYPE_NOT_ACCEPTED, refusal(null, "html", "main", "a{}", null));
    assertEquals(StaticResourceMcpTool.NAME_NOT_ACCEPTED, refusal(null, "CSS", null, "a{}", null));
    assertEquals(StaticResourceMcpTool.NAME_NOT_ACCEPTED, refusal(null, "CSS", " ", "a{}", null));
    assertEquals(StaticResourceMcpTool.NAME_NOT_ACCEPTED, refusal(null, "CSS", "n".repeat(256), "a{}", null));
    assertEquals(StaticResourceMcpTool.CONTENT_NOT_ACCEPTED, refusal(null, "CSS", "main", null, null));
    assertEquals(StaticResourceMcpTool.POSITION_NOT_ACCEPTED, refusal(null, "CSS", "main", "a{}", "MIDDLE"));
    assertEquals(StaticResourceMcpTool.POSITION_NOT_ACCEPTED, refusal(null, "CSS", "main", "a{}", ""));
    assertEquals(StaticResourceMcpTool.RESOURCE_ID_NOT_ACCEPTED, refusal(0L, null, null, "a{}", null));
    assertEquals(StaticResourceMcpTool.NOTHING_TO_UPDATE, refusal(4L, null, null, null, null));
    assertEquals(StaticResourceMcpTool.NOTHING_TO_UPDATE, refusal(4L, "CSS", null, null, null));
    verifyNoInteractions(layoutService, staticResourceService);
  }

  @Test
  public void aContentOverTheSizeIsRefusedAndOneAtTheSizeAccepted() {
    ReflectionTestUtils.setField(tool, "contentMaxBytes", 4L);
    assertEquals(StaticResourceMcpTool.CONTENT_NOT_ACCEPTED, refusal(null, "CSS", "main", "a{}é", null));
    assertEquals(StaticResourceMcpTool.CONTENT_NOT_ACCEPTED, refusal(4L, null, null, "12345", null));
    verifyNoInteractions(staticResourceService);
    ReflectionTestUtils.setField(tool, "contentMaxBytes", 0L);
    assertEquals(StaticResourceMcpTool.CONTENT_NOT_ACCEPTED,
                 refusal(null, "JS", "main", "x".repeat((int) StaticResourceMcpTool.DEFAULT_MAX_BYTES + 1), null));
    verifyNoInteractions(staticResourceService);
  }

  @Test
  @SneakyThrows
  public void aCreationWritesTheResourceAsSiteManagementDoes() {
    when(staticResourceService.createStaticResource(any(), eq(USER))).thenAnswer(i -> {
      Widget widget = i.getArgument(0);
      widget.setId(12L);
      return widget;
    });

    StaticResourceResult result = tool.setSiteStaticResource(SITE_ID, null, "css", "brand", "a{}", null, null);

    ArgumentCaptor<Widget> captor = ArgumentCaptor.forClass(Widget.class);
    verify(staticResourceService).createStaticResource(captor.capture(), eq(USER));
    Widget created = captor.getValue();
    assertEquals(WidgetType.CSS, created.getType());
    assertEquals("a{}", created.getCss());
    assertEquals(null, created.getJs());
    assertEquals(Map.of("siteName", SITE, "resourceName", "brand", "position", "", "enabled", "true"), created.getProperties());
    assertEquals(new StaticResourceResult(12L, "CSS", "brand", "a{}", "END_OF_BODY", true), result);
    verify(staticResourceService, never()).updateStaticResource(anyLong(),
                                                               anyString(),
                                                               nullable(String.class),
                                                               nullable(String.class),
                                                               nullable(String.class),
                                                               nullable(Boolean.class),
                                                               anyString());
  }

  @Test
  @SneakyThrows
  public void aCreationOfAJsResourceKeepsItsPositionAndItsDisabledState() {
    when(staticResourceService.createStaticResource(any(), eq(USER))).thenAnswer(i -> {
      Widget widget = i.getArgument(0);
      widget.setId(13L);
      return widget;
    });

    StaticResourceResult result = tool.setSiteStaticResource(SITE_ID, null, "JS", "tracker", "track()", "END_OF_HEAD", false);

    ArgumentCaptor<Widget> captor = ArgumentCaptor.forClass(Widget.class);
    verify(staticResourceService).createStaticResource(captor.capture(), eq(USER));
    assertEquals(WidgetType.JS, captor.getValue().getType());
    assertEquals("track()", captor.getValue().getJs());
    assertEquals(null, captor.getValue().getCss());
    assertEquals(new StaticResourceResult(13L, "JS", "tracker", "track()", "END_OF_HEAD", false), result);
  }

  @Test
  @SneakyThrows
  public void anUpdateSendsOnlyWhatWasGivenAndAnswersTheSavedResource() {
    when(staticResourceService.getStaticResource(4L, SITE, USER)).thenReturn(resource(4L, WidgetType.JS, "tracker", "old()", "", "true"));
    when(staticResourceService.updateStaticResource(4L, SITE, null, "track()", null, false, USER)).thenReturn(resource(4L,
                                                                                                                         WidgetType.JS,
                                                                                                                         "tracker",
                                                                                                                         "track()",
                                                                                                                         "",
                                                                                                                         "false"));

    StaticResourceResult result = tool.setSiteStaticResource(SITE_ID, 4L, "js", null, "track()", null, false);

    assertEquals(new StaticResourceResult(4L, "JS", "tracker", "track()", "END_OF_BODY", false), result);
    verify(staticResourceService, never()).createStaticResource(any(), anyString());
  }

  @Test
  @SneakyThrows
  public void anUpdateThatNamesAnotherTypeIsRefusedAndWritesNothing() {
    when(staticResourceService.getStaticResource(4L, SITE, USER)).thenReturn(resource(4L, WidgetType.JS, "tracker", "old()", "", "true"));
    assertEquals(StaticResourceMcpTool.TYPE_NOT_ACCEPTED, refusal(4L, "CSS", null, "a{}", null));
    verify(staticResourceService, never()).updateStaticResource(anyLong(),
                                                               anyString(),
                                                               nullable(String.class),
                                                               nullable(String.class),
                                                               nullable(String.class),
                                                               nullable(Boolean.class),
                                                               anyString());
  }

  @Test
  @SneakyThrows
  public void aResourceThatIsNotOnTheSiteIsNotFound() {
    when(staticResourceService.getStaticResource(4L, SITE, USER)).thenThrow(new ObjectNotFoundException(StaticResourceService.RESOURCE_NOT_FOUND));
    assertEquals(StaticResourceMcpTool.RESOURCE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class,
                              () -> tool.setSiteStaticResource(SITE_ID, 4L, null, null, "x()", null, null)).getMessage());
  }

  @Test
  @SneakyThrows
  public void aStorageFailureIsAnsweredByTheFixedText() {
    when(staticResourceService.createStaticResource(any(), eq(USER))).thenThrow(new IllegalStateException("could not execute statement [IDE_WIDGETS]"));
    assertEquals(StaticResourceMcpTool.RESOURCE_NOT_SAVED,
                 assertThrows(IllegalStateException.class,
                              () -> tool.setSiteStaticResource(SITE_ID, null, "CSS", "main", "a{}", null, null)).getMessage());
    when(staticResourceService.getStaticResource(4L, SITE, USER)).thenReturn(resource(4L, WidgetType.CSS, "main", "a{}", "", "true"));
    when(staticResourceService.updateStaticResource(4L, SITE, null, "b{}", null, null, USER)).thenThrow(new IllegalArgumentException("Can't find row with id : 4"));
    assertEquals(StaticResourceMcpTool.RESOURCE_NOT_SAVED,
                 assertThrows(IllegalStateException.class,
                              () -> tool.setSiteStaticResource(SITE_ID, 4L, null, null, "b{}", null, null)).getMessage());
  }

  @Test
  public void everyArgumentRefusalNamesItsArgumentAndWhatToSend() {
    assertTrue(StaticResourceMcpTool.SITE_ID_NOT_ACCEPTED.startsWith("site_id is not accepted: send "));
    assertTrue(StaticResourceMcpTool.SITE_ID_NOT_ACCEPTED.contains("a portal site Site Management lists"));
    assertTrue(StaticResourceMcpTool.RESOURCE_ID_NOT_ACCEPTED.startsWith("resource_id is not accepted: send "));
    assertTrue(StaticResourceMcpTool.TYPE_NOT_ACCEPTED.startsWith("type is not accepted: send CSS or JS"));
    assertTrue(StaticResourceMcpTool.NAME_NOT_ACCEPTED.startsWith("name is not accepted: send "));
    assertTrue(StaticResourceMcpTool.CONTENT_NOT_ACCEPTED.startsWith("content is not accepted: send "));
    assertTrue(StaticResourceMcpTool.POSITION_NOT_ACCEPTED.contains("START_OF_HEAD, END_OF_HEAD, START_OF_BODY or END_OF_BODY"));
    assertTrue(StaticResourceMcpTool.NOTHING_TO_UPDATE.contains("name, content, position and enabled"));
    assertFalse(StaticResourceMcpTool.RESOURCE_NOT_FOUND.contains("send"));
  }

  private String refusal(Long resourceId, String type, String name, String content, String position) {
    return assertThrows(IllegalArgumentException.class,
                        () -> tool.setSiteStaticResource(SITE_ID, resourceId, type, name, content, position, null)).getMessage();
  }

  private Widget resource(long id, WidgetType type, String name, String content, String position, String enabled) {
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("siteName", SITE);
    properties.put("resourceName", name);
    properties.put("position", position);
    properties.put("enabled", enabled);
    Widget widget = new Widget();
    widget.setId(id);
    widget.setType(type);
    if (type == WidgetType.CSS) {
      widget.setCss(content);
    } else if (type == WidgetType.JS) {
      widget.setJs(content);
    }
    widget.setProperties(properties);
    return widget;
  }

}
