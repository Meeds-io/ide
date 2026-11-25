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

import io.meeds.ide.constant.WidgetType;
import io.meeds.ide.model.Widget;
import io.meeds.ide.storage.WidgetStorage;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.portal.navigation.service.NavigationConfigurationService;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.exception.ObjectNotFoundException;
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
import static io.meeds.ide.service.WidgetService.IDE_WIDGET_UPDATED_EVENT;
import static io.meeds.ide.service.WidgetService.NOT_ADMINISTRATOR_USER;

@Service
public class StaticResourceService {

  public static final String             SITE_NAME              = "siteName";

  public static final String             POSITION               = "position";

  public static final String             ENABLED                = "enabled";

  public static final String             RESOURCE_NAME          = "resourceName";

  public static final String             RESOURCE_NOT_FOUND     = "Static resource doesn't exist on the site";

  public static final String             CONTENT_ID             = "ide/WidgetPortlet";

  public static final String             WIDGET_ID              = "widgetId";

  private static final String            SPACE_SITE_TYPE_PREFIX = "/spaces/";

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

  /**
   * Retrieves a CSS or JavaScript static resource of a site
   *
   * @param id the resource's id
   * @param siteName the name of the site the resource is expected on
   * @param username the user who reads
   * @return the resource
   * @throws IllegalAccessException when the user is not an administrator
   * @throws ObjectNotFoundException when no CSS or JavaScript static resource
   *           of that site has this id
   */
  public Widget getStaticResource(long id, String siteName, String username) throws IllegalAccessException,
                                                                             ObjectNotFoundException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException(NOT_ADMINISTRATOR_USER);
    }
    Widget widget = widgetStorage.getWidget(id);
    if (widget == null
        || (widget.getType() != WidgetType.CSS && widget.getType() != WidgetType.JS)
        || widget.getProperties() == null
        || StringUtils.isBlank(siteName)
        || !StringUtils.equals(siteName, widget.getProperties().get(SITE_NAME))) {
      throw new ObjectNotFoundException(RESOURCE_NOT_FOUND);
    }
    return widget;
  }

  /**
   * Updates a CSS or JavaScript static resource of a site: each given value
   * replaces the stored one, a null one keeps it
   *
   * @param id the resource's id
   * @param siteName the name of the site the resource is expected on
   * @param name the resource's name, null to keep the stored one
   * @param content the CSS or JavaScript, null to keep the stored one
   * @param position where the resource is rendered, null to keep the stored
   *          one
   * @param enabled whether the resource is rendered, null to keep the stored
   *          value
   * @param username the user who writes
   * @return the updated resource
   * @throws IllegalAccessException when the user is not an administrator
   * @throws ObjectNotFoundException when no CSS or JavaScript static resource
   *           of that site has this id
   */
  public Widget updateStaticResource(long id, // NOSONAR
                                     String siteName,
                                     String name,
                                     String content,
                                     String position,
                                     Boolean enabled,
                                     String username) throws IllegalAccessException, ObjectNotFoundException {
    Widget existingWidget = getStaticResource(id, siteName, username);
    Map<String, String> properties = new LinkedHashMap<>(existingWidget.getProperties());
    if (name != null) {
      properties.put(RESOURCE_NAME, name);
    }
    if (position != null) {
      properties.put(POSITION, position);
    }
    if (enabled != null) {
      properties.put(ENABLED, String.valueOf(enabled.booleanValue()));
    }
    Identity identity = identityManager.getOrCreateUserIdentity(username);
    Widget widget = new Widget(existingWidget.getId(),
                               existingWidget.getPortletId(),
                               existingWidget.getHtml(),
                               content != null && existingWidget.getType() == WidgetType.CSS ? content : existingWidget.getCss(),
                               content != null && existingWidget.getType() == WidgetType.JS ? content : existingWidget.getJs(),
                               existingWidget.getCreatorId(),
                               Long.parseLong(identity.getId()),
                               LocalDateTime.now(),
                               existingWidget.getCreatedDate(),
                               existingWidget.getType(),
                               properties);
    Widget updatedWidget = widgetStorage.updateWidget(widget);
    listenerService.broadcast(IDE_WIDGET_UPDATED_EVENT, null, updatedWidget);
    return updatedWidget;
  }

  public List<Application> getStaticResourceApplications(String siteName, String applicationPosition) {
    List<Application> applications = new ArrayList<>(getApplicationsBySite(siteName, applicationPosition));
    if (!StringUtils.equals(userPortalConfigService.getMetaPortal(), siteName)
        && (getNavigationConfigurationService().isMetaSiteNavigation(siteName) || siteName.startsWith(SPACE_SITE_TYPE_PREFIX))) {
      applications.addAll(getApplicationsBySite(userPortalConfigService.getMetaPortal(), applicationPosition));
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

  private List<Widget> getWidgetsBySite(String siteName, String applicationPosition) {
    Map<String, String> filters = createFilters(siteName, applicationPosition);
    List<Widget> widgets = new ArrayList<>(widgetStorage.getWidgetsByProperties(filters));
    if ("END_OF_BODY".equals(applicationPosition)) {
      filters.put(POSITION, "");
      widgets.addAll(widgetStorage.getWidgetsByProperties(filters));
    }
    return widgets;
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
