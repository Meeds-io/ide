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

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.ide.model.Widget;
import io.meeds.ide.storage.WidgetStorage;
import io.meeds.layout.service.LayoutAclService;

@Service
public class WidgetService {

  public static final String IDE_WIDGET_CREATED_EVENT = "ide.widget.created";

  public static final String IDE_WIDGET_UPDATED_EVENT = "ide.widget.updated";

  @Autowired
  private LayoutAclService   layoutAclService;

  @Autowired
  private IdentityManager    identityManager;

  @Autowired
  private ListenerService    listenerService;

  @Autowired
  private WidgetStorage      widgetStorage;

  public Widget getWidget(long id) throws ObjectNotFoundException {
    Widget widget = widgetStorage.getWidget(id);
    if (widget == null) {
      throw new ObjectNotFoundException(String.format("Widget with id %s doesn't exists", id));
    }
    return widget;
  }

  public Widget getWidgetByPortletId(long portletInstanceId) {
    return widgetStorage.getWidgetByPortletId(portletInstanceId);
  }

  public Widget createWidget(Widget widget, String username) throws ObjectAlreadyExistsException, IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't an administrator");
    }
    if (widget.getPortletId() == null || widget.getPortletId() == 0) {
      throw new IllegalArgumentException("Widget portlet instance id is mandatory");
    }
    if (widgetStorage.existsByPortletInstanceId(widget.getPortletId())) {
      throw new ObjectAlreadyExistsException(String.format("Widget for portlet with id %s already exists",
                                                           widget.getPortletId()));
    }
    Identity identity = identityManager.getOrCreateUserIdentity(username);
    widget.setCreatorId(Long.parseLong(identity.getId()));
    widget.setModifierId(Long.parseLong(identity.getId()));
    widget.setCreatedDate(LocalDateTime.now());
    widget.setModifiedDate(LocalDateTime.now());
    Widget createdWidget = widgetStorage.createWidget(widget);
    listenerService.broadcast(IDE_WIDGET_CREATED_EVENT, null, createdWidget);
    return createdWidget;
  }

  public Widget updateWidget(Widget widget, String username) throws ObjectNotFoundException, IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException("User isn't an administrator");
    }
    Widget existingWidget = widgetStorage.getWidget(widget.getId());
    if (existingWidget == null) {
      throw new ObjectNotFoundException(String.format("Widget with id %s doesn't exists", widget.getId()));
    }
    Identity identity = identityManager.getOrCreateUserIdentity(username);
    existingWidget.setHtml(widget.getHtml());
    existingWidget.setCss(widget.getCss());
    existingWidget.setJs(widget.getJs());
    existingWidget.setModifierId(Long.parseLong(identity.getId()));
    existingWidget.setModifiedDate(LocalDateTime.now());
    Widget updatedWidget = widgetStorage.updateWidget(existingWidget);
    listenerService.broadcast(IDE_WIDGET_UPDATED_EVENT, null, updatedWidget);
    return updatedWidget;
  }

}
