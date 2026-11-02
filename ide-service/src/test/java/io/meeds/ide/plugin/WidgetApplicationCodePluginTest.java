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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.meeds.ide.service.WidgetService;
import io.meeds.layout.model.ApplicationCode;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.service.ApplicationCodeService;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@SpringBootTest(classes = { WidgetApplicationCodePlugin.class })
@ExtendWith(MockitoExtension.class)
public class WidgetApplicationCodePluginTest {

  @MockitoBean
  private WidgetService               widgetService;

  @MockitoBean
  private ApplicationCodeService      applicationCodeService;

  @Autowired
  private WidgetApplicationCodePlugin plugin;

  @Test
  @SneakyThrows
  public void thePluginRegistersItselfForTheWidgetPortletAtStartup() {
    plugin.init();
    verify(applicationCodeService).addPlugin(plugin);
    assertEquals("WidgetPortlet", plugin.getPortletName());
    assertTrue(WidgetApplicationCodePlugin.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
  }

  @Test
  @SneakyThrows
  public void theReadDelegatesToTheService() {
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    ApplicationCode code = new ApplicationCode("<b/>", null, "x()");
    when(widgetService.readWidgetCodeOnApplication(context)).thenReturn(code);
    assertEquals(code, plugin.read(context));
  }

}
