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
import io.meeds.social.navigation.constant.SidebarItemType;
import io.meeds.social.navigation.model.NavigationConfiguration;
import io.meeds.social.navigation.service.NavigationConfigurationService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.pom.spi.portlet.PortletBuilder;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static io.meeds.ide.service.WidgetService.IDE_WIDGET_CREATED_EVENT;
import static io.meeds.ide.service.WidgetService.NOT_ADMINISTRATOR_USER;

@Service
public class StaticResourceService {

  public static final String             SITE_NAME  = "siteName";

  public static final String             POSITION   = "position";

  public static final String             ENABLED    = "enabled";

  public static final String             CONTENT_ID = "ide/WidgetPortlet";

  public static final String             WIDGET_ID  = "widgetId";

  @Autowired
  private LayoutAclService               layoutAclService;

  @Autowired
  private IdentityManager                identityManager;

  @Autowired
  private ListenerService                listenerService;

  @Autowired
  private WidgetStorage                  widgetStorage;

  @Autowired
  private UserPortalConfigService        userPortalConfigService;

  private NavigationConfigurationService navigationConfigurationService;

  public List<Widget> getStaticResources(String siteName, String username) throws IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException(NOT_ADMINISTRATOR_USER);
    }
    Map<String, String> filters = new LinkedHashMap<>();
    filters.put(SITE_NAME, siteName);
    return widgetStorage.getWidgetsByProperties(filters);
  }

  public List<Application> getStaticResourceApplications(String siteName, String applicationPosition) {
    List<Application> applications = new ArrayList<>(getApplicationsBySite(siteName, applicationPosition));
    if (isMetaSiteNavigation(siteName)) {
      applications.addAll(getMetaSiteStaticResourceApplications(applicationPosition));
    }
    return applications;
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

  private List<Application> getApplicationsBySite(String siteName, String applicationPosition) {
    return getWidgetsBySite(siteName, applicationPosition).stream().map(this::toApplication).toList();
  }

  public List<Application> getMetaSiteStaticResourceApplications(String applicationPosition) {
    return getWidgetsBySite(userPortalConfigService.getMetaPortal(), applicationPosition).stream()
                                                                                         .map(this::toApplication)
                                                                                         .toList();
  }

  private List<Widget> getWidgetsBySite(String siteName, String applicationPosition) {
    Map<String, String> filters = createFilters(siteName, applicationPosition);
    List<Widget> widgets = new ArrayList<>(widgetStorage.getWidgetsByProperties(filters));
    if ("END_OF_BODY".equals(applicationPosition)) {
      filters.put(POSITION, "");
      widgets.addAll(widgetStorage.getWidgetsByProperties(filters));
    }
    return widgets;
  }

  private boolean isMetaSiteNavigation(String siteName) {
    NavigationConfiguration navigationConfiguration = getNavigationConfigurationService().getConfiguration();
    if (navigationConfiguration == null) {
      return false;
    }
    return navigationConfiguration.getSidebar()
                                  .getItems()
                                  .stream()
                                  .anyMatch(sidebarItem -> sidebarItem.getProperties() != null
                                      && Objects.equals(sidebarItem.getProperties().get(SITE_NAME), siteName)
                                      && SidebarItemType.SITE.equals(sidebarItem.getType()));
  }

  private Map<String, String> createFilters(String siteName, String applicationPosition) {
    Map<String, String> filters = new LinkedHashMap<>();
    filters.put(SITE_NAME, siteName);
    filters.put(POSITION, applicationPosition);
    filters.put(ENABLED, "true");
    return filters;
  }

  private Application toApplication(Widget widget) {
    Application app = Application.createPortletApplication();
    app.setState(new TransientApplicationState(CONTENT_ID,
                                               new PortletBuilder().add(WIDGET_ID, String.valueOf(widget.getId())).build()));
    app.setStorageId(String.valueOf(widget.getId()));
    return app;
  }

  private NavigationConfigurationService getNavigationConfigurationService() {
    if (navigationConfigurationService == null) {
      navigationConfigurationService = CommonsUtils.getService(NavigationConfigurationService.class);
    }
    return navigationConfigurationService;
  }
}
