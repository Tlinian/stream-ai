/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource;

/**
 * CDC 事件统计指标，可用于单表维度或全局聚合维度。
 *
 * <p>记录从数据源启动到当前的累计事件统计， 既可作为 {@link SourceMetrics#total()} 的全局聚合， 也可作为按表维度的明细统计。
 *
 * @param eventCount 事件总数
 * @param dmlCount DML 事件累计数
 * @param ddlCount DDL 事件累计数
 * @param insertCount INSERT 操作累计数
 * @param updateCount UPDATE 操作累计数
 * @param deleteCount DELETE 操作累计数
 * @param chunkCount CHUNK 大字段事件累计数
 * @param totalBytes 累计字节数（含所有事件类型）
 * @param chunkBytes CHUNK 事件累计字节数
 */
public record TableMetrics(
    long eventCount,
    long dmlCount,
    long ddlCount,
    long insertCount,
    long updateCount,
    long deleteCount,
    long chunkCount,
    long totalBytes,
    long chunkBytes) {

  /** 空指标。 */
  public static final TableMetrics EMPTY = new TableMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0);
}
