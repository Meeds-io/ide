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
package io.meeds.ide.rest;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.ide.model.Widget;
import io.meeds.ide.service.WidgetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/widgets")
@Tag(name = "/ide/rest/widgets", description = "Managing Web Application Widgets")
public class WidgetRest {

  private static final CacheControl CACHE_CONTROL = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic();

  @Autowired
  private WidgetService             widgetService;

  @GetMapping("{id}")
  @Operation(summary = "Retrieve a Web application widget",
             method = "GET",
             description = "This will retrieve a page template designated by its id")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "404", description = "Widget not found"),
  })
  public Widget getWidget(
                          @Parameter(description = "Widget identifier")
                          @PathVariable("id")
                          long id) {
    try {
      return widgetService.getWidget(id);
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @GetMapping("{id}/html")
  @Operation(summary = "Retrieve a Web application widget html",
             method = "GET",
             description = "Retrieve a Web application widget html")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "404", description = "Widget not found"),
  })
  public ResponseEntity<String> getWidgetHtml(
                                              HttpServletRequest request,
                                              @Parameter(description = "Widget identifier")
                                              @PathVariable("id")
                                              long id) {
    try {
      Widget widget = widgetService.getWidget(id);
      return ResponseEntity.ok()
                           .cacheControl(CACHE_CONTROL)
                           .contentType(MediaType.TEXT_HTML)
                           .body(widget.getHtml());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @GetMapping("{id}/css")
  @Operation(summary = "Retrieve a Web application widget css",
             method = "GET",
             description = "Retrieve a Web application widget css")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "404", description = "Widget not found"),
  })
  public ResponseEntity<String> getWidgetCss(
                                             HttpServletRequest request,
                                             @Parameter(description = "Widget identifier")
                                             @PathVariable("id")
                                             long id) {
    try {
      Widget widget = widgetService.getWidget(id);
      return ResponseEntity.ok()
                           .cacheControl(CACHE_CONTROL)
                           .contentType(MediaType.parseMediaType("text/css"))
                           .body(widget.getCss());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @GetMapping("{id}/js")
  @Operation(summary = "Retrieve a Web application widget javascript",
             method = "GET",
             description = "Retrieve a Web application widget javascript")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "404", description = "Widget not found"),
  })
  public ResponseEntity<String> getWidgetJs(
                                            HttpServletRequest request,
                                            @Parameter(description = "Widget identifier")
                                            @PathVariable("id")
                                            long id) {
    try {
      Widget widget = widgetService.getWidget(id);
      return ResponseEntity.ok()
                           .cacheControl(CACHE_CONTROL)
                           .contentType(MediaType.parseMediaType("text/javascript"))
                           .body(widget.getJs());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @PutMapping("{id}")
  @Secured("administrators")
  @Operation(summary = "Update an existing a Web application widget",
             method = "PUT",
             description = "Update an existing a Web application widget")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void updateWidget(
                           HttpServletRequest request,
                           @Parameter(description = "Widget identifier")
                           @PathVariable("id")
                           long id,
                           @RequestBody
                           Widget widget) {
    try {
      widget.setId(id);
      widgetService.updateWidget(widget, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

}
