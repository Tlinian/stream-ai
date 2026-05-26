/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.pojo;

/**
 * 表标识信息。
 *
 * @param schema 模式名，可为 null
 * @param name 表名
 */
public record TableInfo(String schema, String name) {

  /** 仅指定表名的便捷构造方法。 */
  public TableInfo(final String name) {
    this(null, name);
  }

  @Override
  public String toString() {
    return schema == null ? name : schema + "." + name;
  }
}
