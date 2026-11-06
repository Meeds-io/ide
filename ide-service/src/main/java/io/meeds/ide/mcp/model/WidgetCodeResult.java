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
package io.meeds.ide.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * What update_widget_code answers: the window on the user's draft, the
 * widget it now renders and its code
 *
 * @param applicationId the window's storage id on the draft
 * @param widgetId the widget the window renders, the draft's own copy
 * @param html the widget's HTML
 * @param css the widget's CSS
 * @param js the widget's JavaScript
 * @param draftCreated whether this call created the user's draft, whose
 *          ids differ from the published layout's
 */
public record WidgetCodeResult(@JsonProperty("application_id") String applicationId,
                               @JsonProperty("widget_id") long widgetId,
                               @JsonProperty("html") String html,
                               @JsonProperty("css") String css,
                               @JsonProperty("js") String js,
                               @JsonProperty("draft_created") boolean draftCreated) {
}
