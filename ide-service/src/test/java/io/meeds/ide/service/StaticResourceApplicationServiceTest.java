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
package io.meeds.ide.service;

import static io.meeds.ide.service.WidgetService.IDE_WIDGET_CREATED_EVENT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.ide.model.Widget;
import io.meeds.ide.storage.WidgetStorage;
import io.meeds.layout.service.LayoutAclService;

import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest(classes = { StaticResourceApplicationService.class, })
@ExtendWith(MockitoExtension.class)
class StaticResourceApplicationServiceTest {

  private static final String              USERNAME  = "testUser";

  private static final String              SITE_NAME = "siteName";

  @MockBean
  private LayoutAclService                 layoutAclService;

  @MockBean
  private IdentityManager                  identityManager;

  @MockBean
  private ListenerService                  listenerService;

  @MockBean
  private WidgetStorage                    widgetStorage;

  @Autowired
  private StaticResourceApplicationService staticResourceApplicationService;

  @Mock
  private Widget                           widget;

  @Mock
  private Identity                         identity;

  @Test
  void getStaticResourceApplications() throws IllegalAccessException {
    assertThrows(IllegalAccessException.class,
                 () -> staticResourceApplicationService.getStaticResourceApplications(SITE_NAME, USERNAME));

    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    Map<String, String> filters = new LinkedHashMap<>();
    filters.put("siteName", SITE_NAME);
    staticResourceApplicationService.getStaticResourceApplications(SITE_NAME, USERNAME);
    verify(widgetStorage).getWidgetsByProperties(filters);
  }

  @Test
  void createStaticResourceApplication() throws IllegalAccessException {
    assertThrows(IllegalAccessException.class,
                 () -> staticResourceApplicationService.createStaticResourceApplication(widget, USERNAME));
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);

    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("5");
    when(widgetStorage.createWidget(widget)).thenReturn(widget);

    Widget savedWidget = staticResourceApplicationService.createStaticResourceApplication(widget, USERNAME);
    assertNotNull(savedWidget);
    verify(widget).setCreatorId(5L);
    verify(widget).setModifierId(5L);
    verify(widget).setCreatedDate(notNull());
    verify(widget).setModifiedDate(notNull());
    verify(widgetStorage).createWidget(widget);
    verify(listenerService).broadcast(IDE_WIDGET_CREATED_EVENT, null, widget);
  }
}
