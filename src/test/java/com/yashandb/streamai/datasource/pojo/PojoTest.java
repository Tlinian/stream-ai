/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.yashandb.streamai.datasource.SourceMetrics;
import com.yashandb.streamai.datasource.TableMetrics;
import java.util.Map;
import org.junit.Test;

/** POJO 类单元测试，覆盖 record 和枚举。 */
public class PojoTest {

  // ==================== TableInfo ====================

  @Test
  public void tableInfoWithSchemaAndName() {
    final TableInfo info = new TableInfo("finance", "transaction");
    assertEquals("finance", info.schema());
    assertEquals("transaction", info.name());
  }

  @Test
  public void tableInfoNameOnly() {
    final TableInfo info = new TableInfo("orders");
    assertNull(info.schema());
    assertEquals("orders", info.name());
  }

  @Test
  public void tableInfoToStringWithSchema() {
    final TableInfo info = new TableInfo("tpcc", "orders");
    assertEquals("tpcc.orders", info.toString());
  }

  @Test
  public void tableInfoToStringWithoutSchema() {
    final TableInfo info = new TableInfo("orders");
    assertEquals("orders", info.toString());
  }

  @Test
  public void tableInfoEquality() {
    final TableInfo a = new TableInfo("schema", "table");
    final TableInfo b = new TableInfo("schema", "table");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void tableInfoInequality() {
    final TableInfo a = new TableInfo("schema1", "table");
    final TableInfo b = new TableInfo("schema2", "table");
    assertNotEquals(a, b);
  }

  // ==================== LogPosition ====================

  @Test
  public void logPositionCreation() {
    final LogPosition pos = new LogPosition("12345");
    assertEquals("12345", pos.position());
  }

  @Test
  public void logPositionEquality() {
    assertEquals(new LogPosition("42"), new LogPosition("42"));
    assertNotEquals(new LogPosition("42"), new LogPosition("43"));
  }

  // ==================== ColumnValue ====================

  @Test
  public void columnValueCreation() {
    final ColumnValue cv = new ColumnValue(3, "hello");
    assertEquals(3, cv.index());
    assertEquals("hello", cv.value());
  }

  @Test
  public void columnValueWithNullValue() {
    final ColumnValue cv = new ColumnValue(0, null);
    assertEquals(0, cv.index());
    assertNull(cv.value());
  }

  @Test
  public void columnValueOfFactoryMethod() {
    assertEquals("42", ColumnValue.of(0, 42).value());
    assertEquals("3.14", ColumnValue.of(0, 3.14).value());
    assertEquals("100", ColumnValue.of(0, 100L).value());
    assertNull(ColumnValue.of(0, null).value());
  }

  @Test
  public void columnValueOfByteArrayToHex() {
    assertEquals("0a1bff00", ColumnValue.of(0, new byte[] {0x0A, 0x1B, (byte) 0xFF, 0x00}).value());
    assertEquals("", ColumnValue.of(0, new byte[] {}).value());
    assertEquals(
        "deadbeef",
        ColumnValue.of(0, new byte[] {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF}).value());
  }

  @Test
  public void columnValueEquality() {
    assertEquals(new ColumnValue(0, "x"), new ColumnValue(0, "x"));
    assertNotEquals(new ColumnValue(0, "x"), new ColumnValue(1, "x"));
  }

  // ==================== TransactionInfo ====================

  @Test
  public void transactionInfoCreation() {
    final TransactionInfo info = new TransactionInfo("TXN-001", 1, false);
    assertEquals("TXN-001", info.transactionId());
    assertEquals(1, info.transactionPart());
    assertEquals(false, info.isBigTransaction());
  }

  @Test
  public void transactionInfoNotParallelConstant() {
    assertEquals(-1, TransactionInfo.NOT_PARALLEL_TRANSACTION_PART);
  }

  @Test
  public void transactionInfoBigTransaction() {
    final TransactionInfo info = new TransactionInfo("TXN-BIG", 5, true);
    assertTrue(info.isBigTransaction());
    assertEquals(5, info.transactionPart());
  }

  @Test
  public void transactionInfoEquality() {
    assertEquals(new TransactionInfo("T1", 1, false), new TransactionInfo("T1", 1, false));
    assertNotEquals(new TransactionInfo("T1", 1, false), new TransactionInfo("T2", 1, false));
  }

  // ==================== CdcOperationType ====================

  @Test
  public void operationTypeValues() {
    assertEquals(3, CdcOperationType.values().length);
    assertNotNull(CdcOperationType.INSERT);
    assertNotNull(CdcOperationType.UPDATE);
    assertNotNull(CdcOperationType.DELETE);
  }

  @Test
  public void operationTypeValueOf() {
    assertEquals(CdcOperationType.INSERT, CdcOperationType.valueOf("INSERT"));
    assertEquals(CdcOperationType.UPDATE, CdcOperationType.valueOf("UPDATE"));
    assertEquals(CdcOperationType.DELETE, CdcOperationType.valueOf("DELETE"));
  }

  // ==================== CdcEventType ====================

  @Test
  public void eventTypeValues() {
    assertEquals(5, CdcEventType.values().length);
    assertNotNull(CdcEventType.BEGIN);
    assertNotNull(CdcEventType.COMMIT);
    assertNotNull(CdcEventType.DDL);
    assertNotNull(CdcEventType.DML);
    assertNotNull(CdcEventType.CHUNK);
  }

  @Test
  public void eventTypeValueOf() {
    for (final CdcEventType type : CdcEventType.values()) {
      assertEquals(type, CdcEventType.valueOf(type.name()));
    }
  }

  // ==================== SourceMetrics ====================

  @Test
  public void sourceMetricsEmpty() {
    assertNotNull(SourceMetrics.EMPTY);
    assertEquals(0, SourceMetrics.EMPTY.totalEvents());
    assertEquals(0, SourceMetrics.EMPTY.transactionCount());
    assertEquals(0, SourceMetrics.EMPTY.uptimeMs());
  }

  @Test
  public void sourceMetricsTotalEvents() {
    final TableMetrics total = new TableMetrics(100, 80, 5, 50, 20, 10, 3, 5000, 2000);
    final SourceMetrics metrics = new SourceMetrics(total, 10, 5000, 20.0, 2.0, Map.of());
    assertEquals(100, metrics.totalEvents());
    assertEquals(10, metrics.transactionCount());
  }

  // ==================== TableMetrics ====================

  @Test
  public void tableMetricsEmpty() {
    assertNotNull(TableMetrics.EMPTY);
    assertEquals(0, TableMetrics.EMPTY.eventCount());
    assertEquals(0, TableMetrics.EMPTY.dmlCount());
    assertEquals(0, TableMetrics.EMPTY.ddlCount());
    assertEquals(0, TableMetrics.EMPTY.insertCount());
    assertEquals(0, TableMetrics.EMPTY.updateCount());
    assertEquals(0, TableMetrics.EMPTY.deleteCount());
    assertEquals(0, TableMetrics.EMPTY.chunkCount());
    assertEquals(0, TableMetrics.EMPTY.totalBytes());
    assertEquals(0, TableMetrics.EMPTY.chunkBytes());
  }

  @Test
  public void tableMetricsCreation() {
    final TableMetrics m = new TableMetrics(100, 80, 5, 50, 20, 10, 3, 5000, 2000);
    assertEquals(100, m.eventCount());
    assertEquals(80, m.dmlCount());
    assertEquals(5, m.ddlCount());
    assertEquals(50, m.insertCount());
    assertEquals(20, m.updateCount());
    assertEquals(10, m.deleteCount());
    assertEquals(3, m.chunkCount());
    assertEquals(5000, m.totalBytes());
    assertEquals(2000, m.chunkBytes());
  }
}
