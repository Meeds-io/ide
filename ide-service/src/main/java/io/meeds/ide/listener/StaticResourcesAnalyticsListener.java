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
import static io.meeds.ide.service.WidgetService.IDE_WIDGET_CREATED_EVENT;

import io.meeds.ide.model.Widget;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.analytics.model.StatisticData;
import jakarta.annotation.PostConstruct;

@Asynchronous
@Component
@Profile("layout")
public class StaticResourcesAnalyticsListener extends Listener<String, Widget> {

  private static final String STATISTICS_TYPE_PARAM      = "type";

  @Autowired
  private ListenerService     listenerService;

  @PostConstruct
  public void init() {
    listenerService.addListener(IDE_WIDGET_CREATED_EVENT, this);
  }

  @Override
  public void onEvent(Event<String, Widget> event) {
    Widget widget = event.getData();
    if (widget == null || MapUtils.isEmpty(widget.getProperties())) {
      return;
    }
    StatisticData statisticData = new StatisticData();
    statisticData.setModule("portal");
    statisticData.setSubModule("layout");
    statisticData.setOperation("addStatisticResource");
    statisticData.setUserId(widget.getCreatorId());
    statisticData.addKeyword(SITE_NAME, widget.getProperties().get(SITE_NAME));
    statisticData.addKeyword(STATISTICS_TYPE_PARAM, widget.getType());
    addStatisticData(statisticData);
  }
}
