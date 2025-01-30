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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package io.meeds.ide.rest;

import io.meeds.ide.model.Widget;
import io.meeds.ide.service.StaticResourceApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/static/resources")
@Tag(name = "/ide/rest/static/resources", description = "Managing Static Resource Applications")
public class StaticResourceApplicationRest {

  @Autowired
  private StaticResourceApplicationService staticResourceApplicationService;

  @GetMapping
  @Secured("administrators")
  @Operation(summary = "Retrieve a static resource applications", method = "GET", description = "Retrieve a static resource applications")
  @ApiResponses(value = { 
          @ApiResponse(responseCode = "200", description = "Request fulfilled"), 
          @ApiResponse(responseCode = "404", description = "Widget not found"), })
  public List<Widget> getStaticResourceApplications(HttpServletRequest request,
                                                    @RequestParam("siteName") String siteName) {
    try {
      return staticResourceApplicationService.getStaticResourceApplications(siteName, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PostMapping
  @Secured("administrators")
  @Operation(summary = "Create a Static Resource", method = "POST", description = "Create a Static Resource")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "403", description = "Forbidden"), })
  public Widget createStaticResourceApplication(HttpServletRequest request,
                                                @RequestBody Widget widget) {
    try {
      return staticResourceApplicationService.createStaticResourceApplication(widget, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }
}
