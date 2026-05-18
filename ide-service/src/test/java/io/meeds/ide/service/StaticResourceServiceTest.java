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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

import io.meeds.portal.navigation.service.NavigationConfigurationService;
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

import io.meeds.ide.model.Widget;
import io.meeds.ide.storage.WidgetStorage;
import io.meeds.layout.service.LayoutAclService;

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
}
