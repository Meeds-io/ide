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
package io.meeds.ide.service;

import static io.meeds.ide.service.StaticResourceService.ENABLED;
import static io.meeds.ide.service.StaticResourceService.POSITION;
import static io.meeds.ide.service.WidgetService.IDE_WIDGET_CREATED_EVENT;
import static io.meeds.ide.service.WidgetService.IDE_WIDGET_UPDATED_EVENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

import io.meeds.portal.navigation.service.NavigationConfigurationService;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.ide.constant.WidgetType;
import io.meeds.ide.model.Widget;
import io.meeds.ide.storage.WidgetStorage;
import io.meeds.layout.service.LayoutAclService;

import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest(classes = { StaticResourceService.class, })
@ExtendWith(MockitoExtension.class)
class StaticResourceServiceTest {

  private static final MockedStatic<CommonsUtils> COMMONS_UTILS_UTIL = mockStatic(CommonsUtils.class);

  private static final String            USERNAME  = "testUser";

  private static final String            SITE_NAME = "siteName";

  @MockitoBean
  private LayoutAclService               layoutAclService;

  @MockitoBean
  private IdentityManager                identityManager;

  @MockitoBean
  private ListenerService                listenerService;

  @MockitoBean
  private WidgetStorage                  widgetStorage;

  @MockitoBean
  private UserPortalConfigService        userPortalConfigService;

  @Autowired
  private StaticResourceService          staticResourceService;

  @Mock
  private Widget                         widget;

  @Mock
  private Identity                       identity;

  @Test
  void getStaticResources() throws IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> staticResourceService.getStaticResources(SITE_NAME, USERNAME));

    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    Map<String, String> filters = new LinkedHashMap<>();
    filters.put("siteName", SITE_NAME);
    staticResourceService.getStaticResources(SITE_NAME, USERNAME);
    verify(widgetStorage).getWidgetsByProperties(filters);
  }

  @Test
  void createStaticResource() throws IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> staticResourceService.createStaticResource(widget, USERNAME));
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);

    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("5");
    when(widgetStorage.createWidget(widget)).thenReturn(widget);

    Widget savedWidget = staticResourceService.createStaticResource(widget, USERNAME);
    assertNotNull(savedWidget);
    verify(widget).setCreatorId(5L);
    verify(widget).setModifierId(5L);
    verify(widget).setCreatedDate(notNull());
    verify(widget).setModifiedDate(notNull());
    verify(widgetStorage).createWidget(widget);
    verify(listenerService).broadcast(IDE_WIDGET_CREATED_EVENT, null, widget);
  }

  @Test
  void getStaticResourceApplications() {
    Map<String, String> filters = new LinkedHashMap<>();
    filters.put(SITE_NAME, "siteName");
    filters.put(POSITION, "END_OF_BODY");
    filters.put(ENABLED, "true");

    NavigationConfigurationService navigationConfigurationService = mock(NavigationConfigurationService.class);
    when(navigationConfigurationService.isMetaSiteNavigation("siteName")).thenReturn(true);

    COMMONS_UTILS_UTIL.when(() -> CommonsUtils.getService(NavigationConfigurationService.class)).thenReturn(navigationConfigurationService);
    when(widgetStorage.getWidgetsByProperties(filters)).thenReturn(List.of(widget));

    List<Application> apps = staticResourceService.getStaticResourceApplications("siteName", "END_OF_BODY");
    assertEquals(1, apps.size());

    assertEquals("ide/WidgetPortlet", ((TransientApplicationState) apps.getFirst().getState()).getContentId());
  }

  @Test
  void getStaticResourceIsForAnAdministratorOnly() {
    when(widgetStorage.getWidget(8L)).thenReturn(resource(8L, WidgetType.CSS, "a{}", "siteName"));
    assertEquals(WidgetService.NOT_ADMINISTRATOR_USER,
                 assertThrows(IllegalAccessException.class,
                              () -> staticResourceService.getStaticResource(8L, "siteName", USERNAME)).getMessage());
    verify(widgetStorage, never()).getWidget(8L);
  }

  @Test
  void getStaticResourceAnswersACssOrJsResourceOfTheNamedSiteOnly() throws Exception {
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    Widget css = resource(8L, WidgetType.CSS, "a{}", "siteName");
    Widget js = resource(9L, WidgetType.JS, "x()", "siteName");
    Widget app = resource(10L, WidgetType.APP, null, "siteName");
    Widget widgetOfAPage = resource(11L, WidgetType.CSS, "a{}", null);
    widgetOfAPage.setProperties(null);
    when(widgetStorage.getWidget(8L)).thenReturn(css);
    when(widgetStorage.getWidget(9L)).thenReturn(js);
    when(widgetStorage.getWidget(10L)).thenReturn(app);
    when(widgetStorage.getWidget(11L)).thenReturn(widgetOfAPage);

    assertSame(css, staticResourceService.getStaticResource(8L, "siteName", USERNAME));
    assertSame(js, staticResourceService.getStaticResource(9L, "siteName", USERNAME));
    for (long id : new long[] { 10L, 11L, 12L }) {
      assertEquals(StaticResourceService.RESOURCE_NOT_FOUND,
                   assertThrows(ObjectNotFoundException.class,
                                () -> staticResourceService.getStaticResource(id, "siteName", USERNAME)).getMessage());
    }
    assertThrows(ObjectNotFoundException.class, () -> staticResourceService.getStaticResource(8L, "otherSite", USERNAME));
    assertThrows(ObjectNotFoundException.class, () -> staticResourceService.getStaticResource(8L, null, USERNAME));
    widgetOfAPage.setProperties(new LinkedHashMap<>());
    assertThrows(ObjectNotFoundException.class, () -> staticResourceService.getStaticResource(11L, null, USERNAME));
    assertThrows(ObjectNotFoundException.class, () -> staticResourceService.getStaticResource(11L, "", USERNAME));
  }

  @Test
  void updateStaticResourceIsForAnAdministratorOnly() {
    when(widgetStorage.getWidget(8L)).thenReturn(resource(8L, WidgetType.CSS, "a{}", "siteName"));
    assertThrows(IllegalAccessException.class,
                 () -> staticResourceService.updateStaticResource(8L, "siteName", "n", "b{}", "END_OF_HEAD", false, USERNAME));
    verify(widgetStorage, never()).updateWidget(any());
    verifyNoInteractions(listenerService);
  }

  @Test
  void updateStaticResourceOfAnotherSiteWritesNothing() {
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    when(widgetStorage.getWidget(8L)).thenReturn(resource(8L, WidgetType.CSS, "a{}", "siteName"));
    assertThrows(ObjectNotFoundException.class,
                 () -> staticResourceService.updateStaticResource(8L, "otherSite", "n", "b{}", "END_OF_HEAD", false, USERNAME));
    verify(widgetStorage, never()).updateWidget(any());
    verifyNoInteractions(listenerService);
  }

  @Test
  void updateStaticResourceReplacesWhatIsGivenAndKeepsTheRest() throws Exception {
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("5");
    Widget stored = resource(8L, WidgetType.CSS, "a{}", "siteName");
    stored.setCreatorId(2L);
    stored.setModifierId(2L);
    stored.setCreatedDate(LocalDateTime.of(2026, 1, 1, 0, 0));
    stored.setModifiedDate(LocalDateTime.of(2026, 1, 1, 0, 0));
    when(widgetStorage.getWidget(8L)).thenReturn(stored);
    when(widgetStorage.updateWidget(any())).thenAnswer(i -> i.getArgument(0));

    Widget updated = staticResourceService.updateStaticResource(8L, "siteName", null, "b{}", null, null, USERNAME);

    assertNotSame(stored, updated);
    assertEquals(8L, updated.getId());
    assertEquals(WidgetType.CSS, updated.getType());
    assertEquals("b{}", updated.getCss());
    assertNull(updated.getJs());
    assertEquals(Map.of("siteName", "siteName", "resourceName", "brand", "position", "START_OF_HEAD", "enabled", "true"),
                 updated.getProperties());
    assertEquals(2L, updated.getCreatorId());
    assertEquals(5L, updated.getModifierId());
    assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), updated.getCreatedDate());
    assertTrue(updated.getModifiedDate().isAfter(LocalDateTime.of(2026, 1, 1, 0, 0)));
    assertEquals("a{}", stored.getCss());
    verify(listenerService).broadcast(IDE_WIDGET_UPDATED_EVENT, null, updated);

    Widget renamed = staticResourceService.updateStaticResource(8L, "siteName", "main", null, "", false, USERNAME);
    assertEquals("a{}", renamed.getCss());
    assertEquals(Map.of("siteName", "siteName", "resourceName", "main", "position", "", "enabled", "false"),
                 renamed.getProperties());
    assertEquals(Map.of("siteName", "siteName", "resourceName", "brand", "position", "START_OF_HEAD", "enabled", "true"),
                 stored.getProperties());
  }

  @Test
  void updateStaticResourceWritesTheContentOfAJsResourceInItsJs() throws Exception {
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("5");
    when(widgetStorage.getWidget(9L)).thenReturn(resource(9L, WidgetType.JS, "x()", "siteName"));
    when(widgetStorage.updateWidget(any())).thenAnswer(i -> i.getArgument(0));

    Widget updated = staticResourceService.updateStaticResource(9L, "siteName", null, "y()", null, true, USERNAME);

    assertEquals("y()", updated.getJs());
    assertNull(updated.getCss());
    assertEquals("true", updated.getProperties().get(ENABLED));
  }

  private Widget resource(long id, WidgetType type, String content, String siteName) {
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("siteName", siteName);
    properties.put("resourceName", "brand");
    properties.put("position", "START_OF_HEAD");
    properties.put("enabled", "true");
    Widget resource = new Widget();
    resource.setId(id);
    resource.setType(type);
    if (type == WidgetType.CSS) {
      resource.setCss(content);
    } else if (type == WidgetType.JS) {
      resource.setJs(content);
    }
    resource.setProperties(properties);
    return resource;
  }
}
