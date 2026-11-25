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
 * A CSS or JavaScript static resource of a site, as the static resource
 * tools answer it
 *
 * @param resourceId the resource's id
 * @param type CSS or JS
 * @param name the resource's name
 * @param content the CSS or the JavaScript
 * @param position where the resource is rendered on the site's pages
 * @param enabled whether the resource is rendered
 */
public record StaticResourceResult(@JsonProperty("resource_id") long resourceId,
                                   @JsonProperty("type") String type,
                                   @JsonProperty("name") String name,
                                   @JsonProperty("content") String content,
                                   @JsonProperty("position") String position,
                                   @JsonProperty("enabled") boolean enabled) {
}
