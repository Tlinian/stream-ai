/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.pojo;

/** CDC 事件类型枚举。 */
public enum CdcEventType {
  /** 事务开始。 */
  BEGIN,
  /** 事务提交。 */
  COMMIT,
  /** DDL 变更（表结构修改）。 */
  DDL,
  /** DML 变更（数据增删改）。 */
  DML,
  /** LOB 大字段分片数据。 */
  CHUNK
}
