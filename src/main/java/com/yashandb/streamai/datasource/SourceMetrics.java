/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource;

import java.util.Map;

/**
 * CDC 数据源运行指标统计。
 *
 * <p>记录数据源从启动到当前的累计运行指标， 用于监控数据源健康状态和吞吐能力。
 *
 * <p>{@link #total()} 为全局聚合，包含所有表的事件统计； {@link #tableMetrics()} 为按表维度的明细统计。
 *
 * @param total 全局聚合指标（所有表的汇总）
 * @param transactionCount 事务数（BEGIN+COMMIT 配对计数，跨表事件）
 * @param uptimeMs 运行时长（毫秒）
 * @param rps 每秒记录数（Records Per Second）
 * @param tps 每秒事务数（Transactions Per Second）
 * @param tableMetrics 按表维度的指标统计，键为 "schema.table"
 */
public record SourceMetrics(
    TableMetrics total,
    long transactionCount,
    long uptimeMs,
    double rps,
    double tps,
    Map<String, TableMetrics> tableMetrics) {

  /** 空指标，用于数据源尚未启动时。 */
  public static final SourceMetrics EMPTY =
      new SourceMetrics(TableMetrics.EMPTY, 0, 0, 0.0, 0.0, Map.of());

  /** 便捷获取事件总数。 */
  public long totalEvents() {
    return total.eventCount();
  }
}
