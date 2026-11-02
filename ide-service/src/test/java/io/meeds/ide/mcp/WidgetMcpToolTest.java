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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.ide.mcp.model.WidgetCodeResult;
import io.meeds.ide.model.Widget;
import io.meeds.ide.service.WidgetService;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.model.DraftWindows;
import io.meeds.layout.model.LayoutWrite;
import io.meeds.layout.service.ApplicationDraftService;
import io.meeds.layout.service.LayoutAclService;

import lombok.SneakyThrows;

@SpringBootTest(classes = { WidgetMcpTool.class })
@ActiveProfiles("mcp-server")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WidgetMcpToolTest {

  private static final String             USER      = "john";

  private static final String             PAGE_REF  = "portal::test::home";

  private static final PageKey            PAGE_KEY  = PageKey.parse(PAGE_REF);

  private static final PageKey            DRAFT_KEY = PageKey.parse("portal::test::home_draft_john");

  private static final SiteKey            SITE_KEY  = SiteKey.portal("test");

  private static final String             WIDGET    = "ide/WidgetPortlet";

  @MockitoBean
  private WidgetService                   widgetService;

  @MockitoBean
  private ApplicationDraftService         draftService;

  @MockitoBean
  private LayoutAclService                aclService;

  @Autowired
  private WidgetMcpTool                   tool;

  private MockedStatic<ConversationState> conversation;

  private Widget                          written;

  @BeforeEach
  @SneakyThrows
  public void setUp() {
    ConversationState state = mock(ConversationState.class);
    when(state.getIdentity()).thenReturn(new Identity(USER));
    conversation = mockStatic(ConversationState.class);
    conversation.when(ConversationState::getCurrent).thenReturn(state);
    when(aclService.isAdministrator(USER)).thenReturn(true);
    when(draftService.onCreatedDraft(any())).thenAnswer(i -> i.<LayoutWrite<Object>> getArgument(0).run());
    written = new Widget();
    written.setId(31L);
    written.setHtml("<b/>");
    written.setCss("b{}");
    written.setJs("x()");
    when(widgetService.updateWidgetCodeOnApplication(any(), anyString(), any(), any(), any(), eq(USER))).thenReturn(written);
  }

  @AfterEach
  public void tearDown() {
    conversation.close();
  }

  @Test
  public void aNonAdministratorIsRefusedBeforeAnyRead() {
    when(aclService.isAdministrator(USER)).thenReturn(false);
    assertEquals(WidgetMcpTool.ADMINISTRATORS_ONLY,
                 assertThrows(IllegalAccessException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", "<b/>", null, null)).getMessage());
    verifyNoInteractions(draftService, widgetService);
  }

  @Test
  public void theArgumentsAreCheckedBeforeAnyRead() {
    assertEquals(WidgetMcpTool.APPLICATION_ID_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> tool.updateWidgetCode(null, PAGE_REF, " ", "<b/>", null, null)).getMessage());
    assertEquals(WidgetMcpTool.HTML_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", null, null, null)).getMessage());
    assertEquals(WidgetMcpTool.SITE_ID_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> tool.updateWidgetCode(5L, PAGE_REF, "20", "<b/>", null, null)).getMessage());
    assertEquals(WidgetMcpTool.SITE_ID_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> tool.updateWidgetCode(null, null, "20", "<b/>", null, null)).getMessage());
    assertEquals(WidgetMcpTool.SITE_ID_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> tool.updateWidgetCode(0L, null, "20", "<b/>", null, null)).getMessage());
    assertEquals(WidgetMcpTool.PAGE_REF_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> tool.updateWidgetCode(null, "home", "20", "<b/>", null, null)).getMessage());
    String tooLong = "x".repeat((int) WidgetMcpTool.DEFAULT_MAX_BYTES + 1);
    assertEquals(WidgetMcpTool.CSS_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", null, tooLong, null)).getMessage());
    assertEquals(WidgetMcpTool.JS_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", null, null, tooLong)).getMessage());
    verifyNoInteractions(draftService, widgetService);
  }

  @Test
  @SneakyThrows
  public void theCodeIsWrittenOnTheDraftTheUserAlreadyHas() {
    ApplicationSettingsContext window = context(21, WIDGET, DRAFT_KEY);
    when(draftService.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER)).thenReturn(pageWindows(DRAFT_KEY, window));

    WidgetCodeResult result = tool.updateWidgetCode(null, PAGE_REF, "20", "<b/>", null, "x()");

    verify(widgetService).updateWidgetCodeOnApplication(window, PAGE_REF, "<b/>", null, "x()", USER);
    verify(draftService, never()).createDraft(any(), any());
    assertEquals(new WidgetCodeResult("21", 31L, "<b/>", "b{}", "x()", false), result);
  }

  @Test
  @SneakyThrows
  public void theDraftIsCreatedFirstWhenTheUserHasNoneAndTheAnswerSaysSo() {
    ApplicationSettingsContext liveWindow = context(20, WIDGET, PAGE_KEY);
    ApplicationSettingsContext draftWindow = context(21, WIDGET, DRAFT_KEY);
    DraftWindows windows = pageWindows(null, liveWindow);
    when(draftService.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER)).thenReturn(windows);
    when(draftService.createDraft(windows, USER)).thenReturn(List.of(draftWindow));

    WidgetCodeResult result = tool.updateWidgetCode(null, PAGE_REF, "20", "<b/>", null, null);

    verify(widgetService).updateWidgetCodeOnApplication(draftWindow, PAGE_REF, "<b/>", null, null, USER);
    verify(widgetService, never()).updateWidgetCodeOnApplication(eq(liveWindow), any(), any(), any(), any(), any());
    assertTrue(result.draftCreated());
    assertEquals("21", result.applicationId());
    assertEquals(31L, result.widgetId());
  }

  @Test
  @SneakyThrows
  public void aSiteContainerWindowIsWrittenUnderTheLiveSiteKey() {
    ApplicationSettingsContext window = context(40, WIDGET, null);
    PortalConfig site = mock(PortalConfig.class);
    when(site.getSiteKey()).thenReturn(SITE_KEY);
    DraftWindows windows = new DraftWindows(null, null, null, site, site, null, List.of(), List.of(window));
    when(draftService.resolveSite(5L, List.of("40"), USER)).thenReturn(windows);

    WidgetCodeResult result = tool.updateWidgetCode(5L, null, "40", null, "b{}", null);

    verify(widgetService).updateWidgetCodeOnApplication(window, "portal::test", null, "b{}", null, USER);
    assertFalse(result.draftCreated());
  }

  @Test
  @SneakyThrows
  public void aWindowOfAnotherApplicationIsRefusedWithoutAWrite() {
    ApplicationSettingsContext window = context(21, "content/Links", DRAFT_KEY);
    when(draftService.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER)).thenReturn(pageWindows(DRAFT_KEY, window));

    assertEquals(WidgetMcpTool.APPLICATION_ID_NOT_ACCEPTED,
                 assertThrows(IllegalArgumentException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", "<b/>", null, null)).getMessage());
    verify(widgetService, never()).updateWidgetCodeOnApplication(any(), any(), any(), any(), any(), any());
  }

  @Test
  @SneakyThrows
  public void theResolutionRefusalsPassThroughWithTheirFixedTexts() {
    when(draftService.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER)).thenThrow(new ObjectNotFoundException(ApplicationDraftService.PAGE_NOT_FOUND));
    assertEquals(ApplicationDraftService.PAGE_NOT_FOUND,
                 assertThrows(ObjectNotFoundException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", "<b/>", null, null)).getMessage());
    doThrow(new IllegalAccessException(ApplicationDraftService.PAGE_NOT_ALLOWED)).when(draftService).resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER);
    assertEquals(ApplicationDraftService.PAGE_NOT_ALLOWED,
                 assertThrows(IllegalAccessException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", "<b/>", null, null)).getMessage());
  }

  @Test
  @SneakyThrows
  public void aServiceFailureIsAnsweredWithAFixedTextAndTheBusyLayoutWithItsOwn() {
    ApplicationSettingsContext window = context(21, WIDGET, DRAFT_KEY);
    when(draftService.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER)).thenReturn(pageWindows(DRAFT_KEY, window));

    doThrow(new IllegalAccessException(WidgetService.NOT_ADMINISTRATOR_USER)).when(widgetService)
                                                                              .updateWidgetCodeOnApplication(any(), any(), any(), any(), any(), any());
    assertEquals(WidgetMcpTool.ADMINISTRATORS_ONLY,
                 assertThrows(IllegalAccessException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", "<b/>", null, null)).getMessage());
    doThrow(new IllegalStateException(WidgetService.LAYOUT_BUSY)).when(widgetService)
                                                                 .updateWidgetCodeOnApplication(any(), any(), any(), any(), any(), any());
    assertEquals(WidgetService.LAYOUT_BUSY,
                 assertThrows(IllegalStateException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", "<b/>", null, null)).getMessage());
    doThrow(new IllegalStateException("engine text with a path")).when(widgetService)
                                                                  .updateWidgetCodeOnApplication(any(), any(), any(), any(), any(), any());
    assertEquals(WidgetMcpTool.WIDGET_CODE_NOT_SAVED,
                 assertThrows(IllegalStateException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", "<b/>", null, null)).getMessage());
    doThrow(new NullPointerException("engine")).when(widgetService).updateWidgetCodeOnApplication(any(), any(), any(), any(), any(), any());
    assertEquals(WidgetMcpTool.WIDGET_CODE_NOT_SAVED,
                 assertThrows(IllegalStateException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", "<b/>", null, null)).getMessage());
  }

  @Test
  @SneakyThrows
  public void aFailureAfterTheDraftCreationNamesTheDraft() {
    ApplicationSettingsContext liveWindow = context(20, WIDGET, PAGE_KEY);
    ApplicationSettingsContext draftWindow = context(21, WIDGET, DRAFT_KEY);
    DraftWindows windows = pageWindows(null, liveWindow);
    when(draftService.resolvePage(PAGE_KEY, PAGE_REF, List.of("20"), USER)).thenReturn(windows);
    when(draftService.createDraft(windows, USER)).thenReturn(List.of(draftWindow));
    doThrow(new IllegalStateException(String.format(ApplicationDraftService.DRAFT_CREATED_BEFORE_FAILURE, WidgetMcpTool.WIDGET_CODE_NOT_SAVED))).when(draftService)
                                                                                                                                                 .onCreatedDraft(any());

    assertEquals(String.format(ApplicationDraftService.DRAFT_CREATED_BEFORE_FAILURE, WidgetMcpTool.WIDGET_CODE_NOT_SAVED),
                 assertThrows(IllegalStateException.class, () -> tool.updateWidgetCode(null, PAGE_REF, "20", "<b/>", null, null)).getMessage());
  }

  private DraftWindows pageWindows(PageKey draftKey, ApplicationSettingsContext window) {
    return new DraftWindows(PAGE_KEY, 11L, draftKey, null, null, null, List.of(), List.of(window));
  }

  private ApplicationSettingsContext context(long applicationId, String contentId, PageKey pageKey) {
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    context.setApplicationId(applicationId);
    context.setContentId(contentId);
    context.setPageKey(pageKey);
    context.setSiteContainer(pageKey == null);
    context.setSiteKey(SITE_KEY);
    context.setUsername(USER);
    lenient().when(aclService.isAdministrator(USER)).thenReturn(true);
    return context;
  }

}
