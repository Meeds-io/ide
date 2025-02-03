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

import io.meeds.ide.model.Widget;
import io.meeds.ide.storage.WidgetStorage;
import io.meeds.layout.service.LayoutAclService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.pom.spi.portlet.PortletBuilder;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.meeds.ide.service.WidgetService.IDE_WIDGET_CREATED_EVENT;
import static io.meeds.ide.service.WidgetService.NOT_ADMINISTRATOR_USER;

@Service
public class StaticResourceService {

  public static final String SITE_NAME = "siteName";

  public static final String POSITION  = "position";

  public static final String ENABLED   = "enabled";

  @Autowired
  private LayoutAclService   layoutAclService;

  @Autowired
  private IdentityManager    identityManager;

  @Autowired
  private ListenerService    listenerService;

  @Autowired
  private WidgetStorage      widgetStorage;

  public List<Widget> getStaticResources(String siteName, String username) throws IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException(NOT_ADMINISTRATOR_USER);
    }
    Map<String, String> filters = new LinkedHashMap<>();
    filters.put(SITE_NAME, siteName);
    return widgetStorage.getWidgetsByProperties(filters);
  }

  public List<Application> getStaticResourceApplications(String siteName, String applicationPosition) {
    Map<String, String> filters = new LinkedHashMap<>();
    filters.put(SITE_NAME, siteName);
    filters.put(POSITION, applicationPosition);
    filters.put(ENABLED, "true");
    List<Widget> widgets = new ArrayList<>(widgetStorage.getWidgetsByProperties(filters));
    if (applicationPosition.equals("END_OF_BODY")) {
      filters.put(POSITION, "");
      widgets.addAll(widgetStorage.getWidgetsByProperties(filters));
    }
    return widgets.stream().map(this::toApplication).toList();
  }

  public Widget createStaticResource(Widget widget, String username) throws IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException(NOT_ADMINISTRATOR_USER);
    }
    Identity identity = identityManager.getOrCreateUserIdentity(username);
    long userId = Long.parseLong(identity.getId());
    widget.setCreatorId(userId);
    widget.setModifierId(userId);
    widget.setCreatedDate(LocalDateTime.now());
    widget.setModifiedDate(LocalDateTime.now());
    Widget createdWidget = widgetStorage.createWidget(widget);
    listenerService.broadcast(IDE_WIDGET_CREATED_EVENT, null, createdWidget);
    return createdWidget;
  }

  private Application toApplication(Widget widget) {
    Application app = Application.createPortletApplication();
    app.setState(new TransientApplicationState("ide/WidgetPortlet",
                                               new PortletBuilder().add("widgetId", String.valueOf(widget.getId())).build()));
    app.setStorageId(String.valueOf(widget.getId()));
    return app;
  }
}
