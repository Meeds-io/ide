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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.ide.service.WidgetService;
import io.meeds.layout.service.PageLayoutService;
import io.meeds.layout.service.SiteLayoutService;

@SpringBootTest(classes = { WidgetPublicationListener.class })
@ExtendWith(MockitoExtension.class)
public class WidgetPublicationListenerTest {

  private static final String       PAGE_REF = "portal::test::home";

  @MockitoBean
  private ListenerService           listenerService;

  @MockitoBean
  private LayoutService             layoutService;

  @MockitoBean
  private WidgetService             widgetService;

  @Autowired
  private WidgetPublicationListener listener;

  @Test
  public void theListenerIsRegisteredOnTheTwoPublicationEventsOnly() {
    listener.init();
    verify(listenerService).addListener(PageLayoutService.PAGE_PUBLISHED_EVENT, listener);
    verify(listenerService).addListener(SiteLayoutService.SITE_PUBLISHED_EVENT, listener);
    verify(listenerService, never()).addListener(eq(PageLayoutService.PAGE_UPDATED_EVENT), any());
    verify(listenerService, never()).addListener(eq(PageLayoutService.PAGE_PERMISSIONS_UPDATED_EVENT), any());
    verify(listenerService, never()).addListener(eq(LayoutService.PORTAL_CONFIG_UPDATED), any());
    verify(listenerService, never()).addListener(eq(LayoutService.PAGE_UPDATED), any());
  }

  @Test
  public void aPublishedPageMarksItsCopiesPublishedFromTheLayoutReadBack() {
    Page page = mock(Page.class);
    when(layoutService.getPage(PageKey.parse(PAGE_REF))).thenReturn(page);

    listener.onEvent(new Event<>(PageLayoutService.PAGE_PUBLISHED_EVENT, "john", PAGE_REF));

    verify(widgetService).publishDraftCopies(PAGE_REF, page, "john");
  }

  @Test
  public void aPublishedSiteMarksItsCopiesPublishedUnderTheSiteKey() {
    SiteKey siteKey = SiteKey.portal("test");
    PortalConfig site = mock(PortalConfig.class);
    org.exoplatform.portal.config.serialize.model.SiteLayout layout = mock(org.exoplatform.portal.config.serialize.model.SiteLayout.class);
    when(site.getPortalLayout()).thenReturn(layout);
    when(layoutService.getPortalConfig(siteKey)).thenReturn(site);

    listener.onEvent(new Event<>(SiteLayoutService.SITE_PUBLISHED_EVENT, "john", siteKey));

    verify(widgetService).publishDraftCopies("portal::test", layout, "john");
  }

  @Test
  public void aFailureToMarkTheCopiesNeverAbortsThePublication() {
    when(layoutService.getPage(PageKey.parse(PAGE_REF))).thenReturn(mock(Page.class));
    doThrow(new IllegalStateException("engine")).when(widgetService).publishDraftCopies(eq(PAGE_REF), any(), any());

    assertDoesNotThrow(() -> listener.onEvent(new Event<>(PageLayoutService.PAGE_PUBLISHED_EVENT, "john", PAGE_REF)));
    assertDoesNotThrow(() -> listener.onEvent(new Event<>(PageLayoutService.PAGE_PUBLISHED_EVENT, "john", 42L)));
    verify(widgetService, never()).publishDraftCopies(eq("42"), any(), any());
  }

}
