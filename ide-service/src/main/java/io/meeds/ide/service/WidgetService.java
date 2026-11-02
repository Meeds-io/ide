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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.ide.constant.WidgetType;
import io.meeds.ide.model.Widget;
import io.meeds.ide.storage.WidgetStorage;
import io.meeds.layout.model.ApplicationCode;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.util.JsonUtils;

@Service
public class WidgetService {

  public static final String IDE_WIDGET_CREATED_EVENT = "ide.widget.created";

  public static final String IDE_WIDGET_UPDATED_EVENT = "ide.widget.updated";

  public static final String NOT_ADMINISTRATOR_USER   = "User isn't an administrator";

  public static final String WIDGET_PORTLET_NAME      = "WidgetPortlet";

  public static final String WIDGET_ID_PREFERENCE     = "widgetId";

  public static final String PORTLET_INSTANCE_ID_PREFERENCE = "portletInstanceId";

  public static final String DATA_INIT_PREFERENCE     = "data.init";

  /** Marker of an unpublished copy: set at the copy, removed at publication only */
  public static final String DRAFT_COPY_PROPERTY      = "draftCopy";

  /** The storage id of the draft window the copy was made for */
  public static final String OWNER_WINDOW_PROPERTY    = "ownerWindow";

  /** The live page reference or site key the draft belongs to */
  public static final String OWNER_LAYOUT_PROPERTY    = "ownerLayout";

  public static final String WIDGET_CODE_NOT_SAVED    = "could not be saved: widget code";

  public static final String LAYOUT_BUSY              = ApplicationSettingsService.LAYOUT_BUSY;

  public static final String UNPUBLISHED_COPY         = "Widget %s is an unpublished copy its layout's editors read";

  private static final Log   LOG                      = ExoLogger.getLogger(WidgetService.class);

  @Autowired
  private LayoutAclService   layoutAclService;

  @Autowired
  private LayoutService      layoutService;

  @Autowired
  private ApplicationSettingsService applicationSettingsService;

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

  /**
   * The widget as a reader may see it: an unpublished copy is readable by the
   * editors of the layout it belongs to only, so that its id is never
   * confirmed to anyone else; any other widget is readable as today
   *
   * @param id the widget id
   * @param username the reader, null when anonymous
   * @return the widget
   * @throws ObjectNotFoundException when no widget has that id
   * @throws IllegalAccessException when the widget is an unpublished copy
   *           the reader may not edit the layout of
   */
  public Widget getWidget(long id, String username) throws ObjectNotFoundException, IllegalAccessException {
    Widget widget = getWidget(id);
    if (isDraftCopy(widget) && !canEditOwnerLayout(widget, username)) {
      throw new IllegalAccessException(String.format(UNPUBLISHED_COPY, id));
    }
    return widget;
  }

  /**
   * @param widget a widget
   * @return whether it is a copy made for a draft window and not published
   *         yet
   */
  public boolean isDraftCopy(Widget widget) {
    return widget != null && widget.getProperties() != null
           && Boolean.parseBoolean(widget.getProperties().get(DRAFT_COPY_PROPERTY));
  }

  /**
   * The code a window renders, as the layout reads describe it: the widget
   * its preference names, else the code a never-rendered window carries in
   * its data.init preference, else the catalog instance's widget, else none
   *
   * @param context the window
   * @return the code, null when the window has none yet
   * @throws ObjectNotFoundException when the widget the window names no
   *           longer exists
   */
  public ApplicationCode readWidgetCodeOnApplication(ApplicationSettingsContext context) throws ObjectNotFoundException {
    Widget widget = sourceWidget(context.getPreferences());
    return widget == null ? null : new ApplicationCode(widget.getHtml(), widget.getCss(), widget.getJs());
  }

  /**
   * Writes a window's widget code on the user's draft, by ownership: in
   * place when the window's widget is a copy this window owns on this
   * layout, otherwise into a copy the window is pointed at, so that the
   * published layout keeps rendering the previous code until publication.
   * The copy is created with its code, then the window's widgetId
   * preference is set to it and its data.init cleared in one preference
   * write; a failure of that write deletes the copy, which nothing
   * references. Runs under the per-window lock the settings writes take.
   *
   * @param context the window on the user's draft, its preferences loaded
   * @param liveLayoutKey the live page reference or site key the draft
   *          belongs to
   * @param html the HTML to write, null to keep the current one
   * @param css the CSS to write, null to keep the current one
   * @param js the JavaScript to write, null to keep the current one
   * @param username the administrator writing
   * @return the widget the window now renders
   * @throws ObjectNotFoundException when the window no longer exists
   * @throws IllegalAccessException when the user is not an administrator or
   *           may not edit the layout
   */
  public Widget updateWidgetCodeOnApplication(ApplicationSettingsContext context, // NOSONAR
                                              String liveLayoutKey,
                                              String html,
                                              String css,
                                              String js,
                                              String username) throws ObjectNotFoundException, IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException(NOT_ADMINISTRATOR_USER);
    }
    String lockKey = context.isSiteContainer() ? String.valueOf(context.getSiteKey()) : context.getPageKey().format();
    return applicationSettingsService.runLocked(lockKey, context.getApplicationId(), () -> {
      Portlet stored = layoutService.load(new PersistentApplicationState(String.valueOf(context.getApplicationId())));
      if (stored == null) {
        throw new ObjectNotFoundException(ApplicationSettingsService.APPLICATION_NOT_FOUND);
      }
      context.setPreferences(preferencesOf(stored));
      Widget current = widgetOf(context.getPreferences().get(WIDGET_ID_PREFERENCE));
      if (isOwnedCopy(current, context, liveLayoutKey)) {
        current.setHtml(html == null ? current.getHtml() : html);
        current.setCss(css == null ? current.getCss() : css);
        current.setJs(js == null ? current.getJs() : js);
        return updateWidget(current, username);
      }
      Widget source = current == null ? sourceWidget(context.getPreferences()) : current;
      Widget copy = new Widget();
      copy.setHtml(html == null && source != null ? source.getHtml() : html);
      copy.setCss(css == null && source != null ? source.getCss() : css);
      copy.setJs(js == null && source != null ? source.getJs() : js);
      Map<String, String> markers = new LinkedHashMap<>();
      markers.put(DRAFT_COPY_PROPERTY, Boolean.TRUE.toString());
      markers.put(OWNER_WINDOW_PROPERTY, String.valueOf(context.getApplicationId()));
      markers.put(OWNER_LAYOUT_PROPERTY, liveLayoutKey);
      copy.setProperties(markers);
      Widget created;
      try {
        created = createWidget(copy, username);
      } catch (ObjectAlreadyExistsException e) {
        throw new IllegalStateException(WIDGET_CODE_NOT_SAVED, e);
      }
      Map<String, String> preferences = new HashMap<>();
      preferences.put(WIDGET_ID_PREFERENCE, String.valueOf(created.getId()));
      preferences.put(DATA_INIT_PREFERENCE, null);
      try {
        applicationSettingsService.updatePreferences(context, preferences);
      } catch (ObjectNotFoundException | IllegalAccessException | RuntimeException e) {
        compensateCopy(created);
        throw e;
      }
      return created;
    });
  }

  /**
   * Marks published every unpublished copy a window of the published layout
   * references and whose owner layout is that layout; the ownership markers
   * stay, since ownership keeps deciding copy against in-place on the next
   * draft. Called by the publication listener alone.
   *
   * @param layoutKey the live page reference or site key just published
   * @param publishedLayout the published layout's tree, read back after the
   *          save
   */
  public void publishDraftCopies(String layoutKey, ModelObject publishedLayout) {
    Map<String, String> marked = new HashMap<>();
    marked.put(DRAFT_COPY_PROPERTY, Boolean.TRUE.toString());
    marked.put(OWNER_LAYOUT_PROPERTY, layoutKey);
    List<Widget> copies = widgetStorage.getWidgetsByProperties(marked);
    if (copies == null || copies.isEmpty()) {
      return;
    }
    Set<Long> referenced = new HashSet<>();
    collectWidgetIds(publishedLayout, referenced);
    copies.stream().filter(copy -> referenced.contains(copy.getId())).forEach(copy -> {
      Map<String, String> properties = new LinkedHashMap<>(copy.getProperties());
      properties.remove(DRAFT_COPY_PROPERTY);
      copy.setProperties(properties);
      widgetStorage.updateWidget(copy);
    });
  }

  /**
   * @param siteKey a site key
   * @return the key a copy's owner layout records for a site, the form the
   *         publication listener and the REST gating rebuild
   */
  public static String layoutKey(SiteKey siteKey) {
    return siteKey.getTypeName() + "::" + siteKey.getName();
  }

  private boolean canEditOwnerLayout(Widget widget, String username) {
    String ownerLayout = widget.getProperties().get(OWNER_LAYOUT_PROPERTY);
    if (StringUtils.isBlank(username) || StringUtils.isBlank(ownerLayout)) {
      return false;
    }
    String[] parts = StringUtils.splitByWholeSeparator(ownerLayout, "::");
    try {
      if (parts.length == 3) {
        return layoutAclService.canEditPage(PageKey.parse(ownerLayout), username);
      } else if (parts.length == 2) {
        return layoutAclService.canEditSite(new SiteKey(parts[0], parts[1]), username);
      }
    } catch (RuntimeException e) {
      LOG.debug("Owner layout {} of widget {} could not be resolved", ownerLayout, widget.getId(), e);
    }
    return false;
  }

  private boolean isOwnedCopy(Widget widget, ApplicationSettingsContext context, String liveLayoutKey) {
    return isDraftCopy(widget)
           && StringUtils.equals(widget.getProperties().get(OWNER_WINDOW_PROPERTY), String.valueOf(context.getApplicationId()))
           && StringUtils.equals(widget.getProperties().get(OWNER_LAYOUT_PROPERTY), liveLayoutKey);
  }

  private Widget sourceWidget(Map<String, String> preferences) throws ObjectNotFoundException {
    if (preferences == null) {
      return null;
    }
    String widgetId = preferences.get(WIDGET_ID_PREFERENCE);
    if (NumberUtils.isDigits(widgetId)) {
      return getWidget(Long.parseLong(widgetId));
    }
    String dataInit = preferences.get(DATA_INIT_PREFERENCE);
    if (StringUtils.isNotBlank(dataInit)) {
      Widget imported = JsonUtils.fromJsonString(dataInit, Widget.class);
      if (imported != null) {
        return imported;
      }
    }
    String portletInstanceId = preferences.get(PORTLET_INSTANCE_ID_PREFERENCE);
    return NumberUtils.isDigits(portletInstanceId) ? getWidgetByPortletId(Long.parseLong(portletInstanceId)) : null;
  }

  private Widget widgetOf(String widgetId) {
    return NumberUtils.isDigits(widgetId) ? widgetStorage.getWidget(Long.parseLong(widgetId)) : null;
  }

  private void compensateCopy(Widget created) {
    try {
      widgetStorage.deleteWidgetById(created.getId());
    } catch (Exception e) {
      LOG.debug("Widget copy {} could not be deleted after its window write failed", created.getId(), e);
    }
  }

  private void collectWidgetIds(ModelObject object, Set<Long> ids) {
    if (object instanceof Container container && container.getChildren() != null) {
      container.getChildren().forEach(child -> collectWidgetIds(child, ids));
    } else if (object instanceof Application application) {
      Portlet preferences = layoutService.load(application.getState());
      String widgetId = preferences == null ? null : preferences.getValue(WIDGET_ID_PREFERENCE);
      if (NumberUtils.isDigits(widgetId)) {
        ids.add(Long.parseLong(widgetId));
      }
    }
  }

  private static Map<String, String> preferencesOf(Portlet portlet) {
    Map<String, String> preferences = new LinkedHashMap<>();
    if (portlet != null) {
      for (Preference preference : portlet) {
        preferences.put(preference.getName(), preference.getValue());
      }
    }
    return preferences;
  }

  public Widget getWidgetByPortletId(long portletInstanceId) {
    return widgetStorage.getWidgetByPortletId(portletInstanceId);
  }

  public Widget createWidget(Widget widget, String username) throws ObjectAlreadyExistsException, IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException(NOT_ADMINISTRATOR_USER);
    }
    if (widget.getPortletId() != null && widgetStorage.existsByPortletInstanceId(widget.getPortletId())) {
      throw new ObjectAlreadyExistsException(String.format("Widget for portlet with id %s already exists",
                                                           widget.getPortletId()));
    }
    Identity identity = identityManager.getOrCreateUserIdentity(username);
    widget.setCreatorId(Long.parseLong(identity.getId()));
    widget.setModifierId(Long.parseLong(identity.getId()));
    widget.setCreatedDate(LocalDateTime.now());
    widget.setModifiedDate(LocalDateTime.now());
    widget.setType(WidgetType.APP);
    Widget createdWidget = widgetStorage.createWidget(widget);
    listenerService.broadcast(IDE_WIDGET_CREATED_EVENT, null, createdWidget);
    return createdWidget;
  }

  public Widget updateWidget(Widget widget, String username) throws ObjectNotFoundException, IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException(NOT_ADMINISTRATOR_USER);
    }
    Widget existingWidget = widgetStorage.getWidget(widget.getId());
    if (existingWidget == null) {
      throw new ObjectNotFoundException(String.format("Widget with id %s doesn't exists", widget.getId()));
    }
    Identity identity = identityManager.getOrCreateUserIdentity(username);
    existingWidget.setHtml(widget.getHtml());
    existingWidget.setCss(widget.getCss());
    existingWidget.setJs(widget.getJs());
    existingWidget.setProperties(widget.getProperties());
    existingWidget.setModifierId(Long.parseLong(identity.getId()));
    existingWidget.setModifiedDate(LocalDateTime.now());
    Widget updatedWidget = widgetStorage.updateWidget(existingWidget);
    listenerService.broadcast(IDE_WIDGET_UPDATED_EVENT, null, updatedWidget);
    return updatedWidget;
  }

  public void deleteWidget(long widgetId, String username) throws ObjectNotFoundException, IllegalAccessException {
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException(NOT_ADMINISTRATOR_USER);
    }
    widgetStorage.deleteWidgetById(widgetId);
  }

}
