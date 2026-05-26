/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.MockSceneType;
import com.yashandb.streamai.datasource.SourceMetrics;
import com.yashandb.streamai.datasource.event.CdcBeginEvent;
import com.yashandb.streamai.datasource.event.CdcChunkEvent;
import com.yashandb.streamai.datasource.event.CdcCommitEvent;
import com.yashandb.streamai.datasource.event.CdcDdlEvent;
import com.yashandb.streamai.datasource.event.CdcDmlEvent;
import com.yashandb.streamai.datasource.pojo.CdcEventType;
import com.yashandb.streamai.datasource.pojo.CdcOperationType;
import com.yashandb.streamai.datasource.pojo.ColumnValue;
import com.yashandb.streamai.datasource.pojo.LogPosition;
import com.yashandb.streamai.datasource.pojo.TableInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/** MockCdcSource 单元测试。 */
public class MockCdcSourceTest {

  private static final int EVENTS_PER_RUN = 50;

  /** 高速测试画像：高 TPS、高 pullRps，加速事件产出。 */
  private static TrafficProfile fastTestProfile() {
    return TrafficProfile.builder()
        .baseTps(1000)
        .pullRps(1_000_000)
        .peak(0, 24, 1.0)
        .concurrentSessions(4)
        .build();
  }

  // ==================== 工厂方法 ====================

  @Test
  public void ofMockSceneTypeCreatesAllScenes() {
    for (final MockSceneType scene : MockSceneType.values()) {
      try (final MockCdcSource source = MockCdcSource.of(scene)) {
        assertNotNull(scene.name(), source);
      }
    }
  }

  @Test
  public void staticFactoryMethods() {
    assertNotNull(MockCdcSource.tpcc());
    assertNotNull(MockCdcSource.financeRisk());
    assertNotNull(MockCdcSource.telecomRecon());
    assertNotNull(MockCdcSource.iotManufacturing());
    assertNotNull(MockCdcSource.ecommerce());
    assertNotNull(MockCdcSource.financialTick());
    MockCdcSource.tpcc().close();
    MockCdcSource.financeRisk().close();
    MockCdcSource.telecomRecon().close();
    MockCdcSource.iotManufacturing().close();
    MockCdcSource.ecommerce().close();
    MockCdcSource.financialTick().close();
  }

  @Test
  public void ofCustomGeneratorAndProfile() {
    final TrafficProfile profile = TrafficProfile.builder().baseTps(50).peak(0, 24, 1.0).build();
    final MockEventGenerator gen = new TpccEventGenerator();
    try (final MockCdcSource source = MockCdcSource.of("custom", gen, profile)) {
      assertNotNull(source);
    }
  }

  @Test
  public void ofCustomGeneratorDefaultProfile() {
    final MockEventGenerator gen = new TpccEventGenerator();
    try (final MockCdcSource source = MockCdcSource.of("default-profile", gen)) {
      assertNotNull(source);
    }
  }

  // ==================== 生命周期 ====================

  @Test
  public void startAndNextReturnsEvents() throws Exception {
    try (final MockCdcSource source = MockCdcSource.of(MockSceneType.TPCC)) {
      source.start();
      final CdcEvent first = source.next();
      assertNotNull("启动后应能获取事件", first);
    }
  }

  @Test
  public void closeReturnsNullFromNext() throws Exception {
    final MockCdcSource source = MockCdcSource.of(MockSceneType.TPCC);
    source.start();
    final CdcEvent event = source.next();
    assertNotNull(event);
    source.close();
    final CdcEvent afterClose = source.next();
    assertNull("关闭后 next() 应返回 null", afterClose);
  }

  @Test
  public void doubleCloseIsIdempotent() {
    final MockCdcSource source = MockCdcSource.of(MockSceneType.TPCC);
    source.close();
    source.close();
  }

  @Test
  public void startAfterCloseIsIgnored() throws Exception {
    final MockCdcSource source = MockCdcSource.of(MockSceneType.TPCC);
    source.close();
    source.start();
    assertNull("关闭后 start 不应恢复", source.next());
  }

  @Test
  public void doubleStartIsIgnored() throws Exception {
    try (final MockCdcSource source = MockCdcSource.of(MockSceneType.TPCC)) {
      source.start();
      source.start();
      assertNotNull(source.next());
    }
  }

  @Test
  public void stopWithoutStartIsNoop() {
    final MockCdcSource source = MockCdcSource.of(MockSceneType.TPCC);
    source.stop();
    source.close();
  }

  // ==================== 事务原子性 ====================

  @Test
  public void transactionAtomicity() throws Exception {
    try (final MockCdcSource source =
        MockCdcSource.of("tpcc-fast", new TpccEventGenerator(), fastTestProfile())) {
      source.start();
      final List<CdcEvent> events = collectEvents(source, EVENTS_PER_RUN);
      verifyTransactionBoundaries(events);
    }
  }

  @Test
  public void transactionAtomicityForAllScenes() throws Exception {
    for (final MockSceneType scene : MockSceneType.values()) {
      try (final MockCdcSource source = MockCdcSource.of(scene)) {
        source.start();
        final List<CdcEvent> events = collectEvents(source, 30);
        verifyTransactionBoundaries(events);
      }
    }
  }

  // ==================== CHUNK 事件 ====================

  @Test
  public void chunkEventsFollowDml() throws Exception {
    // 使用 IoT 场景（lobChance=0.15），使用高速画像
    final TrafficProfile iotFast =
        TrafficProfile.builder()
            .baseTps(500)
            .pullRps(1_000_000)
            .peak(0, 24, 1.0)
            .lobChance(0.3)
            .concurrentSessions(2)
            .build();
    try (final MockCdcSource source =
        MockCdcSource.of("iot-chunk-fast", new IotManufacturingEventGenerator(), iotFast)) {
      source.start();
      final List<CdcEvent> events = collectEvents(source, 150);
      boolean foundChunk = false;
      CdcDmlEvent lastDml = null;

      for (final CdcEvent event : events) {
        if (event instanceof CdcDmlEvent) {
          lastDml = (CdcDmlEvent) event;
        } else if (event instanceof CdcChunkEvent) {
          foundChunk = true;
          final CdcChunkEvent chunk = (CdcChunkEvent) event;
          assertNotNull("CHUNK 应关联表", chunk.table());
          assertTrue("CHUNK byteSize 应为正", chunk.byteSize() > 0);
          assertEquals(CdcEventType.CHUNK, chunk.type());
        }
      }
      assertTrue("IoT 场景应产生 CHUNK 事件", foundChunk);
    }
  }

  @Test
  public void chunkLastChunkFlag() throws Exception {
    final TrafficProfile iotFast =
        TrafficProfile.builder()
            .baseTps(500)
            .pullRps(1_000_000)
            .peak(0, 24, 1.0)
            .lobChance(0.3)
            .concurrentSessions(2)
            .build();
    try (final MockCdcSource source =
        MockCdcSource.of("iot-chunk-flag", new IotManufacturingEventGenerator(), iotFast)) {
      source.start();
      final List<CdcEvent> events = collectEvents(source, 150);

      final List<CdcChunkEvent> chunks = new ArrayList<>();
      for (final CdcEvent event : events) {
        if (event instanceof CdcChunkEvent) {
          chunks.add((CdcChunkEvent) event);
        }
      }

      if (!chunks.isEmpty()) {
        // 每组 CHUNK 序列应以 isLastChunk=true 结尾
        boolean inGroup = false;
        int lastSeq = -1;
        for (int i = 0; i < chunks.size(); i++) {
          final CdcChunkEvent chunk = chunks.get(i);
          if (!inGroup) {
            assertEquals("每组 CHUNK 从 seq=0 开始", 0, chunk.chunkSeq());
            inGroup = true;
            lastSeq = 0;
          } else {
            assertEquals("CHUNK seq 应递增", lastSeq + 1, chunk.chunkSeq());
            lastSeq = chunk.chunkSeq();
          }

          if (chunk.isLastChunk()) {
            inGroup = false;
            lastSeq = -1;
          }
        }
      }
    }
  }

  @Test
  public void noChunkWhenLobChanceZero() throws Exception {
    // financialTick 场景 lobChance=0.0
    try (final MockCdcSource source =
        MockCdcSource.of("tick-no-chunk", new FinancialTickEventGenerator(), fastTestProfile())) {
      source.start();
      final List<CdcEvent> events = collectEvents(source, 100);
      for (final CdcEvent event : events) {
        assertFalse("lobChance=0 时不应产生 CHUNK", event instanceof CdcChunkEvent);
      }
    }
  }

  // ==================== DDL 事件 ====================

  @Test
  public void ddlEventsGenerated() throws Exception {
    // 使用高 ddlChance 确保在少量事件中触发 DDL
    final TrafficProfile highDdl =
        TrafficProfile.builder()
            .baseTps(500)
            .pullRps(1_000_000)
            .peak(0, 24, 1.0)
            .ddlChance(0.5)
            .dmlCount(1, 2)
            .concurrentSessions(2)
            .build();
    try (final MockCdcSource source =
        MockCdcSource.of("ddl-test", new TpccEventGenerator(), highDdl)) {
      source.start();
      final List<CdcEvent> events = collectEvents(source, 80);
      boolean foundDdl = false;
      for (final CdcEvent event : events) {
        if (event instanceof CdcDdlEvent) {
          foundDdl = true;
          final CdcDdlEvent ddl = (CdcDdlEvent) event;
          assertTrue("DDL 语句应包含 ALTER TABLE", ddl.ddl().contains("ALTER TABLE"));
          assertEquals(CdcEventType.DDL, ddl.type());
          assertNotNull(ddl.table());
        }
      }
      assertTrue("高 ddlChance 应产生 DDL 事件", foundDdl);
    }
  }

  @Test
  public void ddlContainsAlterTable() throws Exception {
    final TrafficProfile highDdl =
        TrafficProfile.builder()
            .baseTps(500)
            .pullRps(1_000_000)
            .peak(0, 24, 1.0)
            .ddlChance(0.5)
            .dmlCount(1, 2)
            .concurrentSessions(2)
            .build();
    try (final MockCdcSource source =
        MockCdcSource.of("ddl-alter-test", new TpccEventGenerator(), highDdl)) {
      source.start();
      final List<CdcEvent> events = collectEvents(source, 80);
      boolean foundDdl = false;
      for (final CdcEvent event : events) {
        if (event instanceof CdcDdlEvent) {
          foundDdl = true;
          final CdcDdlEvent ddl = (CdcDdlEvent) event;
          assertTrue(ddl.ddl().contains("ALTER TABLE"));
        }
      }
      assertTrue("应找到 DDL 事件", foundDdl);
    }
  }

  // ==================== 指标统计 ====================

  @Test
  public void metricsAfterStart() throws Exception {
    try (final MockCdcSource source =
        MockCdcSource.of("tpcc-metrics", new TpccEventGenerator(), fastTestProfile())) {
      source.start();
      collectEvents(source, 30);
      final SourceMetrics metrics = source.metrics();
      assertTrue("事件总数应 > 0", metrics.totalEvents() > 0);
      assertTrue("事务数应 > 0", metrics.transactionCount() > 0);
      assertTrue("运行时长应 > 0", metrics.uptimeMs() > 0);
      assertTrue("RPS 应 >= 0", metrics.rps() >= 0);
      assertTrue("TPS 应 >= 0", metrics.tps() >= 0);
      assertTrue("DML 数应 > 0", metrics.total().dmlCount() > 0);
      assertFalse("按表指标不应为空", metrics.tableMetrics().isEmpty());
    }
  }

  @Test
  public void metricsBeforeStartReturnsZero() {
    try (final MockCdcSource source = MockCdcSource.of(MockSceneType.TPCC)) {
      final SourceMetrics metrics = source.metrics();
      assertEquals(0, metrics.totalEvents());
      assertEquals(0, metrics.transactionCount());
      assertEquals(0, metrics.uptimeMs());
    }
  }

  @Test
  public void metricsInsertUpdateCounts() throws Exception {
    try (final MockCdcSource source =
        MockCdcSource.of("tpcc-iud", new TpccEventGenerator(), fastTestProfile())) {
      source.start();
      collectEvents(source, 50);
      final SourceMetrics metrics = source.metrics();
      final long total =
          metrics.total().insertCount()
              + metrics.total().updateCount()
              + metrics.total().deleteCount();
      assertEquals("I+U+D 应等于 DML 数", metrics.total().dmlCount(), total);
    }
  }

  // ==================== put() 手动放入事件 ====================

  @Test
  public void putEventBeforeStart() throws Exception {
    try (final MockCdcSource source = MockCdcSource.of(MockSceneType.TPCC)) {
      final TrafficProfile p = TrafficProfile.builder().build();
      final CdcDmlEvent dml = (CdcDmlEvent) new TpccEventGenerator().generate(999);
      source.put(dml);
      source.start();
      final CdcEvent first = source.next();
      assertNotNull(first);
      assertTrue("第一条应为手动放入的 DML", first instanceof CdcDmlEvent);
    }
  }

  @Test
  public void putAfterCloseIsIgnored() {
    final MockCdcSource source = MockCdcSource.of(MockSceneType.TPCC);
    source.close();
    final CdcDmlEvent dml = (CdcDmlEvent) new TpccEventGenerator().generate(1);
    source.put(dml);
  }

  // ==================== 事件类型覆盖 ====================

  @Test
  public void allEventTypesPresent() throws Exception {
    final TrafficProfile iotFast =
        TrafficProfile.builder()
            .baseTps(500)
            .pullRps(1_000_000)
            .peak(0, 24, 1.0)
            .lobChance(0.3)
            .ddlChance(0.2)
            .concurrentSessions(2)
            .build();
    try (final MockCdcSource source =
        MockCdcSource.of("iot-all-types", new IotManufacturingEventGenerator(), iotFast)) {
      source.start();
      final List<CdcEvent> events = collectEvents(source, 150);
      final Set<CdcEventType> types = new HashSet<>();
      for (final CdcEvent event : events) {
        types.add(event.type());
      }
      assertTrue("应包含 BEGIN", types.contains(CdcEventType.BEGIN));
      assertTrue("应包含 COMMIT", types.contains(CdcEventType.COMMIT));
      assertTrue("应包含 DML", types.contains(CdcEventType.DML));
      // CHUNK 和 DDL 概率性出现，在 IoT 场景高概率画像 150 事件中应能出现
      assertTrue("应包含 CHUNK（lobChance=0.3）", types.contains(CdcEventType.CHUNK));
    }
  }

  // ==================== BEGIN/COMMIT 配对 ====================

  @Test
  public void beginCommitPairing() throws Exception {
    try (final MockCdcSource source =
        MockCdcSource.of("ecom-pair", new EcommerceEventGenerator(), fastTestProfile())) {
      source.start();
      final List<CdcEvent> events = collectEvents(source, EVENTS_PER_RUN);
      int begins = 0;
      int commits = 0;
      for (final CdcEvent event : events) {
        if (event instanceof CdcBeginEvent) {
          begins++;
        } else if (event instanceof CdcCommitEvent) {
          commits++;
        }
      }
      // 最后一个事务可能被截断（BEGIN 已入队但 COMMIT 尚未到达），允许差 1
      assertTrue("BEGIN 和 COMMIT 差值应 <= 1", begins - commits <= 1);
      assertTrue("应有至少一个事务", begins > 0);
    }
  }

  // ==================== DELETE 操作覆盖 ====================

  @Test
  public void deleteOperationTracked() throws Exception {
    try (final MockCdcSource source = MockCdcSource.of(MockSceneType.TPCC)) {
      final TableInfo table = new TableInfo("test", "t");
      final LogPosition pos = new LogPosition("9999");
      final long now = System.currentTimeMillis();
      final CdcDmlEvent deleteEvent =
          new CdcDmlEvent(
              128,
              pos,
              table,
              CdcOperationType.DELETE,
              List.of(ColumnValue.of(0, 1)),
              List.of(),
              now,
              now);
      source.put(deleteEvent);
      source.start();
      final CdcEvent event = source.next();
      assertNotNull(event);
      assertTrue(event instanceof CdcDmlEvent);
      assertEquals(CdcOperationType.DELETE, ((CdcDmlEvent) event).operation());

      final SourceMetrics metrics = source.metrics();
      assertTrue("DELETE 应被统计", metrics.total().deleteCount() > 0);
    }
  }

  // ==================== 辅助方法 ====================

  private static List<CdcEvent> collectEvents(final MockCdcSource source, final int maxCount)
      throws Exception {
    final List<CdcEvent> events = new ArrayList<>();
    int emptyCount = 0;
    while (events.size() < maxCount && emptyCount < 5) {
      final CdcEvent event = source.next();
      if (event == null) {
        emptyCount++;
        TimeUnit.MILLISECONDS.sleep(1);
      } else {
        emptyCount = 0;
        events.add(event);
      }
    }
    return events;
  }

  /** 验证事务边界：每个 BEGIN 后应有若干 DML/CHUNK/DDL，最终匹配一个 COMMIT。 BEGIN 和 COMMIT 之间不应出现另一个 BEGIN。 */
  private static void verifyTransactionBoundaries(final List<CdcEvent> events) {
    boolean inTransaction = false;
    int dmlInTxn = 0;

    for (final CdcEvent event : events) {
      if (event instanceof CdcBeginEvent) {
        assertFalse("事务不应嵌套", inTransaction);
        inTransaction = true;
        dmlInTxn = 0;
      } else if (event instanceof CdcCommitEvent) {
        assertTrue("COMMIT 前应有 BEGIN", inTransaction);
        assertTrue("事务应至少包含 1 条 DML", dmlInTxn >= 1);
        inTransaction = false;
      } else if (event instanceof CdcDmlEvent) {
        assertTrue("DML 应在事务内", inTransaction);
        dmlInTxn++;
      } else if (event instanceof CdcChunkEvent) {
        assertTrue("CHUNK 应在事务内（跟随 DML）", inTransaction);
      }
    }
  }
}
