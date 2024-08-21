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
import static io.meeds.ide.service.WidgetService.IDE_WIDGET_UPDATED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.ide.model.Widget;
import io.meeds.ide.storage.WidgetStorage;
import io.meeds.layout.service.LayoutAclService;

@SpringBootTest(classes = {
                            WidgetService.class,
})
@ExtendWith(MockitoExtension.class)
public class WidgetServiceTest {

  private static final String USERNAME     = "testUser";

  private static final String HTML_CONTENT = "...html...";

  private static final String CSS_CONTENT  = "...css...";

  private static final String JS_CONTENT   = "...js...";

  @MockBean
  private LayoutAclService    layoutAclService;

  @MockBean
  private IdentityManager     identityManager;

  @MockBean
  private ListenerService     listenerService;

  @MockBean
  private WidgetStorage       widgetStorage;

  @Autowired
  private WidgetService       widgetService;

  @Mock
  private Widget              widget;

  @Mock
  private Identity            identity;

  @Test
  public void getWidget() throws ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> widgetService.getWidget(2l));
    when(widgetStorage.getWidget(2l)).thenReturn(widget);
    assertEquals(widget, widgetService.getWidget(2l));
  }

  @Test
  public void createWidget() throws ObjectAlreadyExistsException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> widgetService.createWidget(widget, USERNAME));
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);

    assertThrows(IllegalArgumentException.class, () -> widgetService.createWidget(widget, USERNAME));
    when(widget.getPortletId()).thenReturn(3l);

    when(widgetStorage.existsByPortletInstanceId(3l)).thenReturn(true);
    assertThrows(ObjectAlreadyExistsException.class, () -> widgetService.createWidget(widget, USERNAME));

    when(widgetStorage.existsByPortletInstanceId(3l)).thenReturn(false);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("5");
    when(widgetStorage.createWidget(widget)).thenReturn(widget);

    Widget savedWidget = widgetService.createWidget(widget, USERNAME);
    assertNotNull(savedWidget);
    verify(widget).setCreatorId(5l);
    verify(widget).setModifierId(5l);
    verify(widget).setCreatedDate(notNull());
    verify(widget).setModifiedDate(notNull());
    verify(widgetStorage).createWidget(widget);
    verify(listenerService).broadcast(IDE_WIDGET_CREATED_EVENT, null, widget);
  }

  @Test
  public void updateWidget() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> widgetService.updateWidget(widget, USERNAME));
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);


    when(widget.getId()).thenReturn(2l);
    when(widget.getHtml()).thenReturn(HTML_CONTENT);
    when(widget.getCss()).thenReturn(CSS_CONTENT);
    when(widget.getJs()).thenReturn(JS_CONTENT);
    when(widgetStorage.getWidget(2l)).thenReturn(null);

    assertThrows(ObjectNotFoundException.class, () -> widgetService.updateWidget(widget, USERNAME));

    when(widgetStorage.getWidget(2l)).thenReturn(widget);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("5");
    when(widgetStorage.updateWidget(widget)).thenReturn(widget);

    Widget savedWidget = widgetService.updateWidget(widget, USERNAME);
    assertNotNull(savedWidget);
    verify(widget).setHtml(HTML_CONTENT);
    verify(widget).setJs(JS_CONTENT);
    verify(widget).setCss(CSS_CONTENT);
    verify(widget).setModifierId(5l);
    verify(widget).setModifiedDate(notNull());
    verify(widgetStorage).updateWidget(widget);
    verify(listenerService).broadcast(IDE_WIDGET_UPDATED_EVENT, null, widget);
  }

}
