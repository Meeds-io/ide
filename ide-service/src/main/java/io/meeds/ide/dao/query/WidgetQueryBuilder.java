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
package io.meeds.ide.dao.query;

import io.meeds.ide.entity.WidgetEntity;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;
import java.util.Objects;

public class WidgetQueryBuilder {

  private WidgetQueryBuilder() {
    // Static Utils methods
  }

  public static Specification<WidgetEntity> hasProperties(Map<String, String> properties) {
    return (Root<WidgetEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
      Predicate finalPredicate = cb.conjunction();

      for (Map.Entry<String, String> entry : properties.entrySet()) {
        Subquery<Long> subquery = Objects.requireNonNull(query).subquery(Long.class);
        Root<WidgetEntity> subRoot = subquery.from(WidgetEntity.class);
        MapJoin<WidgetEntity, String, String> subPropertiesJoin = subRoot.joinMap("properties");

        subquery.select(cb.literal(1L))
                .where(cb.equal(subRoot.get("id"), root.get("id")),
                       cb.equal(subPropertiesJoin.key(), entry.getKey()),
                       cb.equal(subPropertiesJoin.value(), entry.getValue()));

        finalPredicate = cb.and(finalPredicate, cb.exists(subquery));
      }

      return finalPredicate;
    };
  }
}
