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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.SpaceUtils;

import io.meeds.ide.constant.WidgetType;
import io.meeds.ide.mcp.model.SiteStaticResourcesResult;
import io.meeds.ide.mcp.model.StaticResourceResult;
import io.meeds.ide.model.Widget;
import io.meeds.ide.service.StaticResourceService;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.mcp.server.plugin.McpToolPlugin;

/**
 * The MCP tools that read and write the CSS and JavaScript static resources
 * of a portal site, the ones Site Management lists: the argument checks, the
 * choice of the sites Site Management lists and the exception translation,
 * the read and the write being {@link StaticResourceService}'s. A write is
 * live at once, a site's static resources having no draft. Every refusal is
 * a fixed text; no engine message reaches the wire.
 */
@Service
@Profile("mcp-server")
public class StaticResourceMcpTool implements McpToolPlugin {

  public static final String    ADMINISTRATORS_ONLY       = "not allowed: administrators only";

  public static final String    SITE_ID_NOT_ACCEPTED      =
                                                     "site_id is not accepted: send the id of a portal site Site Management lists, as list_sites answers it; Site Management manages no static resource for a group site, a space's site or a space's public site";

  public static final String    SITE_NOT_FOUND            = "not found: site";

  public static final String    RESOURCE_NOT_FOUND        = "not found: static resource";

  public static final String    RESOURCE_ID_NOT_ACCEPTED  =
                                                         "resource_id is not accepted: send a resource_id get_site_static_resources answered for this site, or omit it to create a resource";

  public static final String    TYPE_NOT_ACCEPTED         =
                                                  "type is not accepted: send CSS or JS to create a resource; to update one, omit it or send the resource's own type, which cannot change";

  public static final String    NAME_NOT_ACCEPTED         =
                                                  "name is not accepted: send a non-blank name of at most 255 characters; it is required to create a resource";

  public static final String    CONTENT_NOT_ACCEPTED      =
                                                     "content is not accepted: send the CSS or JavaScript text, within the configured size (1 MB by default); it is required to create a resource";

  public static final String    POSITION_NOT_ACCEPTED     =
                                                      "position is not accepted: send START_OF_HEAD, END_OF_HEAD, START_OF_BODY or END_OF_BODY, or omit it";

  public static final String    NOTHING_TO_UPDATE         =
                                                  "nothing to update: send at least one of name, content, position and enabled with resource_id";

  public static final String    RESOURCE_NOT_SAVED        = "could not be saved: static resource";

  public static final String    DEFAULT_POSITION          = "END_OF_BODY";

  public static final int       NAME_MAX_LENGTH           = 255;

  public static final long      DEFAULT_MAX_BYTES         = 1024L * 1024;

  private static final List<String> POSITIONS             = List.of("START_OF_HEAD", "END_OF_HEAD", "START_OF_BODY", DEFAULT_POSITION);

  private static final Log      LOG                       = ExoLogger.getLogger(StaticResourceMcpTool.class);

  @Autowired
  private StaticResourceService staticResourceService;

  @Autowired
  private LayoutService         layoutService;

  @Autowired
  private LayoutAclService      layoutAclService;

  @Value("${meeds.mcp.ide.staticResource.maxSize:0}")
  private long                  contentMaxBytes;

  /**
   * get_site_static_resources: reads the CSS and JavaScript static resources
   * of a site
   *
   * @param siteId the site
   * @return the site's CSS and JavaScript static resources
   * @throws ObjectNotFoundException when the site does not exist
   * @throws IllegalAccessException when the user is not an administrator
   */
  public SiteStaticResourcesResult getSiteStaticResources(Long siteId) throws ObjectNotFoundException, IllegalAccessException {
    String username = requireAdministrator();
    String siteName = requireSiteName(siteId);
    List<Widget> resources;
    try {
      resources = staticResourceService.getStaticResources(siteName, username);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(ADMINISTRATORS_ONLY);
    }
    return new SiteStaticResourcesResult(siteId,
                                         resources.stream()
                                                  .filter(StaticResourceMcpTool::isCssOrJs)
                                                  .map(StaticResourceMcpTool::toResult)
                                                  .toList());
  }

  /**
   * set_site_static_resource: creates a CSS or JavaScript static resource on
   * a site, or updates the one resourceId names; the change is live at once
   *
   * @param siteId the site
   * @param resourceId the resource to update, null to create one
   * @param type CSS or JS, required to create
   * @param name the resource's name, required to create, null to keep it on
   *          an update
   * @param content the CSS or JavaScript, required to create, null to keep
   *          it on an update
   * @param position where the resource is rendered, null for the default on
   *          a creation and to keep it on an update
   * @param enabled whether the resource is rendered, null for true on a
   *          creation and to keep it on an update
   * @return the resource as saved
   * @throws ObjectNotFoundException when the site or the resource does not
   *           exist
   * @throws IllegalAccessException when the user is not an administrator
   */
  public StaticResourceResult setSiteStaticResource(Long siteId, // NOSONAR
                                                    Long resourceId,
                                                    String type,
                                                    String name,
                                                    String content,
                                                    String position,
                                                    Boolean enabled) throws ObjectNotFoundException, IllegalAccessException {
    String username = requireAdministrator();
    if (resourceId != null && resourceId <= 0) {
      throw new IllegalArgumentException(RESOURCE_ID_NOT_ACCEPTED);
    }
    WidgetType widgetType = parseType(type);
    if (name != null && (StringUtils.isBlank(name) || name.length() > NAME_MAX_LENGTH)) {
      throw new IllegalArgumentException(NAME_NOT_ACCEPTED);
    }
    if (content != null && content.getBytes(StandardCharsets.UTF_8).length > maxBytes()) {
      throw new IllegalArgumentException(CONTENT_NOT_ACCEPTED);
    }
    if (position != null && !POSITIONS.contains(position)) {
      throw new IllegalArgumentException(POSITION_NOT_ACCEPTED);
    }
    if (resourceId == null) {
      if (widgetType == null) {
        throw new IllegalArgumentException(TYPE_NOT_ACCEPTED);
      }
      if (name == null) {
        throw new IllegalArgumentException(NAME_NOT_ACCEPTED);
      }
      if (content == null) {
        throw new IllegalArgumentException(CONTENT_NOT_ACCEPTED);
      }
    } else if (name == null && content == null && position == null && enabled == null) {
      throw new IllegalArgumentException(NOTHING_TO_UPDATE);
    }
    String siteName = requireSiteName(siteId);
    if (resourceId == null) {
      return toResult(create(siteName, widgetType, name, content, position, enabled, username));
    }
    return toResult(update(resourceId, siteName, widgetType, name, content, position, enabled, username));
  }

  private Widget create(String siteName, // NOSONAR
                        WidgetType type,
                        String name,
                        String content,
                        String position,
                        Boolean enabled,
                        String username) throws IllegalAccessException {
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put(StaticResourceService.SITE_NAME, siteName);
    properties.put(StaticResourceService.RESOURCE_NAME, name);
    properties.put(StaticResourceService.POSITION, position == null ? "" : position);
    properties.put(StaticResourceService.ENABLED, String.valueOf(!Boolean.FALSE.equals(enabled)));
    Widget widget = new Widget();
    widget.setType(type);
    if (type == WidgetType.CSS) {
      widget.setCss(content);
    } else {
      widget.setJs(content);
    }
    widget.setProperties(properties);
    try {
      return staticResourceService.createStaticResource(widget, username);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(ADMINISTRATORS_ONLY);
    } catch (RuntimeException e) {
      LOG.debug("Static resource of site {} could not be created by {}", siteName, username, e);
      throw new IllegalStateException(RESOURCE_NOT_SAVED);
    }
  }

  private Widget update(long resourceId, // NOSONAR
                        String siteName,
                        WidgetType type,
                        String name,
                        String content,
                        String position,
                        Boolean enabled,
                        String username) throws IllegalAccessException, ObjectNotFoundException {
    try {
      Widget existing = staticResourceService.getStaticResource(resourceId, siteName, username);
      if (type != null && type != existing.getType()) {
        throw new IllegalArgumentException(TYPE_NOT_ACCEPTED);
      }
      return staticResourceService.updateStaticResource(resourceId, siteName, name, content, position, enabled, username);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(ADMINISTRATORS_ONLY);
    } catch (ObjectNotFoundException e) {
      throw new ObjectNotFoundException(RESOURCE_NOT_FOUND);
    } catch (IllegalArgumentException e) {
      if (StringUtils.equals(e.getMessage(), TYPE_NOT_ACCEPTED)) {
        throw e;
      }
      LOG.debug("Static resource {} of site {} could not be updated by {}", resourceId, siteName, username, e);
      throw new IllegalStateException(RESOURCE_NOT_SAVED);
    } catch (RuntimeException e) {
      LOG.debug("Static resource {} of site {} could not be updated by {}", resourceId, siteName, username, e);
      throw new IllegalStateException(RESOURCE_NOT_SAVED);
    }
  }

  private String requireAdministrator() throws IllegalAccessException {
    String username = getCurrentUserName();
    if (!layoutAclService.isAdministrator(username)) {
      throw new IllegalAccessException(ADMINISTRATORS_ONLY);
    }
    return username;
  }

  private String requireSiteName(Long siteId) throws ObjectNotFoundException {
    if (siteId == null || siteId <= 0) {
      throw new IllegalArgumentException(SITE_ID_NOT_ACCEPTED);
    }
    PortalConfig site;
    try {
      site = layoutService.getPortalConfig(siteId);
    } catch (RuntimeException e) {
      LOG.debug("Site {} could not be read", siteId, e);
      site = null;
    }
    if (site == null || StringUtils.isBlank(site.getName())) {
      throw new ObjectNotFoundException(SITE_NOT_FOUND);
    }
    if (!StringUtils.equals(site.getType(), PortalConfig.PORTAL_TYPE)
        || StringUtils.isNotEmpty(site.getProperty(SpaceUtils.IS_PUBLIC_SITE_SPACE))) {
      throw new IllegalArgumentException(SITE_ID_NOT_ACCEPTED);
    }
    return site.getName();
  }

  private WidgetType parseType(String type) {
    if (type == null) {
      return null;
    }
    if (StringUtils.equalsIgnoreCase(type, WidgetType.CSS.name())) {
      return WidgetType.CSS;
    } else if (StringUtils.equalsIgnoreCase(type, WidgetType.JS.name())) {
      return WidgetType.JS;
    }
    throw new IllegalArgumentException(TYPE_NOT_ACCEPTED);
  }

  private long maxBytes() {
    return contentMaxBytes > 0 ? contentMaxBytes : DEFAULT_MAX_BYTES;
  }

  private static boolean isCssOrJs(Widget widget) {
    return widget.getType() == WidgetType.CSS || widget.getType() == WidgetType.JS;
  }

  private static StaticResourceResult toResult(Widget widget) {
    Map<String, String> properties = widget.getProperties() == null ? Map.of() : widget.getProperties();
    String position = properties.get(StaticResourceService.POSITION);
    return new StaticResourceResult(widget.getId(),
                                    widget.getType().name(),
                                    properties.get(StaticResourceService.RESOURCE_NAME),
                                    widget.getType() == WidgetType.CSS ? widget.getCss() : widget.getJs(),
                                    StringUtils.isBlank(position) ? DEFAULT_POSITION : position,
                                    StringUtils.equals(properties.get(StaticResourceService.ENABLED), "true"));
  }

}
