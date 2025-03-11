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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.ide.plugin.renderer;

import java.util.Collections;
import java.util.List;

import io.meeds.ide.model.Widget;
import io.meeds.ide.service.WidgetService;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;

import lombok.SneakyThrows;

@Service
public class WidgetPortletInstancePreferencePlugin implements PortletInstancePreferencePlugin {

  private static final String WIDGET_ID_PARAM           = "widgetId";

  private static final String DATA_INIT_PREFERENCE_NAME = "data.init";

  private final WidgetService widgetService;

  public WidgetPortletInstancePreferencePlugin(WidgetService widgetService) {
    this.widgetService = widgetService;
  }

  @Override
  public String getPortletName() {
    return "WidgetPortlet";
  }

  @Override
  @SneakyThrows
  public List<PortletInstancePreference> generatePreferences(Application application, Portlet preferences) {
    long widgetId = 0L;
    if (preferences != null) {
      widgetId = Long.parseLong(preferences.getPreference(WIDGET_ID_PARAM).getValue());
    }
    Widget widget = widgetService.getWidget(widgetId);
    JSONObject json = new JSONObject();
    json.put("html", widget.getHtml());
    json.put("js", widget.getJs());
    json.put("css", widget.getCss());
    return Collections.singletonList(new PortletInstancePreference(DATA_INIT_PREFERENCE_NAME, json.toString()));
  }
}
