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
package io.meeds.ide.mcp;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.ide.mcp.model.WidgetCodeResult;
import io.meeds.ide.model.Widget;
import io.meeds.ide.service.WidgetService;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.model.DraftWindows;
import io.meeds.layout.service.ApplicationDraftService;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.mcp.server.plugin.McpToolPlugin;

/**
 * The MCP tool that writes an IDE widget's code on the user's draft, the one
 * tool of this add-on: argument mapping and exception translation only, the
 * copy-on-write rule being {@link WidgetService}'s. Every refusal is a fixed
 * text naming the object or the option it concerns; no engine message
 * reaches the wire.
 */
@Service
@Profile("mcp-server")
public class WidgetMcpTool implements McpToolPlugin {

  public static final String      ADMINISTRATORS_ONLY         = "not allowed: administrators only";

  public static final String      SITE_ID_NOT_ACCEPTED        = "value is not accepted for site_id";

  public static final String      PAGE_REF_NOT_ACCEPTED       = "value is not accepted for page_ref";

  public static final String      APPLICATION_ID_NOT_ACCEPTED = ApplicationDraftService.APPLICATION_ID_NOT_ACCEPTED;

  public static final String      APPLICATION_NOT_FOUND       = ApplicationDraftService.APPLICATION_NOT_FOUND;

  public static final String      HTML_NOT_ACCEPTED           = "value is not accepted for html";

  public static final String      CSS_NOT_ACCEPTED            = "value is not accepted for css";

  public static final String      JS_NOT_ACCEPTED             = "value is not accepted for js";

  public static final String      WIDGET_CODE_NOT_SAVED       = WidgetService.WIDGET_CODE_NOT_SAVED;

  public static final String      WIDGET_PORTLET_SUFFIX       = "/" + WidgetService.WIDGET_PORTLET_NAME;

  public static final long        DEFAULT_MAX_BYTES           = 1024L * 1024;

  private static final Log        LOG                         = ExoLogger.getLogger(WidgetMcpTool.class);

  @Autowired
  private WidgetService           widgetService;

  @Autowired
  private ApplicationDraftService applicationDraftService;

  @Autowired
  private LayoutAclService        layoutAclService;

  @Value("${meeds.mcp.ide.widget.code.maxSize:0}")
  private long                    codeMaxBytes;

  /**
   * update_widget_code: writes the code of the widget a window renders, on
   * the user's draft of the page or the site, copying the shared widget for
   * the window on the first write of a draft session
   *
   * @param siteId the live site, for a window of a site container
   * @param pageRef the live page reference, for a window of a page
   * @param applicationId the window's storage id as the read answered it
   * @param html the HTML to write, null to keep the current one
   * @param css the CSS to write, null to keep the current one
   * @param js the JavaScript to write, null to keep the current one
   * @return the window on the draft, the widget it renders and its code
   * @throws ObjectNotFoundException when the layout, its draft or the window
   *           does not exist
   * @throws IllegalAccessException when the user is not an administrator
   *           (administrators only), or may not edit the page or the site
   *           (not allowed: page, not allowed: site)
   */
  public WidgetCodeResult updateWidgetCode(Long siteId, // NOSONAR
                                           String pageRef,
                                           String applicationId,
                                           String html,
                                           String css,
                                           String js) throws ObjectNotFoundException, IllegalAccessException {
    String username = getCurrentUserName();
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException(ADMINISTRATORS_ONLY);
    }
    if (StringUtils.isBlank(applicationId)) {
      throw new IllegalArgumentException(APPLICATION_ID_NOT_ACCEPTED);
    }
    if (html == null && css == null && js == null) {
      throw new IllegalArgumentException(HTML_NOT_ACCEPTED);
    }
    requireSize(html, HTML_NOT_ACCEPTED);
    requireSize(css, CSS_NOT_ACCEPTED);
    requireSize(js, JS_NOT_ACCEPTED);
    DraftWindows windows;
    String liveLayoutKey;
    if (pageRef != null) {
      if (siteId != null) {
        throw new IllegalArgumentException(SITE_ID_NOT_ACCEPTED);
      }
      PageKey pageKey = parsePageKey(pageRef);
      windows = applicationDraftService.resolvePage(pageKey, pageRef, List.of(applicationId), username);
      liveLayoutKey = pageKey.format();
    } else {
      if (siteId == null || siteId <= 0) {
        throw new IllegalArgumentException(SITE_ID_NOT_ACCEPTED);
      }
      windows = applicationDraftService.resolveSite(siteId, List.of(applicationId), username);
      liveLayoutKey = WidgetService.layoutKey(windows.liveSite().getSiteKey());
    }
    requireWidgetWindow(windows.contexts().get(0));
    if (windows.createDraft()) {
      ApplicationSettingsContext draftWindow = applicationDraftService.createDraft(windows, username).get(0);
      Widget widget = applicationDraftService.onCreatedDraft(() -> write(draftWindow, liveLayoutKey, html, css, js, username));
      return toResult(draftWindow, widget, true);
    }
    ApplicationSettingsContext window = windows.contexts().get(0);
    return toResult(window, write(window, liveLayoutKey, html, css, js, username), false);
  }

  private Widget write(ApplicationSettingsContext window,
                       String liveLayoutKey,
                       String html,
                       String css,
                       String js,
                       String username) throws ObjectNotFoundException, IllegalAccessException {
    try {
      return widgetService.updateWidgetCodeOnApplication(window, liveLayoutKey, html, css, js, username);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(writeRefusal(e, window));
    } catch (ObjectNotFoundException e) {
      LOG.debug("Window {} or its widget disappeared while {} wrote its code", window.getApplicationId(), username, e);
      throw new ObjectNotFoundException(APPLICATION_NOT_FOUND);
    } catch (IllegalStateException e) {
      if (StringUtils.equals(e.getMessage(), WidgetService.LAYOUT_BUSY)) {
        throw e;
      }
      LOG.debug("Widget code of window {} could not be written by {}", window.getApplicationId(), username, e);
      throw new IllegalStateException(WIDGET_CODE_NOT_SAVED);
    } catch (RuntimeException e) {
      LOG.debug("Widget code of window {} could not be written by {}", window.getApplicationId(), username, e);
      throw new IllegalStateException(WIDGET_CODE_NOT_SAVED);
    }
  }

  private String writeRefusal(IllegalAccessException refusal, ApplicationSettingsContext window) {
    if (StringUtils.equals(refusal.getMessage(), WidgetService.NOT_ADMINISTRATOR_USER)) {
      return ADMINISTRATORS_ONLY;
    }
    return window.isSiteContainer() ? ApplicationDraftService.SITE_NOT_ALLOWED : ApplicationDraftService.PAGE_NOT_ALLOWED;
  }

  private WidgetCodeResult toResult(ApplicationSettingsContext window, Widget widget, boolean draftCreated) {
    return new WidgetCodeResult(String.valueOf(window.getApplicationId()),
                                widget.getId(),
                                widget.getHtml(),
                                widget.getCss(),
                                widget.getJs(),
                                draftCreated);
  }

  private void requireWidgetWindow(ApplicationSettingsContext window) {
    if (!StringUtils.endsWith(window.getContentId(), WIDGET_PORTLET_SUFFIX)) {
      throw new IllegalArgumentException(APPLICATION_ID_NOT_ACCEPTED);
    }
  }

  private void requireSize(String code, String refusal) {
    if (code != null && code.getBytes(StandardCharsets.UTF_8).length > maxBytes()) {
      throw new IllegalArgumentException(refusal);
    }
  }

  private long maxBytes() {
    return codeMaxBytes > 0 ? codeMaxBytes : DEFAULT_MAX_BYTES;
  }

  private PageKey parsePageKey(String pageRef) {
    try {
      PageKey pageKey = PageKey.parse(pageRef);
      if (pageKey == null || StringUtils.isBlank(pageKey.getName())) {
        throw new IllegalArgumentException(PAGE_REF_NOT_ACCEPTED);
      }
      return pageKey;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(PAGE_REF_NOT_ACCEPTED);
    }
  }

}
