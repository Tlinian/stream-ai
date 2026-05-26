/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.event;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.pojo.CdcEventType;
import com.yashandb.streamai.datasource.pojo.CdcOperationType;
import com.yashandb.streamai.datasource.pojo.ColumnValue;
import com.yashandb.streamai.datasource.pojo.LogPosition;
import com.yashandb.streamai.datasource.pojo.TableInfo;
import java.util.List;

/**
 * DML 变更事件（数据增删改）。
 *
 * @param byteSize 事件字节大小
 * @param position 日志位点
 * @param table 目标表信息
 * @param operation DML 操作类型（INSERT/UPDATE/DELETE）
 * @param beforeColumns 变更前的列值列表（DELETE 和 UPDATE 时有值，INSERT 时为空集合）
 * @param afterColumns 变更后的列值列表（INSERT 和 UPDATE 时有值，DELETE 时为空集合）
 * @param changeAt 数据变更时间戳（毫秒）
 * @param processAt 事件处理时间戳（毫秒）
 */
public record CdcDmlEvent(
    int byteSize,
    LogPosition position,
    TableInfo table,
    CdcOperationType operation,
    List<ColumnValue> beforeColumns,
    List<ColumnValue> afterColumns,
    long changeAt,
    long processAt)
    implements CdcEvent {

  @Override
  public CdcEventType type() {
    return CdcEventType.DML;
  }
}
