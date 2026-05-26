/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.event;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.pojo.CdcEventType;
import com.yashandb.streamai.datasource.pojo.LogPosition;
import com.yashandb.streamai.datasource.pojo.TransactionInfo;

/**
 * 事务开始事件。
 *
 * @param byteSize 事件字节大小
 * @param position 日志位点
 * @param transactionInfo 事务信息
 */
public record CdcBeginEvent(int byteSize, LogPosition position, TransactionInfo transactionInfo)
    implements CdcEvent {

  @Override
  public CdcEventType type() {
    return CdcEventType.BEGIN;
  }
}
