/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.ide.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.meeds.ide.service.WidgetService;
import io.meeds.layout.model.ApplicationCode;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.plugin.ApplicationCodePlugin;
import io.meeds.layout.service.ApplicationCodeService;

import jakarta.annotation.PostConstruct;

/**
 * The code read of an IDE widget's window for the layout descriptions: the
 * widget the window's preference names, else the code a never-rendered
 * window carries, else the catalog instance's widget. Glue over
 * {@link WidgetService}.
 */
@Component
public class WidgetApplicationCodePlugin implements ApplicationCodePlugin {

  @Autowired
  private WidgetService          widgetService;

  @Autowired(required = false)
  private ApplicationCodeService applicationCodeService;

  @PostConstruct
  public void init() {
    if (applicationCodeService != null) {
      applicationCodeService.addPlugin(this);
    }
  }

  @Override
  public String getPortletName() {
    return WidgetService.WIDGET_PORTLET_NAME;
  }

  @Override
  public ApplicationCode read(ApplicationSettingsContext context) {
    return widgetService.readWidgetCodeOnApplication(context);
  }

}
