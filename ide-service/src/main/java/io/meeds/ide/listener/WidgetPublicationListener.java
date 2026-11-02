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
package io.meeds.ide.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.ide.service.WidgetService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.SiteLayoutService;

import jakarta.annotation.PostConstruct;

/**
 * Glue on the two publication events of the layout, and on those two alone:
 * a publication is the one write that makes a draft's widget copies live,
 * so it is the one event that clears their unpublished marker. Synchronous
 * inside the publishing call; a failure is logged and never aborts the
 * publication, whose layout is saved already: the copy then stays marked,
 * served to the layout's editors only.
 */
@Component
public class WidgetPublicationListener extends Listener<String, Object> {

  private static final Log LOG = ExoLogger.getLogger(WidgetPublicationListener.class);

  @Autowired
  private ListenerService  listenerService;

  @Autowired
  private LayoutService    layoutService;

  @Autowired
  private WidgetService    widgetService;

  @PostConstruct
  public void init() {
    listenerService.addListener(PageLayoutService.PAGE_PUBLISHED_EVENT, this);
    listenerService.addListener(SiteLayoutService.SITE_PUBLISHED_EVENT, this);
  }

  @Override
  public void onEvent(Event<String, Object> event) {
    Object data = event.getData();
    try {
      if (data instanceof String pageRef) {
        Page page = layoutService.getPage(PageKey.parse(pageRef));
        widgetService.publishDraftCopies(pageRef, page);
      } else if (data instanceof SiteKey siteKey) {
        PortalConfig site = layoutService.getPortalConfig(siteKey);
        widgetService.publishDraftCopies(WidgetService.layoutKey(siteKey), site == null ? null : site.getPortalLayout());
      }
    } catch (Exception e) {
      LOG.warn("Widget copies of published layout {} could not be marked published", data, e);
    }
  }

}
