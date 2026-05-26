/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource;

import com.yashandb.streamai.datasource.pojo.CdcEventType;
import com.yashandb.streamai.datasource.pojo.LogPosition;

/**
 * CDC 事件基础接口。
 *
 * <p>所有 CDC 事件均携带事件类型、日志位点和事件大小信息。
 */
public interface CdcEvent {

  /** 获取事件类型。 */
  CdcEventType type();

  /** 获取日志位点。 */
  LogPosition position();

  /** 获取事件字节大小。 */
  int byteSize();
}
