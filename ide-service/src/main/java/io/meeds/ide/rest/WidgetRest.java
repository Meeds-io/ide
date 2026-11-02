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
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
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

  private static final CacheControl CACHE_CONTROL         = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic();

  /** Every answer about an unpublished copy, the editor's 200 and the 404 alike */
  private static final CacheControl PRIVATE_CACHE_CONTROL = CacheControl.noStore().cachePrivate();

  @Autowired
  private WidgetService             widgetService;

  @GetMapping("{id}")
  @Operation(summary = "Retrieve a Web application widget", method = "GET", description = "This will retrieve a page template designated by its id")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "404", description = "Widget not found"),
  })
  public ResponseEntity<Widget> getWidget(
                                          HttpServletRequest request,
                                          @Parameter(description = "Widget identifier")
                                          @PathVariable("id")
                                          long id) {
    return serve(request, id, widget -> widget, null);
  }

  @GetMapping("{id}/html")
  @Operation(summary = "Retrieve a Web application widget html", method = "GET", description = "Retrieve a Web application widget html")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "404", description = "Widget not found"),
  })
  public ResponseEntity<String> getWidgetHtml(
                                              HttpServletRequest request,
                                              @Parameter(description = "Widget identifier")
                                              @PathVariable("id")
                                              long id) {
    return serve(request, id, Widget::getHtml, MediaType.TEXT_HTML);
  }

  @GetMapping("{id}/css")
  @Operation(summary = "Retrieve a Web application widget css", method = "GET", description = "Retrieve a Web application widget css")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "404", description = "Widget not found"),
  })
  public ResponseEntity<String> getWidgetCss(
                                              HttpServletRequest request,
                                              @Parameter(description = "Widget identifier")
                                              @PathVariable("id")
                                              long id) {
    return serve(request, id, Widget::getCss, MediaType.parseMediaType("text/css"));
  }

  @GetMapping("{id}/js")
  @Operation(summary = "Retrieve a Web application widget javascript", method = "GET", description = "Retrieve a Web application widget javascript")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "404", description = "Widget not found"),
  })
  public ResponseEntity<String> getWidgetJs(
                                              HttpServletRequest request,
                                              @Parameter(description = "Widget identifier")
                                              @PathVariable("id")
                                              long id) {
    return serve(request, id, widget -> "(function() {" + widget.getJs() + "})()", MediaType.parseMediaType("text/javascript"));
  }

  /**
   * Serves a widget read: a published widget as today, with the public
   * year-long cache; an unpublished copy to the editors of its layout only,
   * every answer about it, the 200 and the 404 alike, private and not
   * stored, and refused as 404 to anyone else; an unknown id answers the
   * same empty, private 404, so that no answer confirms an id exists
   */
  private <T> ResponseEntity<T> serve(HttpServletRequest request, long id, Function<Widget, T> body, MediaType mediaType) {
    try {
      Widget widget = widgetService.getWidget(id, request.getRemoteUser());
      boolean draftCopy = widgetService.isDraftCopy(widget);
      ResponseEntity.BodyBuilder response = ResponseEntity.ok();
      if (draftCopy) {
        response.cacheControl(PRIVATE_CACHE_CONTROL);
      } else if (mediaType != null) {
        response.cacheControl(CACHE_CONTROL);
      }
      if (mediaType != null) {
        response.contentType(mediaType);
      }
      return response.body(body.apply(widget));
    } catch (IllegalAccessException | ObjectNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(PRIVATE_CACHE_CONTROL).build();
    }
  }

  @DeleteMapping("{widgetId}")
  @Secured("administrators")
  @Operation(summary = "Delete a Web application widget ", method = "DELETE", description = "This deletes a Web application widget")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Not found"),
  })
  public void deleteWidget(HttpServletRequest request,
                           @Parameter(description = "Widget id", required = true)
                           @PathVariable("widgetId")
                           Long widgetId) {
    try {
      widgetService.deleteWidget(widgetId, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PutMapping("{id}")
  @Secured("administrators")
  @Operation(summary = "Update an existing a Web application widget", method = "PUT", description = "Update an existing a Web application widget")
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
