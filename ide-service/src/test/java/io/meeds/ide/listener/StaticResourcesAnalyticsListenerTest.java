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
package io.meeds.ide.listener;

import static io.meeds.analytics.utils.AnalyticsUtils.addStatisticData;
import static io.meeds.ide.service.StaticResourceService.SITE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.meeds.ide.constant.WidgetType;
import io.meeds.ide.model.Widget;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = { StaticResourcesAnalyticsListener.class, })
@TestPropertySource(properties = { "spring.profiles.active=layout", })
class StaticResourcesAnalyticsListenerTest {

  @MockBean
  private ListenerService                  listenerService;

  @MockBean
  private Event<String, Widget>            event;

  @Mock
  private Widget                           widget;

  @Autowired
  private StaticResourcesAnalyticsListener listener;

  @Test
  void onEvent() {
    try (MockedStatic<AnalyticsUtils> mockedStatic = mockStatic(AnalyticsUtils.class)) {

      // When
      Map<String, String> params = new HashMap<>();
      params.put(SITE_NAME, "siteName");
      when(widget.getProperties()).thenReturn(params);
      when(widget.getType()).thenReturn(WidgetType.APP);
      when(widget.getCreatorId()).thenReturn(1L);
      when(event.getData()).thenReturn(widget);
      listener.onEvent(event);

      // Then
      ArgumentCaptor<StatisticData> captor = ArgumentCaptor.forClass(StatisticData.class);
      mockedStatic.verify(() -> AnalyticsUtils.addStatisticData(argThat(statisticData -> {
        assertEquals("portal", statisticData.getModule());
        assertEquals("layout", statisticData.getSubModule());
        assertEquals("addStatisticResource", statisticData.getOperation());
        assertEquals("siteName", statisticData.getParameters().get(SITE_NAME));
        assertEquals("APP", statisticData.getParameters().get("type"));
        assertEquals(1L, statisticData.getUserId());
        return true;
      })), times(1));
      addStatisticData(captor.capture());
    }
  }
}
