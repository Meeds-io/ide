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
package io.meeds.ide.utils;

import io.meeds.ide.entity.WidgetEntity;
import io.meeds.ide.model.Widget;

public class Utils {

  private Utils() {
    // Static Utils methods
  }

  public static Widget fromEntity(WidgetEntity w) {
    return new Widget(w.getId(),
                      w.getPortletId(),
                      w.getHtml(),
                      w.getCss(),
                      w.getJs(),
                      w.getCreatorId(),
                      w.getModifierId(),
                      w.getModifiedDate(),
                      w.getCreatedDate());
  }

  public static WidgetEntity toEntity(Widget widget) {
    return new WidgetEntity(widget.getId() == null || widget.getId() == 0 ? null : widget.getId(),
                            widget.getPortletId(),
                            widget.getHtml(),
                            widget.getCss(),
                            widget.getJs(),
                            widget.getCreatorId(),
                            widget.getModifierId(),
                            widget.getModifiedDate(),
                            widget.getCreatedDate());
  }

}
