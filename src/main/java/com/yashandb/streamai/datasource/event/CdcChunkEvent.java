/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.event;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.pojo.CdcEventType;
import com.yashandb.streamai.datasource.pojo.LogPosition;
import com.yashandb.streamai.datasource.pojo.TableInfo;

/**
 * LOB 大字段分片事件。
 *
 * <p>当 DML 事件中的 LOB 字段过大时，会被拆分为多个 Chunk 事件分片传输。
 *
 * @param byteSize 事件字节大小
 * @param position 日志位点
 * @param table 目标表信息
 * @param columnIndex LOB 字段所在的列索引
 * @param data 分片数据（字符串表示）
 * @param isEnd 是否为当前事件段的结尾
 * @param isLastChunk 是否为该 LOB 字段的最后一个分片
 * @param chunkSeq 分片序号（从 0 开始）
 * @param changeAt 数据变更时间戳（毫秒）
 * @param processAt 事件处理时间戳（毫秒）
 */
public record CdcChunkEvent(
    int byteSize,
    LogPosition position,
    TableInfo table,
    int columnIndex,
    String data,
    boolean isEnd,
    boolean isLastChunk,
    int chunkSeq,
    long changeAt,
    long processAt)
    implements CdcEvent {

  @Override
  public CdcEventType type() {
    return CdcEventType.CHUNK;
  }
}
