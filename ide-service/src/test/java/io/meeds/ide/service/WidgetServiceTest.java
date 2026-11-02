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
package io.meeds.ide.service;

import static io.meeds.ide.service.WidgetService.IDE_WIDGET_CREATED_EVENT;
import static io.meeds.ide.service.WidgetService.IDE_WIDGET_UPDATED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.ide.model.Widget;
import io.meeds.ide.storage.WidgetStorage;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.layout.model.ApplicationCode;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.model.LayoutWrite;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.mockito.InOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest(classes = {
                            WidgetService.class,
})
@ExtendWith(MockitoExtension.class)
public class WidgetServiceTest {

  private static final String USERNAME     = "testUser";

  private static final String HTML_CONTENT = "...html...";

  private static final String CSS_CONTENT  = "...css...";

  private static final String JS_CONTENT   = "...js...";

  @MockitoBean
  private LayoutAclService    layoutAclService;

  @MockitoBean
  private IdentityManager     identityManager;

  @MockitoBean
  private ListenerService     listenerService;

  @MockitoBean
  private WidgetStorage       widgetStorage;

  @MockitoBean
  private LayoutService       layoutService;

  @MockitoBean
  private ApplicationSettingsService applicationSettingsService;

  @Autowired
  private WidgetService       widgetService;

  @Mock
  private Widget              widget;

  @Mock
  private Identity            identity;

  @Test
  public void getWidget() throws ObjectNotFoundException {
    assertThrows(ObjectNotFoundException.class, () -> widgetService.getWidget(2l));
    when(widgetStorage.getWidget(2l)).thenReturn(widget);
    assertEquals(widget, widgetService.getWidget(2l));
  }

  @Test
  public void createWidget() throws ObjectAlreadyExistsException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> widgetService.createWidget(widget, USERNAME));
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    when(widget.getPortletId()).thenReturn(3l);

    when(widgetStorage.existsByPortletInstanceId(3l)).thenReturn(true);
    assertThrows(ObjectAlreadyExistsException.class, () -> widgetService.createWidget(widget, USERNAME));

    when(widgetStorage.existsByPortletInstanceId(3l)).thenReturn(false);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("5");
    when(widgetStorage.createWidget(widget)).thenReturn(widget);

    Widget savedWidget = widgetService.createWidget(widget, USERNAME);
    assertNotNull(savedWidget);
    verify(widget).setCreatorId(5l);
    verify(widget).setModifierId(5l);
    verify(widget).setCreatedDate(notNull());
    verify(widget).setModifiedDate(notNull());
    verify(widgetStorage).createWidget(widget);
    verify(listenerService).broadcast(IDE_WIDGET_CREATED_EVENT, null, widget);
  }

  @Test
  public void updateWidget() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> widgetService.updateWidget(widget, USERNAME));
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);


    when(widget.getId()).thenReturn(2l);
    when(widget.getHtml()).thenReturn(HTML_CONTENT);
    when(widget.getCss()).thenReturn(CSS_CONTENT);
    when(widget.getJs()).thenReturn(JS_CONTENT);
    when(widgetStorage.getWidget(2l)).thenReturn(null);

    assertThrows(ObjectNotFoundException.class, () -> widgetService.updateWidget(widget, USERNAME));

    when(widgetStorage.getWidget(2l)).thenReturn(widget);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("5");
    when(widgetStorage.updateWidget(widget)).thenReturn(widget);

    Widget savedWidget = widgetService.updateWidget(widget, USERNAME);
    assertNotNull(savedWidget);
    verify(widget).setHtml(HTML_CONTENT);
    verify(widget).setJs(JS_CONTENT);
    verify(widget).setCss(CSS_CONTENT);
    verify(widget).setModifierId(5l);
    verify(widget).setModifiedDate(notNull());
    verify(widgetStorage).updateWidget(widget);
    verify(listenerService).broadcast(IDE_WIDGET_UPDATED_EVENT, null, widget);
  }

  @Test
  void deleteWidget() throws ObjectNotFoundException, IllegalAccessException {
    assertThrows(IllegalAccessException.class, () -> widgetService.deleteWidget(6L, USERNAME));

    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);

    when(widgetStorage.getWidget(6L)).thenReturn(widget);
    widgetService.deleteWidget(6L, USERNAME);

    verify(widgetStorage, times(1)).deleteWidgetById(6L);
  }

  @Test
  public void anUnpublishedCopyIsReadByTheEditorsOfItsLayoutOnly() throws Exception {
    Widget copy = copy(7L, "portal::test::home", "21");
    when(widgetStorage.getWidget(7L)).thenReturn(copy);
    Widget published = new Widget();
    published.setId(8L);
    when(widgetStorage.getWidget(8L)).thenReturn(published);

    assertSame(published, widgetService.getWidget(8L, null));
    assertSame(published, widgetService.getWidget(8L, USERNAME));
    assertFalse(widgetService.isDraftCopy(published));
    assertTrue(widgetService.isDraftCopy(copy));
    assertThrows(IllegalAccessException.class, () -> widgetService.getWidget(7L, null));
    assertThrows(IllegalAccessException.class, () -> widgetService.getWidget(7L, USERNAME));
    when(layoutAclService.canEditPage(PageKey.parse("portal::test::home"), USERNAME)).thenReturn(true);
    assertSame(copy, widgetService.getWidget(7L, USERNAME));

    Widget siteCopy = copy(9L, "portal::test", "40");
    when(widgetStorage.getWidget(9L)).thenReturn(siteCopy);
    assertThrows(IllegalAccessException.class, () -> widgetService.getWidget(9L, USERNAME));
    when(layoutAclService.canEditSite(new SiteKey("portal", "test"), USERNAME)).thenReturn(true);
    assertSame(siteCopy, widgetService.getWidget(9L, USERNAME));
    assertThrows(ObjectNotFoundException.class, () -> widgetService.getWidget(99L, USERNAME));
  }

  @Test
  public void theCodeOfAWindowIsReadFromItsWidgetElseItsDataInitElseTheInstanceWidget() throws Exception {
    Widget stored = new Widget();
    stored.setHtml(HTML_CONTENT);
    when(widgetStorage.getWidget(5L)).thenReturn(stored);
    assertEquals(new ApplicationCode(HTML_CONTENT, null, null),
                 widgetService.readWidgetCodeOnApplication(context(20, Map.of("widgetId", "5"))));
    assertEquals(new ApplicationCode("<i/>", "i{}", "y()"),
                 widgetService.readWidgetCodeOnApplication(context(20, Map.of("data.init", "{\"html\":\"<i/>\",\"css\":\"i{}\",\"js\":\"y()\"}"))));
    Widget instance = new Widget();
    instance.setJs(JS_CONTENT);
    when(widgetStorage.getWidgetByPortletId(3L)).thenReturn(instance);
    assertEquals(new ApplicationCode(null, null, JS_CONTENT),
                 widgetService.readWidgetCodeOnApplication(context(20, Map.of("portletInstanceId", "3"))));
    assertNull(widgetService.readWidgetCodeOnApplication(context(20, Map.of())));
    assertThrows(ObjectNotFoundException.class, () -> widgetService.readWidgetCodeOnApplication(context(20, Map.of("widgetId", "66"))));
  }

  @Test
  public void aSharedWidgetIsCopiedForTheDraftWindowAndTheWindowPointedAtTheCopyInOneWrite() throws Exception {
    ApplicationSettingsContext window = lockedWindow(21, Map.of("widgetId", "5", "data.init", "{}"));
    Widget shared = new Widget();
    shared.setId(5L);
    shared.setHtml(HTML_CONTENT);
    shared.setCss(CSS_CONTENT);
    shared.setJs(JS_CONTENT);
    when(widgetStorage.getWidget(5L)).thenReturn(shared);
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("1");
    when(widgetStorage.createWidget(any())).thenAnswer(i -> {
      Widget created = i.getArgument(0);
      created.setId(31L);
      return created;
    });

    Widget result = widgetService.updateWidgetCodeOnApplication(window, "portal::test::home", "<b/>", null, null, USERNAME);

    assertEquals(31L, result.getId());
    assertEquals("<b/>", result.getHtml());
    assertEquals(CSS_CONTENT, result.getCss());
    assertEquals(JS_CONTENT, result.getJs());
    assertNull(result.getPortletId());
    assertEquals(Map.of("draftCopy", "true", "ownerWindow", "21", "ownerLayout", "portal::test::home"), result.getProperties());
    Map<String, String> expected = new HashMap<>();
    expected.put("widgetId", "31");
    expected.put("data.init", null);
    verify(applicationSettingsService).updatePreferences(window, expected);
    verify(widgetStorage, never()).updateWidget(any());
    verify(widgetStorage, never()).deleteWidgetById(anyLong());
    verify(applicationSettingsService).runLocked(eq("portal::test::home_draft_testUser"), eq(21L), any());
  }

  @Test
  public void aCopyTheWindowOwnsIsWrittenInPlaceKeepingTheOmittedPartsAndTheMarkers() throws Exception {
    ApplicationSettingsContext window = lockedWindow(21, Map.of("widgetId", "7"));
    Widget owned = copy(7L, "portal::test::home", "21");
    owned.setHtml(HTML_CONTENT);
    owned.setCss(CSS_CONTENT);
    when(widgetStorage.getWidget(7L)).thenReturn(owned);
    when(widgetStorage.updateWidget(any())).thenAnswer(i -> i.getArgument(0));
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("1");

    Widget result = widgetService.updateWidgetCodeOnApplication(window, "portal::test::home", null, null, "z()", USERNAME);

    assertSame(owned, result);
    assertEquals(HTML_CONTENT, result.getHtml());
    assertEquals(CSS_CONTENT, result.getCss());
    assertEquals("z()", result.getJs());
    assertEquals("true", result.getProperties().get("draftCopy"));
    verify(widgetStorage, never()).createWidget(any());
    verify(applicationSettingsService, never()).updatePreferences(any(), any());

    Widget otherWindows = copy(7L, "portal::test::home", "99");
    when(widgetStorage.getWidget(7L)).thenReturn(otherWindows);
    when(widgetStorage.createWidget(any())).thenAnswer(i -> i.getArgument(0));
    widgetService.updateWidgetCodeOnApplication(window, "portal::test::home", null, null, "z()", USERNAME);
    verify(widgetStorage).createWidget(any());
  }

  @Test
  public void aNeverRenderedWindowCopiesItsDataInitAndClearsIt() throws Exception {
    ApplicationSettingsContext window = lockedWindow(21, Map.of("data.init", "{\"html\":\"<i/>\",\"css\":\"i{}\",\"js\":\"y()\"}", "portletInstanceId", "3"));
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("1");
    when(widgetStorage.createWidget(any())).thenAnswer(i -> {
      Widget created = i.getArgument(0);
      created.setId(32L);
      return created;
    });

    Widget result = widgetService.updateWidgetCodeOnApplication(window, "portal::test::home", null, "b{}", null, USERNAME);

    assertEquals("<i/>", result.getHtml());
    assertEquals("b{}", result.getCss());
    assertEquals("y()", result.getJs());
    verify(widgetStorage, never()).getWidgetByPortletId(anyLong());
    Map<String, String> expected = new HashMap<>();
    expected.put("widgetId", "32");
    expected.put("data.init", null);
    verify(applicationSettingsService).updatePreferences(window, expected);
  }

  @Test
  public void aFailedPreferenceWriteDeletesTheCopyItCreatedAndAnswersTheFailure() throws Exception {
    ApplicationSettingsContext window = lockedWindow(21, Map.of("widgetId", "5"));
    Widget shared = new Widget();
    shared.setId(5L);
    when(widgetStorage.getWidget(5L)).thenReturn(shared);
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("1");
    when(widgetStorage.createWidget(any())).thenAnswer(i -> {
      Widget created = i.getArgument(0);
      created.setId(31L);
      return created;
    });
    doThrow(new IllegalStateException("layout.pageOutdatedError")).when(applicationSettingsService).updatePreferences(any(), any());

    assertEquals("layout.pageOutdatedError",
                 assertThrows(IllegalStateException.class,
                              () -> widgetService.updateWidgetCodeOnApplication(window, "portal::test::home", "<b/>", null, null, USERNAME)).getMessage());

    InOrder order = inOrder(widgetStorage, applicationSettingsService);
    order.verify(widgetStorage).createWidget(any());
    order.verify(applicationSettingsService).updatePreferences(any(), any());
    order.verify(widgetStorage).deleteWidgetById(31L);
  }

  @Test
  public void aNonAdministratorIsRefusedBeforeTheLock() throws Exception {
    ApplicationSettingsContext window = lockedWindow(21, Map.of("widgetId", "5"));
    assertThrows(IllegalAccessException.class,
                 () -> widgetService.updateWidgetCodeOnApplication(window, "portal::test::home", "<b/>", null, null, USERNAME));
    verify(applicationSettingsService, never()).runLocked(any(), anyLong(), any());
  }

  @Test
  public void aPublicationClearsTheMarkerOfTheCopiesThePublishedLayoutReferencesOnly() throws Exception {
    Widget referenced = copy(7L, "portal::test::home", "21");
    Widget unreferenced = copy(8L, "portal::test::home", "22");
    Map<String, String> marked = new HashMap<>();
    marked.put("draftCopy", "true");
    marked.put("ownerLayout", "portal::test::home");
    when(widgetStorage.getWidgetsByProperties(marked)).thenReturn(List.of(referenced, unreferenced));
    Container page = mock(Container.class);
    Application window = mock(Application.class);
    ApplicationState state = mock(ApplicationState.class);
    when(window.getState()).thenReturn(state);
    Portlet preferences = new Portlet();
    preferences.setValue("widgetId", "7");
    when(layoutService.load(state)).thenReturn(preferences);
    when(page.getChildren()).thenReturn(new ArrayList<>(List.of(window)));

    widgetService.publishDraftCopies("portal::test::home", page);

    verify(widgetStorage).updateWidget(argThat(w -> w.getId() == 7L && !w.getProperties().containsKey("draftCopy")
                                                    && "21".equals(w.getProperties().get("ownerWindow"))));
    verify(widgetStorage, never()).updateWidget(argThat(w -> w.getId() == 8L));
    verify(layoutAclService, never()).isAdministrator(any());

    when(widgetStorage.getWidgetsByProperties(marked)).thenReturn(List.of());
    widgetService.publishDraftCopies("portal::test::home", page);
    verify(widgetStorage, times(1)).updateWidget(any());
  }

  @Test
  public void anUnpublishedCopyForgedOnAnotherWindowReadsAsNoCodeAndIsNeverCopiedFrom() throws Exception {
    Widget foreign = copy(7L, "portal::other::page", "99");
    foreign.setHtml(HTML_CONTENT);
    when(widgetStorage.getWidget(7L)).thenReturn(foreign);

    assertNull(widgetService.readWidgetCodeOnApplication(context(20, Map.of("widgetId", "7"))));
    when(layoutAclService.canEditPage(PageKey.parse("portal::other::page"), USERNAME)).thenReturn(true);
    assertEquals(new ApplicationCode(HTML_CONTENT, null, null), widgetService.readWidgetCodeOnApplication(context(20, Map.of("widgetId", "7"))));
    when(layoutAclService.canEditPage(PageKey.parse("portal::other::page"), USERNAME)).thenReturn(false);

    ApplicationSettingsContext window = lockedWindow(21, Map.of("widgetId", "7"));
    when(layoutAclService.isAdministrator(USERNAME)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(identity);
    when(identity.getId()).thenReturn("1");
    when(widgetStorage.createWidget(any())).thenAnswer(i -> i.getArgument(0));

    Widget result = widgetService.updateWidgetCodeOnApplication(window, "portal::test::home", null, "b{}", null, USERNAME);

    assertNull(result.getHtml());
    assertEquals("b{}", result.getCss());
    verify(widgetStorage, never()).updateWidget(any());
  }

  private Widget copy(long id, String ownerLayout, String ownerWindow) {
    Widget copy = new Widget();
    copy.setId(id);
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put("draftCopy", "true");
    properties.put("ownerWindow", ownerWindow);
    properties.put("ownerLayout", ownerLayout);
    copy.setProperties(properties);
    return copy;
  }

  private ApplicationSettingsContext context(long applicationId, Map<String, String> preferences) {
    ApplicationSettingsContext context = new ApplicationSettingsContext();
    context.setApplicationId(applicationId);
    context.setContentId("ide/WidgetPortlet");
    context.setPreferences(new LinkedHashMap<>(preferences));
    context.setPageKey(PageKey.parse("portal::test::home_draft_testUser"));
    context.setSiteKey(SiteKey.portal("test"));
    context.setUsername(USERNAME);
    return context;
  }

  private ApplicationSettingsContext lockedWindow(long applicationId, Map<String, String> preferences) throws Exception {
    ApplicationSettingsContext context = context(applicationId, preferences);
    Portlet stored = new Portlet();
    preferences.forEach(stored::setValue);
    when(layoutService.load(any(PersistentApplicationState.class))).thenReturn(stored);
    when(applicationSettingsService.runLocked(any(), anyLong(), any())).thenAnswer(i -> i.<LayoutWrite<Object>> getArgument(2).run());
    return context;
  }

}
