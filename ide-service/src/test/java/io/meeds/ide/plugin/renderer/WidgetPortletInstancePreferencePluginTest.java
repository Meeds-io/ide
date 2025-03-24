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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.meeds.ide.model.Widget;
import io.meeds.ide.service.WidgetService;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;

import io.meeds.layout.model.PortletInstancePreference;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = { WidgetPortletInstancePreferencePlugin.class, })
@ExtendWith(MockitoExtension.class)
class WidgetPortletInstancePreferencePluginTest {

  private static final String                   DATA_PREF_NAME      = "data.init";

  private static final String                   WIDGET_ID_PREF_NAME = "widgetId";

  private static final String                   SETTING_NAME        = "name";

  @MockBean
  private WidgetService                         widgetService;

  @Autowired
  private WidgetPortletInstancePreferencePlugin widgetPortletInstancePreferencePlugin;

  @Test
  void getPortletName() {
    assertEquals("WidgetPortlet", widgetPortletInstancePreferencePlugin.getPortletName());
  }

  @Test
  void generatePreferences() throws ObjectNotFoundException {
    Map<String, Preference> map = new HashMap<>();
    map.put(DATA_PREF_NAME, new Preference(SETTING_NAME, "value", false));
    map.remove(DATA_PREF_NAME);
    map.put(WIDGET_ID_PREF_NAME, new Preference(SETTING_NAME, "1", false));
    Widget widget = new Widget();
    widget.setJs("Js");
    widget.setHtml("Html");
    widget.setCss("Css");
    when(widgetService.getWidget(1L)).thenReturn(widget);
    Portlet preferences = new Portlet(map);
    List<PortletInstancePreference> generatedPreferences = widgetPortletInstancePreferencePlugin.generatePreferences(null, preferences);
    assertNotNull(generatedPreferences);
    assertEquals(2, generatedPreferences.size());
    assertEquals(DATA_PREF_NAME, generatedPreferences.getFirst().getName());
  }
}
