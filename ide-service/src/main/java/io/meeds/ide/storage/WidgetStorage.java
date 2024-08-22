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
package io.meeds.ide.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import io.meeds.ide.dao.WidgetDAO;
import io.meeds.ide.entity.WidgetEntity;
import io.meeds.ide.model.Widget;
import io.meeds.ide.utils.Utils;

@Component
public class WidgetStorage {

  private static final String CACHE_NAME = "ide.widget";

  @Autowired
  private WidgetDAO           widgetDAO;

  @Cacheable(CACHE_NAME)
  public Widget getWidget(Long id) {
    return widgetDAO.findById(id)
                    .map(Utils::fromEntity)
                    .orElse(null);
  }

  public Widget createWidget(Widget widget) {
    widget.setId(null);
    WidgetEntity widgetEntity = Utils.toEntity(widget);
    widgetEntity = widgetDAO.save(widgetEntity);
    return Utils.fromEntity(widgetEntity);
  }

  @CacheEvict(value = CACHE_NAME, key = "#p0.id")
  public Widget updateWidget(Widget widget) {
    WidgetEntity widgetEntity = Utils.toEntity(widget);
    widgetEntity = widgetDAO.save(widgetEntity);
    return Utils.fromEntity(widgetEntity);
  }

  public Widget getWidgetByPortletId(long portletInstanceId) {
    WidgetEntity widgetEntity = widgetDAO.findByPortletId(portletInstanceId);
    return widgetEntity == null ? null : Utils.fromEntity(widgetEntity);
  }

  public boolean existsByPortletInstanceId(Long portletInstanceId) {
    return widgetDAO.existsByPortletId(portletInstanceId);
  }

}
