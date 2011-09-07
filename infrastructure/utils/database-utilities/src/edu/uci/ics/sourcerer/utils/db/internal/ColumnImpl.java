/* 
 * Sourcerer: an infrastructure for large-scale source code analysis.
 * Copyright (C) by contributors. See CONTRIBUTORS.txt for full list.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.uci.ics.sourcerer.utils.db.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import edu.uci.ics.sourcerer.utils.db.internal.ConstantConditionImpl.Type;
import edu.uci.ics.sourcerer.utils.db.sql.BindVariable;
import edu.uci.ics.sourcerer.utils.db.sql.Column;
import edu.uci.ics.sourcerer.utils.db.sql.ComparisonCondition;
import edu.uci.ics.sourcerer.utils.db.sql.ConstantCondition;
import edu.uci.ics.sourcerer.utils.db.sql.InConstantCondition;
import edu.uci.ics.sourcerer.utils.db.sql.QualifiedColumn;
import edu.uci.ics.sourcerer.utils.db.sql.QualifiedTable;
import edu.uci.ics.sourcerer.utils.db.sql.Selectable;

/**
 * @author Joel Ossher (jossher@uci.edu)
 */
abstract class ColumnImpl<T> implements Column<T> {
  private DatabaseTableImpl table;
  private String name;
  private String type;
  private boolean nullable;
  private int sqlType;
  private boolean indexed;
  
  ColumnImpl(DatabaseTableImpl table, String name, String type, boolean nullable, int sqlType) {
    this.table = table;
    this.name = name;
    this.type = type;
    this.nullable = nullable;
    this.sqlType = sqlType;
  }
  
  @Override
  public final String getName() {
    return name;
  }

  @Override
  public final String getType() {
    return type;
  }

  @Override
  public final Column<T> addIndex() {
    indexed = true;
    return this;
  }
  
  @Override
  public final boolean isIndexed() {
    return indexed;
  }

  @Override
  public String getIndex() {
    if (indexed) {
      return "INDEX(" + name + ")";
    } else {
      throw new IllegalArgumentException(name + " is not indexed");
    }
  }

  @Override
  public final BindVariable bind(T value) {
    if (value == null) {
      if (nullable) {
        return new BindVariable() {
          @Override
          public void bind(PreparedStatement statement, int index) throws SQLException {
            statement.setNull(index, sqlType);
          }
        };
      } else {
        throw new IllegalArgumentException(name + " does not accept null values.");
      }
    } else {
      return bindHelper(value);
    }
  }
  
  protected abstract BindVariable bindHelper(T value);
  
  @Override
  public final T from(String value) {
    if (value == null) {
      return null;
    } else {
      return fromHelper(value);
    }
  }
  
  protected abstract T fromHelper(String value);
  
  @Override
  public final String to(T value) {
    if (value == null) {
      if (nullable) {
        return null;
      } else {
        throw new IllegalArgumentException(name + " is not nullable");
      }
    } else {
      return toHelper(value);
    }
  }
  
  protected abstract String toHelper(T value);
  @Override
  public final DatabaseTableImpl getTable() {
    return table;
  }
  
  @Override
  public QualifiedColumn<T> qualify(QualifiedTable table) {
    if (table.getQualifiedTable() == table) {
      return new QualifiedColumnImpl<T>(this, table);
    } else {
      throw new IllegalArgumentException("This column is not for table " + table);
    }
  }
  
  @Override
  public ComparisonCondition compareEquals(Selectable<T> other) {
    return new ComparisonConditionImpl(this, other);
  }
  
  @Override 
  public ConstantCondition<T> compareEquals() {
    return new ConstantConditionImpl<>(this, Type.EQUALS);
  }
  
  @Override 
  public ConstantCondition<T> compareNotEquals() {
    return new ConstantConditionImpl<>(this, Type.NOT_EQUALS);
  }
  
  @Override
  public InConstantCondition<T> compareIn(Collection<T> values) {
    return new InConstantConditionImpl<>(this, InConstantConditionImpl.Type.IN, values);
  }
  
  @Override
  public String toSql() {
    return table.getName() + "." + name;
  }
  
  @Override
  public String toString() {
    return toSql();
  }
}
