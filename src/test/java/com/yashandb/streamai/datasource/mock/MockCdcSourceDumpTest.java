/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.mock;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.MockSceneType;
import com.yashandb.streamai.datasource.SourceMetrics;
import com.yashandb.streamai.datasource.TableMetrics;
import com.yashandb.streamai.datasource.event.CdcBeginEvent;
import com.yashandb.streamai.datasource.event.CdcChunkEvent;
import com.yashandb.streamai.datasource.event.CdcCommitEvent;
import com.yashandb.streamai.datasource.event.CdcDdlEvent;
import com.yashandb.streamai.datasource.event.CdcDmlEvent;
import com.yashandb.streamai.datasource.pojo.ColumnValue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Mock 数据源落盘验证，将事件流写入文件供人工审查。 */
public class MockCdcSourceDumpTest {

  private static final int EVENTS_PER_SCENE = 30;
  private static final String DUMP_FILE = "build/mock-data-dump.log";

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
  public void dumpAllScenes() throws Exception {
    final File file = new File(DUMP_FILE);
    file.getParentFile().mkdirs();

    try (final PrintWriter out =
        new PrintWriter(
            new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
      out.println("Mock CDC 数据源落盘验证");
      out.println("生成时间: " + java.time.LocalDateTime.now());
      out.println("每场景事件数: " + EVENTS_PER_SCENE);
      out.println();

      for (final MockSceneType scene : MockSceneType.values()) {
        out.println("=".repeat(90));
        out.println("  " + scene.name() + " — " + scene.description());
        out.println("=".repeat(90));
        runScene(scene, out);
      }

      out.flush();
    }

    // 控制台也打印文件路径
    System.out.println("数据已落盘到: " + file.getAbsolutePath());
  }

  private void runScene(final MockSceneType scene, final PrintWriter out) throws Exception {
    try (final MockCdcSource source =
        MockCdcSource.of("dump-" + scene.name(), newGenerator(scene), fastProfile())) {
      source.start();
      int count = 0;
      int dmlInTxn = 0;
      int txnIndex = 0;

      while (count < EVENTS_PER_SCENE) {
        final CdcEvent event = source.next();
        if (event == null) {
          break;
        }

        if (event instanceof CdcBeginEvent) {
          final CdcBeginEvent begin = (CdcBeginEvent) event;
          txnIndex++;
          out.printf(
              "%n  ┌─ 事务 #%d  [%s]  txnId=%s  pos=%s%n",
              txnIndex,
              java.time.Instant.ofEpochMilli(System.currentTimeMillis())
                  .atZone(java.time.ZoneId.systemDefault())
                  .toLocalTime(),
              begin.transactionInfo().transactionId(),
              begin.position().position());
          dmlInTxn = 0;
        } else if (event instanceof CdcCommitEvent) {
          final CdcCommitEvent commit = (CdcCommitEvent) event;
          out.printf(
              "  └─ COMMIT  txnId=%s  pos=%s  dml=%d%n",
              commit.transactionInfo().transactionId(), commit.position().position(), dmlInTxn);
        } else if (event instanceof CdcDmlEvent) {
          final CdcDmlEvent dml = (CdcDmlEvent) event;
          out.printf(
              "  │ [%-8s] %-35s pos=%-6s before=%-50s after=%s%n",
              dml.operation(),
              dml.table(),
              dml.position().position(),
              formatColumns(dml.beforeColumns()),
              formatColumns(dml.afterColumns()));
          dmlInTxn++;
        } else if (event instanceof CdcChunkEvent) {
          final CdcChunkEvent chunk = (CdcChunkEvent) event;
          out.printf(
              "  │ [%-8s] %-35s pos=%-6s col=%d seq=%d last=%s size=%d%n",
              "CHUNK",
              chunk.table(),
              chunk.position().position(),
              chunk.columnIndex(),
              chunk.chunkSeq(),
              chunk.isLastChunk(),
              chunk.byteSize());
        } else if (event instanceof CdcDdlEvent) {
          final CdcDdlEvent ddl = (CdcDdlEvent) event;
          out.printf(
              "  │ [%-8s] %-35s pos=%-6s ddl=%s%n",
              "DDL", ddl.table(), ddl.position().position(), ddl.ddl());
        } else {
          out.printf("  │ [%-8s] pos=%s%n", event.type(), event.position().position());
        }
        count++;
      }

      final SourceMetrics metrics = source.metrics();

      out.println();
      out.println("  ┌───────────── 运行指标 ─────────────┐");
      out.printf("  │ 事务总数: %d%n", metrics.transactionCount());
      out.printf(
          "  │ 事件总数: %d  (DML: %d  DDL: %d)%n",
          metrics.total().eventCount(), metrics.total().dmlCount(), metrics.total().ddlCount());
      out.printf(
          "  │ INSERT: %d  UPDATE: %d  DELETE: %d%n",
          metrics.total().insertCount(),
          metrics.total().updateCount(),
          metrics.total().deleteCount());
      out.printf("  │ RPS: %.2f  TPS: %.2f%n", metrics.rps(), metrics.tps());
      out.printf(
          "  │ CHUNK: %d  chunkBytes: %d%n",
          metrics.total().chunkCount(), metrics.total().chunkBytes());
      out.printf("  │ 运行时长: %dms  总字节: %d%n", metrics.uptimeMs(), metrics.total().totalBytes());

      if (!metrics.tableMetrics().isEmpty()) {
        out.println("  ├───────────── 按表维度 ─────────────┤");
        for (final Map.Entry<String, TableMetrics> entry : metrics.tableMetrics().entrySet()) {
          final TableMetrics tm = entry.getValue();
          out.printf(
              "  │ %-35s events=%-5d dml=%-5d I=%-5d U=%-5d D=%-5d chunks=%-3d bytes=%d%n",
              entry.getKey(),
              tm.eventCount(),
              tm.dmlCount(),
              tm.insertCount(),
              tm.updateCount(),
              tm.deleteCount(),
              tm.chunkCount(),
              tm.totalBytes());
        }
      }
      out.println("  └─────────────────────────────────────┘");
      out.println();
    }
  }

  private static String formatColumns(final List<ColumnValue> columns) {
    if (columns == null || columns.isEmpty()) {
      return "{}";
    }
    final StringBuilder sb = new StringBuilder("{");
    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      final ColumnValue cv = columns.get(i);
      final String val = cv.value();
      sb.append(cv.index()).append(":").append(val);
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
}
