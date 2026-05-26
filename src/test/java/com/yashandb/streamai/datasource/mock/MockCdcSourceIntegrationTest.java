/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.mock;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.MockSceneType;
import com.yashandb.streamai.datasource.SourceMetrics;
import com.yashandb.streamai.datasource.TableMetrics;
import com.yashandb.streamai.datasource.event.CdcBeginEvent;
import com.yashandb.streamai.datasource.event.CdcCommitEvent;
import com.yashandb.streamai.datasource.event.CdcDmlEvent;
import com.yashandb.streamai.datasource.pojo.ColumnValue;
import java.util.Map;
import org.junit.Test;

/** Mock 数据源集成验证，遍历所有预置场景并输出事件流供人工检查。 */
public class MockCdcSourceIntegrationTest {

  private static final int EVENTS_PER_SCENE = 20;

  /** 高速测试画像。 */
  private static TrafficProfile fastProfile() {
    return TrafficProfile.builder()
        .baseTps(1000)
        .pullRps(1_000_000)
        .peak(0, 24, 1.0)
        .concurrentSessions(4)
        .build();
  }

  @Test
  public void testAllScenes() throws Exception {
    for (final MockSceneType scene : MockSceneType.values()) {
      printSeparator(scene.name() + " — " + scene.description());
      runScene(scene);
    }
  }

  private void runScene(final MockSceneType scene) throws Exception {
    try (final MockCdcSource source =
        MockCdcSource.of("int-" + scene.name(), newGenerator(scene), fastProfile())) {
      source.start();
      int count = 0;
      int txnBeginCount = 0;
      int txnCommitCount = 0;
      int dmlCount = 0;
      String currentTxnId = null;

      while (count < EVENTS_PER_SCENE) {
        final CdcEvent event = source.next();
        if (event == null) {
          break;
        }

        if (event instanceof CdcBeginEvent) {
          final CdcBeginEvent begin = (CdcBeginEvent) event;
          currentTxnId = begin.transactionInfo().transactionId();
          System.out.printf(
              "  [BEGIN]  txn=%s pos=%s%n", currentTxnId, begin.position().position());
          txnBeginCount++;
        } else if (event instanceof CdcCommitEvent) {
          final CdcCommitEvent commit = (CdcCommitEvent) event;
          System.out.printf(
              "  [COMMIT] txn=%s pos=%s  (DML: %d)%n",
              commit.transactionInfo().transactionId(), commit.position().position(), dmlCount);
          txnCommitCount++;
          dmlCount = 0;
        } else if (event instanceof CdcDmlEvent) {
          final CdcDmlEvent dml = (CdcDmlEvent) event;
          System.out.printf(
              "    [DML] %-8s %s  before=%s  after=%s%n",
              dml.operation(),
              dml.table(),
              formatColumns(dml.beforeColumns()),
              formatColumns(dml.afterColumns()));
          dmlCount++;
        } else {
          System.out.printf("  [%s] pos=%s%n", event.type(), event.position().position());
        }
        count++;
      }

      // 输出运行指标
      final SourceMetrics metrics = source.metrics();
      System.out.println();
      System.out.println("  ─── 运行指标 ───");
      System.out.printf(
          "  事务数: %d  (BEGIN: %d, COMMIT: %d)%n",
          metrics.transactionCount(), txnBeginCount, txnCommitCount);
      System.out.printf(
          "  事件总数: %d  DML: %d  DDL: %d%n",
          metrics.total().eventCount(), metrics.total().dmlCount(), metrics.total().ddlCount());
      System.out.printf(
          "  INSERT: %d  UPDATE: %d  DELETE: %d%n",
          metrics.total().insertCount(),
          metrics.total().updateCount(),
          metrics.total().deleteCount());
      System.out.printf(
          "  RPS: %.2f  TPS: %.2f  运行时长: %dms%n", metrics.rps(), metrics.tps(), metrics.uptimeMs());
      System.out.printf("  总字节: %d%n", metrics.total().totalBytes());

      // 按表维度指标
      if (!metrics.tableMetrics().isEmpty()) {
        System.out.println("  ─── 按表维度 ───");
        for (final Map.Entry<String, TableMetrics> entry : metrics.tableMetrics().entrySet()) {
          final TableMetrics tm = entry.getValue();
          System.out.printf(
              "  %s: events=%d dml=%d (I=%d U=%d D=%d) bytes=%d%n",
              entry.getKey(),
              tm.eventCount(),
              tm.dmlCount(),
              tm.insertCount(),
              tm.updateCount(),
              tm.deleteCount(),
              tm.totalBytes());
        }
      }

      System.out.println();
    }
  }

  private static String formatColumns(final java.util.List<ColumnValue> columns) {
    if (columns == null || columns.isEmpty()) {
      return "{}";
    }
    final StringBuilder sb = new StringBuilder("{");
    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      final ColumnValue cv = columns.get(i);
      sb.append(cv.index()).append(":").append(cv.value());
    }
    sb.append("}");
    return sb.toString();
  }

  /** 根据场景创建对应生成器。 */
  private static MockEventGenerator newGenerator(final MockSceneType scene) {
    switch (scene) {
      case TPCC:
        return new TpccEventGenerator();
      case FINANCE_RISK:
        return new FinanceRiskEventGenerator();
      case TELECOM_RECON:
        return new TelecomReconEventGenerator();
      case IOT_MANUFACTURING:
        return new IotManufacturingEventGenerator();
      case ECOMMERCE:
        return new EcommerceEventGenerator();
      case FINANCIAL_TICK:
        return new FinancialTickEventGenerator();
      default:
        throw new IllegalArgumentException("未知场景: " + scene);
    }
  }

  private static void printSeparator(final String title) {
    System.out.println();
    System.out.println("=".repeat(80));
    System.out.println("  " + title);
    System.out.println("=".repeat(80));
  }
}
