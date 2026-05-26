/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.event;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.pojo.CdcEventType;
import com.yashandb.streamai.datasource.pojo.LogPosition;
import com.yashandb.streamai.datasource.pojo.TableInfo;

/**
 * DDL 变更事件（表结构修改，如 CREATE TABLE、ALTER TABLE 等）。
 *
 * @param byteSize 事件字节大小
 * @param position 日志位点
 * @param table 目标表信息
 * @param ddl DDL 语句原文
 * @param changeAt 数据变更时间戳（毫秒）
 * @param processAt 事件处理时间戳（毫秒）
 */
public record CdcDdlEvent(
    int byteSize, LogPosition position, TableInfo table, String ddl, long changeAt, long processAt)
    implements CdcEvent {

  @Override
  public CdcEventType type() {
    return CdcEventType.DDL;
  }
}
