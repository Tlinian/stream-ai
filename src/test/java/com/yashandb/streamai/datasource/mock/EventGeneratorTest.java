/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.event.CdcDdlEvent;
import com.yashandb.streamai.datasource.event.CdcDmlEvent;
import com.yashandb.streamai.datasource.pojo.CdcOperationType;
import com.yashandb.streamai.datasource.pojo.TableInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** 所有 6 个事件生成器的单元测试。 */
public class EventGeneratorTest {

  private static final int SAMPLE_SIZE = 10000;

  // ==================== TpccEventGenerator ====================

  @Test
  public void tpccGeneratorAlwaysProducesDml() {
    final TpccEventGenerator gen = new TpccEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcEvent event = gen.generate(i);
      assertTrue("TPC-C 应只产生 DML 事件", event instanceof CdcDmlEvent);
    }
  }

  @Test
  public void tpccGeneratorWeightedDistribution() {
    final TpccEventGenerator gen = new TpccEventGenerator();
    final Map<String, Integer> tableCounts = new HashMap<>();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      tableCounts.merge(dml.table().name(), 1, Integer::sum);
    }
    // orders ~40%, order_line ~30%, stock ~30%
    assertRatio(tableCounts, "orders", 0.35, 0.45);
    assertRatio(tableCounts, "order_line", 0.25, 0.35);
    assertRatio(tableCounts, "stock", 0.25, 0.35);
  }

  @Test
  public void tpccGeneratorTablesList() {
    final TpccEventGenerator gen = new TpccEventGenerator();
    final List<TableInfo> tables = gen.tables();
    assertEquals(3, tables.size());
    assertEquals("tpcc.orders", tables.get(0).toString());
    assertEquals("tpcc.order_line", tables.get(1).toString());
    assertEquals("tpcc.stock", tables.get(2).toString());
  }

  @Test
  public void tpccOrdersAreInserts() {
    final TpccEventGenerator gen = new TpccEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      if ("orders".equals(dml.table().name())) {
        assertEquals(CdcOperationType.INSERT, dml.operation());
        assertTrue(dml.beforeColumns().isEmpty());
        assertFalse(dml.afterColumns().isEmpty());
      }
    }
  }

  @Test
  public void tpccStockIsUpdate() {
    final TpccEventGenerator gen = new TpccEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      if ("stock".equals(dml.table().name())) {
        assertEquals(CdcOperationType.UPDATE, dml.operation());
        assertFalse(dml.beforeColumns().isEmpty());
        assertFalse(dml.afterColumns().isEmpty());
      }
    }
  }

  @Test
  public void tpccPositionUsed() {
    final TpccEventGenerator gen = new TpccEventGenerator();
    final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(42);
    assertEquals("42", dml.position().position());
  }

  @Test
  public void tpccAfterColumnsHave6Fields() {
    final TpccEventGenerator gen = new TpccEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      if ("orders".equals(dml.table().name())) {
        assertEquals("orders INSERT 应有 6 列", 6, dml.afterColumns().size());
        break;
      }
    }
  }

  // ==================== FinanceRiskEventGenerator ====================

  @Test
  public void financeRiskGeneratorAlwaysProducesDml() {
    final FinanceRiskEventGenerator gen = new FinanceRiskEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      assertTrue(gen.generate(i) instanceof CdcDmlEvent);
    }
  }

  @Test
  public void financeRiskGeneratorWeightedDistribution() {
    final FinanceRiskEventGenerator gen = new FinanceRiskEventGenerator();
    final Map<String, Integer> tableCounts = new HashMap<>();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      tableCounts.merge(dml.table().name(), 1, Integer::sum);
    }
    // transaction ~60%, risk_alert ~10%, account ~30%
    assertRatio(tableCounts, "transaction", 0.55, 0.65);
    assertRatio(tableCounts, "risk_alert", 0.05, 0.15);
    assertRatio(tableCounts, "account", 0.25, 0.35);
  }

  @Test
  public void financeRiskGeneratorTablesList() {
    final FinanceRiskEventGenerator gen = new FinanceRiskEventGenerator();
    final List<TableInfo> tables = gen.tables();
    assertEquals(3, tables.size());
    assertEquals("finance", tables.get(0).schema());
  }

  @Test
  public void financeRiskAccountIsUpdate() {
    final FinanceRiskEventGenerator gen = new FinanceRiskEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      if ("account".equals(dml.table().name())) {
        assertEquals(CdcOperationType.UPDATE, dml.operation());
        assertFalse(dml.beforeColumns().isEmpty());
      }
    }
  }

  // ==================== TelecomReconEventGenerator ====================

  @Test
  public void telecomReconGeneratorWeightedDistribution() {
    final TelecomReconEventGenerator gen = new TelecomReconEventGenerator();
    final Map<String, Integer> tableCounts = new HashMap<>();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      tableCounts.merge(dml.table().name(), 1, Integer::sum);
    }
    // billing_record ~70%, settlement ~20%, reconciliation_diff ~10%
    assertRatio(tableCounts, "billing_record", 0.65, 0.75);
    assertRatio(tableCounts, "settlement", 0.15, 0.25);
    assertRatio(tableCounts, "reconciliation_diff", 0.05, 0.15);
  }

  @Test
  public void telecomReconGeneratorTablesList() {
    final TelecomReconEventGenerator gen = new TelecomReconEventGenerator();
    final List<TableInfo> tables = gen.tables();
    assertEquals(3, tables.size());
    assertEquals("telecom", tables.get(0).schema());
  }

  @Test
  public void telecomReconSettlementIsUpdate() {
    final TelecomReconEventGenerator gen = new TelecomReconEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      if ("settlement".equals(dml.table().name())) {
        assertEquals(CdcOperationType.UPDATE, dml.operation());
        assertFalse(dml.beforeColumns().isEmpty());
      }
    }
  }

  @Test
  public void telecomReconReconDiffSystemsDiffer() {
    final TelecomReconEventGenerator gen = new TelecomReconEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      if ("reconciliation_diff".equals(dml.table().name())) {
        final String sysA = (String) dml.afterColumns().get(1).value();
        final String sysB = (String) dml.afterColumns().get(2).value();
        assertNotEquals("对账差异的两系统不应相同", sysA, sysB);
      }
    }
  }

  // ==================== IotManufacturingEventGenerator ====================

  @Test
  public void iotManufacturingGeneratorWeightedDistribution() {
    final IotManufacturingEventGenerator gen = new IotManufacturingEventGenerator();
    final Map<String, Integer> tableCounts = new HashMap<>();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      tableCounts.merge(dml.table().name(), 1, Integer::sum);
    }
    // sensor_reading ~80%, quality_inspection ~15%, equipment_status ~5%
    assertRatio(tableCounts, "sensor_reading", 0.75, 0.85);
    assertRatio(tableCounts, "quality_inspection", 0.10, 0.20);
    assertRatio(tableCounts, "equipment_status", 0.02, 0.08);
  }

  @Test
  public void iotManufacturingGeneratorTablesList() {
    final IotManufacturingEventGenerator gen = new IotManufacturingEventGenerator();
    final List<TableInfo> tables = gen.tables();
    assertEquals(3, tables.size());
    assertEquals("manufacturing", tables.get(0).schema());
  }

  @Test
  public void iotManufacturingEquipmentStatusChanges() {
    final IotManufacturingEventGenerator gen = new IotManufacturingEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      if ("equipment_status".equals(dml.table().name())) {
        assertEquals(CdcOperationType.UPDATE, dml.operation());
        final String beforeStatus = (String) dml.beforeColumns().get(1).value();
        final String afterStatus = (String) dml.afterColumns().get(1).value();
        assertNotEquals("设备状态应变更", beforeStatus, afterStatus);
      }
    }
  }

  // ==================== EcommerceEventGenerator ====================

  @Test
  public void ecommerceGeneratorWeightedDistribution() {
    final EcommerceEventGenerator gen = new EcommerceEventGenerator();
    final Map<String, Integer> tableCounts = new HashMap<>();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      tableCounts.merge(dml.table().name(), 1, Integer::sum);
    }
    // orders ~40%, payments ~35%, inventory ~25%
    assertRatio(tableCounts, "orders", 0.35, 0.45);
    assertRatio(tableCounts, "payments", 0.30, 0.40);
    assertRatio(tableCounts, "inventory", 0.20, 0.30);
  }

  @Test
  public void ecommerceGeneratorTablesList() {
    final EcommerceEventGenerator gen = new EcommerceEventGenerator();
    final List<TableInfo> tables = gen.tables();
    assertEquals(3, tables.size());
    assertEquals("ecommerce", tables.get(0).schema());
  }

  @Test
  public void ecommerceInventoryIsUpdate() {
    final EcommerceEventGenerator gen = new EcommerceEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      if ("inventory".equals(dml.table().name())) {
        assertEquals(CdcOperationType.UPDATE, dml.operation());
        final int oldQty = Integer.parseInt(dml.beforeColumns().get(1).value());
        final int newQty = Integer.parseInt(dml.afterColumns().get(1).value());
        assertTrue("库存应减少或不变", newQty <= oldQty);
      }
    }
  }

  // ==================== FinancialTickEventGenerator ====================

  @Test
  public void financialTickGeneratorWeightedDistribution() {
    final FinancialTickEventGenerator gen = new FinancialTickEventGenerator();
    final Map<String, Integer> tableCounts = new HashMap<>();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      tableCounts.merge(dml.table().name(), 1, Integer::sum);
    }
    // tick_data ~70%, kline_1m ~20%, order_book ~10%
    assertRatio(tableCounts, "tick_data", 0.65, 0.75);
    assertRatio(tableCounts, "kline_1m", 0.15, 0.25);
    assertRatio(tableCounts, "order_book", 0.05, 0.15);
  }

  @Test
  public void financialTickGeneratorTablesList() {
    final FinancialTickEventGenerator gen = new FinancialTickEventGenerator();
    final List<TableInfo> tables = gen.tables();
    assertEquals(3, tables.size());
    assertEquals("market", tables.get(0).schema());
  }

  @Test
  public void financialTickOrderBookIsUpdate() {
    final FinancialTickEventGenerator gen = new FinancialTickEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      if ("order_book".equals(dml.table().name())) {
        assertEquals(CdcOperationType.UPDATE, dml.operation());
        assertFalse(dml.beforeColumns().isEmpty());
        assertFalse(dml.afterColumns().isEmpty());
      }
    }
  }

  @Test
  public void financialTickTickDataIsInsert() {
    final FinancialTickEventGenerator gen = new FinancialTickEventGenerator();
    for (int i = 1; i <= SAMPLE_SIZE; i++) {
      final CdcDmlEvent dml = (CdcDmlEvent) gen.generate(i);
      if ("tick_data".equals(dml.table().name())) {
        assertEquals(CdcOperationType.INSERT, dml.operation());
        assertTrue(dml.beforeColumns().isEmpty());
      }
    }
  }

  // ==================== 通用 ====================

  @Test
  public void allGeneratorsProduceNonNullEvents() {
    final MockEventGenerator[] generators = {
      new TpccEventGenerator(),
      new FinanceRiskEventGenerator(),
      new TelecomReconEventGenerator(),
      new IotManufacturingEventGenerator(),
      new EcommerceEventGenerator(),
      new FinancialTickEventGenerator()
    };
    for (final MockEventGenerator gen : generators) {
      for (int i = 1; i <= 100; i++) {
        assertNotNull(gen.getClass().getSimpleName() + " pos=" + i, gen.generate(i));
      }
    }
  }

  @Test
  public void allGeneratorsTablesNotEmpty() {
    final MockEventGenerator[] generators = {
      new TpccEventGenerator(),
      new FinanceRiskEventGenerator(),
      new TelecomReconEventGenerator(),
      new IotManufacturingEventGenerator(),
      new EcommerceEventGenerator(),
      new FinancialTickEventGenerator()
    };
    for (final MockEventGenerator gen : generators) {
      assertFalse(gen.getClass().getSimpleName(), gen.tables().isEmpty());
    }
  }

  @Test
  public void defaultTablesMethodReturnsEmpty() {
    final MockEventGenerator empty =
        new MockEventGenerator() {
          @Override
          public CdcEvent generate(final long position) {
            return null;
          }

          @Override
          public CdcEvent generateDdl(final long position, final TableInfo table) {
            return null;
          }
        };
    assertTrue(empty.tables().isEmpty());
  }

  @Test
  public void allDmlEventsHaveValidByteSize() {
    final MockEventGenerator[] generators = {
      new TpccEventGenerator(),
      new FinanceRiskEventGenerator(),
      new TelecomReconEventGenerator(),
      new IotManufacturingEventGenerator(),
      new EcommerceEventGenerator(),
      new FinancialTickEventGenerator()
    };
    for (final MockEventGenerator gen : generators) {
      for (int i = 1; i <= 100; i++) {
        assertTrue(gen.getClass().getSimpleName(), gen.generate(i).byteSize() > 0);
      }
    }
  }

  @Test
  public void allGeneratorsProduceDomainSpecificDdl() {
    final MockEventGenerator[] generators = {
      new TpccEventGenerator(),
      new FinanceRiskEventGenerator(),
      new TelecomReconEventGenerator(),
      new IotManufacturingEventGenerator(),
      new EcommerceEventGenerator(),
      new FinancialTickEventGenerator()
    };
    for (final MockEventGenerator gen : generators) {
      final List<TableInfo> tables = gen.tables();
      assertFalse(gen.getClass().getSimpleName(), tables.isEmpty());
      for (final TableInfo table : tables) {
        final CdcEvent ddl = gen.generateDdl(1, table);
        assertNotNull(gen.getClass().getSimpleName() + " DDL for " + table.name(), ddl);
        assertTrue(ddl instanceof CdcDdlEvent);
        final CdcDdlEvent ddlEvent = (CdcDdlEvent) ddl;
        // DDL SQL 应包含表名（schema.table 格式）
        assertTrue(
            gen.getClass().getSimpleName() + " DDL should reference table",
            ddlEvent.ddl().contains(table.schema() + "." + table.name()));
      }
    }
  }

  @Test
  public void generatorsProduceDistinctDdlTemplates() {
    // 不同场景生成器的 DDL 模板应当不同（各自领域特定）
    final TpccEventGenerator tpcc = new TpccEventGenerator();
    final FinanceRiskEventGenerator finance = new FinanceRiskEventGenerator();
    final TableInfo tpccTable = tpcc.tables().get(0);
    final TableInfo financeTable = finance.tables().get(0);

    // 多次采样收集 DDL 模板
    final java.util.Set<String> tpccDdls = new java.util.HashSet<>();
    final java.util.Set<String> financeDdls = new java.util.HashSet<>();
    for (int i = 1; i <= 50; i++) {
      tpccDdls.add(((CdcDdlEvent) tpcc.generateDdl(i, tpccTable)).ddl());
      financeDdls.add(((CdcDdlEvent) finance.generateDdl(i, financeTable)).ddl());
    }
    // 两个场景的 DDL 模板集合不应有交集
    for (final String tpccDdl : tpccDdls) {
      assertFalse(
          "TPCC DDL should not appear in finance DDLs: " + tpccDdl, financeDdls.contains(tpccDdl));
    }
  }

  private static void assertRatio(
      final Map<String, Integer> counts,
      final String key,
      final double minRatio,
      final double maxRatio) {
    final int count = counts.getOrDefault(key, 0);
    final double ratio = (double) count / SAMPLE_SIZE;
    assertTrue(
        key + " 比例 " + ratio + " 不在 [" + minRatio + ", " + maxRatio + "] 范围内",
        ratio >= minRatio && ratio <= maxRatio);
  }
}
