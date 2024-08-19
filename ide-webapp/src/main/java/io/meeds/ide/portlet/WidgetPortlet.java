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
package io.meeds.ide.portlet;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.webui.Utils;

import io.meeds.ide.model.Widget;
import io.meeds.ide.service.WidgetService;
import io.meeds.social.portlet.CMSPortlet;

public class WidgetPortlet extends CMSPortlet {

  private static final String OBJECT_TYPE               = "widget";

  private static final String WIDGET_ID_PARAM           = "widgetId";

  private static final String PORTLET_INSTANCE_ID_PARAM = "portletInstanceId";

  private String              editDispatchedPath;

  @Override
  public void init(PortletConfig config) throws PortletException {
    super.init(config);
    this.editDispatchedPath = config.getInitParameter("portlet-edit-dispatched-file-path");
    this.contentType = OBJECT_TYPE;
  }

  @Override
  public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
    checkPreferences(request);
    super.doView(request, response);
  }

  @Override
  protected void doEdit(RenderRequest request, RenderResponse response) throws PortletException, IOException {
    checkPreferences(request);
    PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher(editDispatchedPath);
    PortletPreferences preferences = request.getPreferences();
    if (preferences != null) {
      Set<Entry<String, String[]>> preferencesEntries = preferences.getMap().entrySet();
      for (Entry<String, String[]> entry : preferencesEntries) {
        request.setAttribute(entry.getKey(), entry.getValue());
      }
    }
    prd.include(request, response);
  }

  private void checkPreferences(RenderRequest request) throws PortletException {
    PortletPreferences preferences = request.getPreferences();
    if (preferences.getValue(PORTLET_INSTANCE_ID_PARAM, null) != null
        && preferences.getValue(WIDGET_ID_PARAM, null) == null) {
      Widget widget = new Widget();
      long portletInstanceId = Long.parseLong(preferences.getValue(PORTLET_INSTANCE_ID_PARAM, null));
      widget.setPortletId(portletInstanceId);
      Identity identity = Utils.getViewerIdentity();
      try {
        widget = ExoContainerContext.getService(WidgetService.class)
                                    .createWidget(widget, identity.getRemoteId());
        preferences.setValue(WIDGET_ID_PARAM, String.valueOf(widget.getId()));
        savePreference(WIDGET_ID_PARAM, String.valueOf(widget.getId()));
      } catch (IllegalAccessException e) {
        throw new PortletException("User not allowed to change Widget settings", e);
      } catch (ObjectAlreadyExistsException e) {
        throw new PortletException("Associated Widget already exists for portlet with instance id " + portletInstanceId, e);
      }
    }
  }

}
