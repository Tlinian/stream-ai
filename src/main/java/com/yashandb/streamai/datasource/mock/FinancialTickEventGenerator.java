/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.mock;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.event.CdcDdlEvent;
import com.yashandb.streamai.datasource.event.CdcDmlEvent;
import com.yashandb.streamai.datasource.pojo.CdcOperationType;
import com.yashandb.streamai.datasource.pojo.ColumnValue;
import com.yashandb.streamai.datasource.pojo.LogPosition;
import com.yashandb.streamai.datasource.pojo.TableInfo;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 金融行情场景事件生成器。
 *
 * <p>对应商业企划中"金融行业 — 实时行情"场景。 模拟证券/期货市场的实时行情 CDC 事件流， 覆盖逐笔成交、K 线聚合、盘口深度等核心行情数据。 涉及表：tick_data（逐笔行情）、
 * kline_1m（1 分钟 K 线）、 order_book（盘口挂单簿）。
 *
 * <p>业务场景：实时行情数据接入， 结合大模型做行情异动检测和交易信号分析。
 *
 * <h2>DML 变更矩阵</h2>
 *
 * <table>
 * <caption>金融行情典型 DML 操作</caption>
 * <tr>
 *   <th>表</th><th>操作</th><th>before</th>
 *   <th>after</th><th>说明</th>
 * </tr>
 * <tr>
 *   <td>tick_data</td><td>INSERT</td>
 *   <td>空</td>
 *   <td>instrument,tickTime,lastPrice,bid,ask</td>
 *   <td>逐笔行情快照</td>
 * </tr>
 * <tr>
 *   <td>kline_1m</td><td>INSERT</td>
 *   <td>空</td>
 *   <td>instrument,barTime,open,high,low,close</td>
 *   <td>1 分钟 K 线聚合</td>
 * </tr>
 * <tr>
 *   <td>order_book</td><td>UPDATE</td>
 *   <td>instrument,bid,bidQty,ask,askQty</td>
 *   <td>instrument,newBid,newQty,newAsk,newQty</td>
 *   <td>盘口深度变动</td>
 * </tr>
 * </table>
 */
public final class FinancialTickEventGenerator implements MockEventGenerator {

  private static final String SCHEMA = "market";
  private static final int MOCK_BYTE_SIZE = 448;

  private static final TableInfo TICK_DATA = new TableInfo(SCHEMA, "tick_data");
  private static final TableInfo KLINE = new TableInfo(SCHEMA, "kline_1m");
  private static final TableInfo ORDER_BOOK = new TableInfo(SCHEMA, "order_book");

  private static final String[] DDL_TEMPLATES = {
    "ALTER TABLE %s ADD INDEX idx_tick_time (tick_time)",
    "ALTER TABLE %s ADD PARTITION (PARTITION p202605 VALUES LESS THAN ('2026-06-01'))",
    "ALTER TABLE %s ADD COLUMN trade_type VARCHAR(16)",
    "CREATE INDEX idx_instrument_time ON %s (instrument, bar_time)",
    "ALTER TABLE %s ADD COLUMN market_maker_flag TINYINT DEFAULT 0"
  };

  @Override
  public CdcEvent generateDdl(final long position, final TableInfo table) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final String template = DDL_TEMPLATES[rnd.nextInt(DDL_TEMPLATES.length)];
    final String ddl = String.format(template, table.schema() + "." + table.name());
    final long now = System.currentTimeMillis();
    return new CdcDdlEvent(128, new LogPosition(String.valueOf(position)), table, ddl, now, now);
  }

  private static final String[] INSTRUMENTS = {
    "600519.SH", "000001.SZ", "300750.SZ", "IF2406", "AU2406"
  };

  @Override
  public CdcEvent generate(final long position) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final long now = System.currentTimeMillis();
    final LogPosition pos = new LogPosition(String.valueOf(position));
    final String instrument = INSTRUMENTS[rnd.nextInt(INSTRUMENTS.length)];

    // 逐笔行情 70%、K 线聚合 20%、盘口深度 10%
    final int op = rnd.nextInt(100);
    if (op < 70) {
      return buildTickInsert(pos, instrument, now, rnd);
    } else if (op < 90) {
      return buildKlineInsert(pos, instrument, now, rnd);
    } else {
      return buildOrderBookUpdate(pos, instrument, now, rnd);
    }
  }

  private CdcEvent buildTickInsert(
      final LogPosition pos, final String instrument, final long now, final ThreadLocalRandom rnd) {
    final double lastPrice = rnd.nextDouble(10.0, 2000.0);
    final List<ColumnValue> after =
        List.of(
            ColumnValue.of(0, instrument),
            ColumnValue.of(1, now),
            ColumnValue.of(2, lastPrice),
            ColumnValue.of(3, lastPrice - rnd.nextDouble(0.01, 5.0)),
            ColumnValue.of(4, lastPrice + rnd.nextDouble(0.01, 5.0)),
            ColumnValue.of(5, rnd.nextLong(100, 100000)),
            ColumnValue.of(6, rnd.nextDouble(1000, 10000000)));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, TICK_DATA, CdcOperationType.INSERT, List.of(), after, now, now);
  }

  private CdcEvent buildKlineInsert(
      final LogPosition pos, final String instrument, final long now, final ThreadLocalRandom rnd) {
    final double open = rnd.nextDouble(10.0, 2000.0);
    final double close = open + rnd.nextDouble(-10.0, 10.0);
    final double high = Math.max(open, close) + rnd.nextDouble(0.1, 20.0);
    final double low = Math.min(open, close) - rnd.nextDouble(0.1, 20.0);
    final List<ColumnValue> after =
        List.of(
            ColumnValue.of(0, instrument),
            ColumnValue.of(1, now),
            ColumnValue.of(2, open),
            ColumnValue.of(3, high),
            ColumnValue.of(4, low),
            ColumnValue.of(5, close),
            ColumnValue.of(6, rnd.nextLong(1000, 500000)));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, KLINE, CdcOperationType.INSERT, List.of(), after, now, now);
  }

  private CdcEvent buildOrderBookUpdate(
      final LogPosition pos, final String instrument, final long now, final ThreadLocalRandom rnd) {
    final double oldBid = rnd.nextDouble(10.0, 2000.0);
    final double oldAsk = oldBid + rnd.nextDouble(0.01, 2.0);
    final double newBid = oldBid + rnd.nextDouble(-1.0, 1.0);
    final double newAsk = newBid + rnd.nextDouble(0.01, 2.0);
    final List<ColumnValue> before =
        List.of(
            ColumnValue.of(0, instrument),
            ColumnValue.of(1, oldBid),
            ColumnValue.of(2, rnd.nextLong(100, 50000)),
            ColumnValue.of(3, oldAsk),
            ColumnValue.of(4, rnd.nextLong(100, 50000)));
    final List<ColumnValue> after =
        List.of(
            ColumnValue.of(0, instrument),
            ColumnValue.of(1, newBid),
            ColumnValue.of(2, rnd.nextLong(100, 50000)),
            ColumnValue.of(3, newAsk),
            ColumnValue.of(4, rnd.nextLong(100, 50000)));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, ORDER_BOOK, CdcOperationType.UPDATE, before, after, now, now);
  }

  @Override
  public List<TableInfo> tables() {
    return List.of(TICK_DATA, KLINE, ORDER_BOOK);
  }
}
