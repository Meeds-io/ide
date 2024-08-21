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
package io.meeds.ide.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import io.meeds.ide.dao.WidgetDAO;
import io.meeds.ide.entity.WidgetEntity;
import io.meeds.ide.model.Widget;

@SpringBootTest(classes = {
                            WidgetStorage.class,
})
@ExtendWith(MockitoExtension.class)
public class WidgetStorageTest {

  private static final String        HTML_CONTENT  = "...html...";

  private static final String        CSS_CONTENT   = "...css...";

  private static final String        JS_CONTENT    = "...js...";

  private static final LocalDateTime CREATED_DATE  = LocalDate.of(2024, 8, 21).atStartOfDay();

  private static final LocalDateTime MODIFIED_DATE = LocalDate.of(2024, 8, 22).atStartOfDay().minusHours(16);

  @MockBean
  private WidgetDAO                  widgetDAO;

  @Autowired
  private WidgetStorage              widgetStorage;

  @Mock
  private WidgetEntity               widgetEntity;

  @Mock
  private Widget                     widget;

  @Test
  public void getWidget() {
    when(widgetDAO.findById(2l)).thenReturn(Optional.of(widgetEntity));
    when(widgetEntity.getId()).thenReturn(2l);
    when(widgetEntity.getHtml()).thenReturn(HTML_CONTENT);
    when(widgetEntity.getCss()).thenReturn(CSS_CONTENT);
    when(widgetEntity.getJs()).thenReturn(JS_CONTENT);
    when(widgetEntity.getCreatorId()).thenReturn(6l);
    when(widgetEntity.getCreatedDate()).thenReturn(CREATED_DATE);
    when(widgetEntity.getModifierId()).thenReturn(5l);
    when(widgetEntity.getModifiedDate()).thenReturn(MODIFIED_DATE);

    Widget retrievedWidget = widgetStorage.getWidget(2l);
    assertNotNull(retrievedWidget);
    assertEquals(2l, retrievedWidget.getId());
    assertEquals(HTML_CONTENT, retrievedWidget.getHtml());
    assertEquals(CSS_CONTENT, retrievedWidget.getCss());
    assertEquals(JS_CONTENT, retrievedWidget.getJs());
    assertEquals(6l, retrievedWidget.getCreatorId());
    assertEquals(CREATED_DATE, retrievedWidget.getCreatedDate());
    assertEquals(5l, retrievedWidget.getModifierId());
    assertEquals(MODIFIED_DATE, retrievedWidget.getModifiedDate());
  }

  @Test
  public void existsByPortletInstanceId() {
    when(widgetDAO.existsByPortletId(2l)).thenReturn(true);
    assertTrue(widgetStorage.existsByPortletInstanceId(2l));
    assertFalse(widgetStorage.existsByPortletInstanceId(3l));
  }

  @Test
  public void createWidget() {
    when(widget.getHtml()).thenReturn(HTML_CONTENT);
    when(widget.getCss()).thenReturn(CSS_CONTENT);
    when(widget.getJs()).thenReturn(JS_CONTENT);
    when(widget.getCreatorId()).thenReturn(6l);
    when(widget.getCreatedDate()).thenReturn(CREATED_DATE);
    when(widget.getModifierId()).thenReturn(5l);
    when(widget.getModifiedDate()).thenReturn(MODIFIED_DATE);

    when(widgetDAO.save(any(WidgetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Widget updatedWidget = widgetStorage.createWidget(widget);
    assertNotNull(updatedWidget);
    assertEquals(HTML_CONTENT, updatedWidget.getHtml());
    assertEquals(CSS_CONTENT, updatedWidget.getCss());
    assertEquals(JS_CONTENT, updatedWidget.getJs());
    assertEquals(6l, updatedWidget.getCreatorId());
    assertEquals(CREATED_DATE, updatedWidget.getCreatedDate());
    assertEquals(5l, updatedWidget.getModifierId());
    assertEquals(MODIFIED_DATE, updatedWidget.getModifiedDate());
  }

  @Test
  public void updateWidget() {
    when(widget.getHtml()).thenReturn(HTML_CONTENT);
    when(widget.getCss()).thenReturn(CSS_CONTENT);
    when(widget.getJs()).thenReturn(JS_CONTENT);
    when(widget.getCreatorId()).thenReturn(6l);
    when(widget.getCreatedDate()).thenReturn(CREATED_DATE);
    when(widget.getModifierId()).thenReturn(5l);
    when(widget.getModifiedDate()).thenReturn(MODIFIED_DATE);

    when(widgetDAO.save(any(WidgetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Widget updatedWidget = widgetStorage.updateWidget(widget);
    assertNotNull(updatedWidget);
    assertEquals(HTML_CONTENT, updatedWidget.getHtml());
    assertEquals(CSS_CONTENT, updatedWidget.getCss());
    assertEquals(JS_CONTENT, updatedWidget.getJs());
    assertEquals(6l, updatedWidget.getCreatorId());
    assertEquals(CREATED_DATE, updatedWidget.getCreatedDate());
    assertEquals(5l, updatedWidget.getModifierId());
    assertEquals(MODIFIED_DATE, updatedWidget.getModifiedDate());
  }

}
