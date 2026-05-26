/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.mock;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.CdcSource;
import com.yashandb.streamai.datasource.MockSceneType;
import com.yashandb.streamai.datasource.SourceMetrics;
import com.yashandb.streamai.datasource.TableMetrics;
import com.yashandb.streamai.datasource.event.CdcBeginEvent;
import com.yashandb.streamai.datasource.event.CdcChunkEvent;
import com.yashandb.streamai.datasource.event.CdcCommitEvent;
import com.yashandb.streamai.datasource.event.CdcDdlEvent;
import com.yashandb.streamai.datasource.event.CdcDmlEvent;
import com.yashandb.streamai.datasource.pojo.CdcOperationType;
import com.yashandb.streamai.datasource.pojo.LogPosition;
import com.yashandb.streamai.datasource.pojo.TableInfo;
import com.yashandb.streamai.datasource.pojo.TransactionInfo;
import com.yashandb.streamai.exception.SaiConnectionFailedException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import lombok.extern.slf4j.Slf4j;

/**
 * Mock CDC 数据源，实现 {@link CdcSource} 接口。
 *
 * <p>核心职责：事件队列管理、生命周期控制、 流量画像驱动调度、事务包装、运行指标统计。
 *
 * <h2>真实 CDC 特征模拟</h2>
 *
 * <ul>
 *   <li><b>事务边界</b> — BEGIN → 多条 DML（可附带 CHUNK）→ COMMIT
 *   <li><b>LOB 大字段</b> — DML 涉及 BLOB/CLOB 时拆分为多个 CHUNK 分片事件
 *   <li><b>DDL 变更</b> — 事务间偶发表结构修改（ALTER TABLE 等）
 *   <li><b>高峰/低谷</b> — 24 小时流量画像，不同时段不同 TPS
 *   <li><b>突发尖刺</b> — 随机概率触发高倍率突发
 *   <li><b>空闲静默</b> — 随机概率进入长时间静默（如深夜无交易）
 *   <li><b>并发会话</b> — 多个 session 并发产生事务
 *   <li><b>消费限速</b> — next() 按画像 RPS 控制事件投递间隔
 * </ul>
 *
 * <h2>使用方式</h2>
 *
 * <pre>{@code
 * // 通过枚举创建预置场景（推荐）
 * CdcSource source = MockCdcSource.of(MockSceneType.FINANCE_RISK);
 * source.start();
 *
 * // 自定义画像
 * TrafficProfile profile = TrafficProfile.builder()
 *     .baseTps(500).peak(9, 18, 1.0).low(22, 7, 0.05).build();
 * MockCdcSource custom = MockCdcSource.of("my-scene", generator, profile);
 * custom.start();
 * }</pre>
 */
@Slf4j
public final class MockCdcSource implements CdcSource {

  private static final TrafficProfile DEFAULT_PROFILE = TrafficProfile.builder().build();

  private final String sourceId;
  private final MockEventGenerator generator;
  private final TrafficProfile profile;
  private final List<TableInfo> generatorTables;
  private final BlockingQueue<CdcEvent> queue = new LinkedBlockingQueue<>();
  private final AtomicLong positionCounter = new AtomicLong(0);
  private final AtomicLong txnCounter = new AtomicLong(0);

  // 全局聚合指标
  private final TableStats totalStats = new TableStats();
  // 事务计数
  private final AtomicLong transactionCount = new AtomicLong(0);
  // 按表维度统计
  private final ConcurrentHashMap<String, TableStats> tableStatsMap = new ConcurrentHashMap<>();
  // 事务串行锁，确保 BEGIN→DMLs→COMMIT 原子入队
  private final Object txnLock = new Object();

  private ScheduledExecutorService scheduler;
  private volatile boolean closed;
  private volatile long startTimeMs;

  // ==================== 构造与工厂方法 ====================

  private MockCdcSource(
      final String sourceId, final MockEventGenerator generator, final TrafficProfile profile) {
    this.sourceId = sourceId;
    this.generator = generator;
    this.profile = profile;
    this.generatorTables = generator.tables();
  }

  /**
   * 创建自定义场景的 Mock 数据源。
   *
   * @param sourceId 数据源标识
   * @param generator 自定义事件生成器
   * @return Mock 数据源实例
   */
  public static MockCdcSource of(final String sourceId, final MockEventGenerator generator) {
    return new MockCdcSource(sourceId, generator, DEFAULT_PROFILE);
  }

  /**
   * 创建自定义场景 + 自定义画像的 Mock 数据源。
   *
   * @param sourceId 数据源标识
   * @param generator 自定义事件生成器
   * @param profile 流量画像
   * @return Mock 数据源实例
   */
  public static MockCdcSource of(
      final String sourceId, final MockEventGenerator generator, final TrafficProfile profile) {
    return new MockCdcSource(sourceId, generator, profile);
  }

  /**
   * 根据预置场景枚举创建 Mock 数据源。
   *
   * <p>推荐使用此方法创建数据源，无需关注内部生成器和画像细节。
   *
   * @param sceneType 场景枚举
   * @return Mock 数据源实例
   */
  public static MockCdcSource of(final MockSceneType sceneType) {
    switch (sceneType) {
      case TPCC:
        return tpcc();
      case FINANCE_RISK:
        return financeRisk();
      case TELECOM_RECON:
        return telecomRecon();
      case IOT_MANUFACTURING:
        return iotManufacturing();
      case ECOMMERCE:
        return ecommerce();
      case FINANCIAL_TICK:
        return financialTick();
      default:
        throw new IllegalArgumentException("未知的场景类型: " + sceneType);
    }
  }

  /** TPC-C 订单处理场景。 */
  public static MockCdcSource tpcc() {
    return new MockCdcSource("tpcc", new TpccEventGenerator(), TrafficProfile.tpcc());
  }

  /** 金融实时风控场景。 */
  public static MockCdcSource financeRisk() {
    return new MockCdcSource(
        "finance-risk", new FinanceRiskEventGenerator(), TrafficProfile.financeRisk());
  }

  /** 电信计费对账场景。 */
  public static MockCdcSource telecomRecon() {
    return new MockCdcSource(
        "telecom-recon", new TelecomReconEventGenerator(), TrafficProfile.telecomRecon());
  }

  /** IoT 制造质检场景。 */
  public static MockCdcSource iotManufacturing() {
    return new MockCdcSource(
        "iot-manufacturing",
        new IotManufacturingEventGenerator(),
        TrafficProfile.iotManufacturing());
  }

  /** 电商交易场景。 */
  public static MockCdcSource ecommerce() {
    return new MockCdcSource(
        "ecommerce", new EcommerceEventGenerator(), TrafficProfile.ecommerce());
  }

  /** 金融实时行情场景。 */
  public static MockCdcSource financialTick() {
    return new MockCdcSource(
        "financial-tick", new FinancialTickEventGenerator(), TrafficProfile.financialTick());
  }

  // ==================== CdcSource 接口实现 ====================

  @Override
  public CdcEvent next() throws SaiConnectionFailedException {
    while (!closed) {
      try {
        final CdcEvent event = queue.poll(10, TimeUnit.MILLISECONDS);
        if (event != null) {
          // 按 pullRps 限速，模拟真实 CDC 解析 redo log 的拉取速率
          final long parkNanos = 1_000_000_000L / Math.max(1, profile.pullRps());
          LockSupport.parkNanos(parkNanos);
          return event;
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new SaiConnectionFailedException("等待事件时被中断: " + sourceId, e);
      }
    }
    return null;
  }

  @Override
  public void start() {
    if (closed) {
      LOG.warn("MockCdcSource[{}] 已关闭，无法启动", sourceId);
      return;
    }
    if (scheduler != null) {
      LOG.warn("MockCdcSource[{}] 已在运行中，忽略重复启动", sourceId);
      return;
    }
    startTimeMs = System.currentTimeMillis();
    final int sessions = Math.max(1, profile.concurrentSessions());
    scheduler = Executors.newScheduledThreadPool(sessions);
    for (int i = 0; i < sessions; i++) {
      scheduler.schedule(new SessionTask(i), 0, TimeUnit.MILLISECONDS);
    }
    LOG.info("MockCdcSource[{}] 已启动，并发会话数 {}，基础 TPS {}", sourceId, sessions, profile.baseTps());
  }

  @Override
  public SourceMetrics metrics() {
    final long uptime = startTimeMs > 0 ? System.currentTimeMillis() - startTimeMs : 0;
    final long events = totalStats.eventCount.get();
    final double rps = uptime > 0 ? events * 1000.0 / uptime : 0.0;
    final double tps = uptime > 0 ? transactionCount.get() * 1000.0 / uptime : 0.0;
    final Map<String, TableMetrics> tables = new HashMap<>();
    tableStatsMap.forEach((key, stats) -> tables.put(key, stats.toMetrics()));
    return new SourceMetrics(
        totalStats.toMetrics(),
        transactionCount.get(),
        uptime,
        Math.round(rps * 100.0) / 100.0,
        Math.round(tps * 100.0) / 100.0,
        Map.copyOf(tables));
  }

  @Override
  public void stop() {
    if (scheduler != null) {
      scheduler.shutdownNow();
      try {
        scheduler.awaitTermination(200, TimeUnit.MILLISECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      scheduler = null;
    }
    LOG.info("MockCdcSource[{}] 已停止", sourceId);
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    stop();
    LOG.info("MockCdcSource[{}] 已关闭", sourceId);
  }

  /**
   * 手动放入一条事件。
   *
   * @param event CDC 事件
   */
  public void put(final CdcEvent event) {
    if (!closed) {
      queue.offer(event);
      trackMetrics(event);
    }
  }

  // ==================== 事务生成 ====================

  /**
   * 生成一个完整事务（BEGIN → N 条 DML（可附带 CHUNK）→ COMMIT），原子入队。
   *
   * <p>通过 {@code txnLock} 保证同一事务的事件在队列中连续排列， 不会与其他并发会话的事务交错。
   *
   * <p>每条 DML 后有 {@code lobChance} 概率触发 LOB 分片，产生多条 CHUNK 事件紧随其后。
   *
   * @return 本次事务产生的 DML 事件数
   */
  private int generateTransaction() {
    if (closed) {
      return 0;
    }
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final long txnSeq = txnCounter.incrementAndGet();
    final String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
    final TransactionInfo txnInfo = new TransactionInfo(txnId, txnSeq, false);

    // 先在本地构建完整事务事件列表
    final List<CdcEvent> events = new ArrayList<>();

    // BEGIN
    final long beginPos = positionCounter.incrementAndGet();
    final LogPosition beginLogPos = new LogPosition(String.valueOf(beginPos));
    events.add(new CdcBeginEvent(64, beginLogPos, txnInfo));

    // DML 事件（根据画像配置的范围，模拟一个事务内多条操作）
    final int dmlCount = rnd.nextInt(profile.minDmlCount(), profile.maxDmlCount() + 1);
    for (int i = 0; i < dmlCount; i++) {
      final long dmlPos = positionCounter.incrementAndGet();
      final CdcEvent dmlEvent = generator.generate(dmlPos);
      events.add(dmlEvent);

      // DML 后判断是否附带 LOB 大字段分片
      if (dmlEvent instanceof CdcDmlEvent && profile.lobChance() > 0) {
        final CdcDmlEvent dml = (CdcDmlEvent) dmlEvent;
        if (rnd.nextDouble() < profile.lobChance()) {
          generateLobChunks(dml, events, rnd);
        }
      }
    }

    // COMMIT
    final long commitPos = positionCounter.incrementAndGet();
    final LogPosition commitLogPos = new LogPosition(String.valueOf(commitPos));
    events.add(new CdcCommitEvent(64, commitLogPos, txnInfo));

    // 原子入队：保证 BEGIN→DMLs→(CHUNKs)→COMMIT 在队列中连续排列
    synchronized (txnLock) {
      for (final CdcEvent event : events) {
        queue.offer(event);
        trackMetrics(event);
      }
      transactionCount.incrementAndGet();
    }

    return dmlCount;
  }

  // ==================== LOB CHUNK 生成 ====================

  /**
   * 为一条 DML 事件生成 LOB 分片事件。
   *
   * <p>模拟 Oracle XStream 行为：当 DML 包含 LOB 字段（BLOB/CLOB）时， 大字段数据拆分为多个 CHUNK 事件紧随 DML 之后传输。 每个 CHUNK
   * 携带 8KB~32KB 的模拟分片数据，最后一个 CHUNK 标记 {@code isLastChunk=true}。
   *
   * @param dml 触发 LOB 的 DML 事件
   * @param events 事务事件列表，CHUNK 追加到此列表
   * @param rnd 随机数生成器
   */
  private void generateLobChunks(
      final CdcDmlEvent dml, final List<CdcEvent> events, final ThreadLocalRandom rnd) {
    final int chunkCount = rnd.nextInt(2, 6);
    // LOB 列索引取 DML 最后一列（模拟大字段列）
    final int columnIndex =
        dml.afterColumns().isEmpty()
            ? 0
            : dml.afterColumns().get(dml.afterColumns().size() - 1).index();
    final long now = System.currentTimeMillis();

    for (int i = 0; i < chunkCount; i++) {
      final long pos = positionCounter.incrementAndGet();
      final LogPosition logPos = new LogPosition(String.valueOf(pos));
      final boolean isLast = (i == chunkCount - 1);
      final int chunkSize = rnd.nextInt(8192, 32769);
      final String data = "LOB_CHUNK_" + pos + "_" + i;

      events.add(
          new CdcChunkEvent(
              chunkSize, logPos, dml.table(), columnIndex, data, isLast, isLast, i, now, now));
    }
  }

  // ==================== DDL 生成 ====================

  /**
   * 在事务间产生一条 DDL 事件。
   *
   * <p>模拟真实 CDC 中偶发的表结构变更。DDL 生成委托给 {@link MockEventGenerator#generateDdl}， 由具体场景实现决定 DDL 模板和内容。DDL
   * 不在事务边界内，独立入队。
   */
  private void generateDdlEvent() {
    if (generatorTables.isEmpty()) {
      return;
    }
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final TableInfo table = generatorTables.get(rnd.nextInt(generatorTables.size()));
    final long pos = positionCounter.incrementAndGet();
    final CdcEvent ddlEvent = generator.generateDdl(pos, table);
    if (ddlEvent != null) {
      emitEvent(ddlEvent);
    }
  }

  /** 放入单条事件并统计（用于 DDL 等非事务事件）。 */
  private void emitEvent(final CdcEvent event) {
    synchronized (txnLock) {
      queue.offer(event);
      trackMetrics(event);
    }
  }

  // ==================== 指标统计 ====================

  /** 根据事件类型更新指标计数器。 */
  private void trackMetrics(final CdcEvent event) {
    final int byteSize = event.byteSize();
    totalStats.eventCount.incrementAndGet();
    totalStats.totalBytes.addAndGet(byteSize);
    if (event instanceof CdcDmlEvent) {
      final CdcDmlEvent dml = (CdcDmlEvent) event;
      totalStats.dmlCount.incrementAndGet();
      totalStats.trackDml(dml.operation());
      trackTableStats(dml.table(), dml.operation(), false, byteSize);
    } else if (event instanceof CdcDdlEvent) {
      final CdcDdlEvent ddl = (CdcDdlEvent) event;
      totalStats.ddlCount.incrementAndGet();
      trackTableStats(ddl.table(), null, true, byteSize);
    } else if (event instanceof CdcChunkEvent) {
      final CdcChunkEvent chunk = (CdcChunkEvent) event;
      totalStats.chunkCount.incrementAndGet();
      totalStats.chunkBytes.addAndGet(byteSize);
      trackTableStats(chunk.table(), null, false, byteSize);
    }
  }

  /** 更新表维度统计。 */
  private void trackTableStats(
      final TableInfo table, final CdcOperationType op, final boolean isDdl, final int byteSize) {
    final String key = table.schema() + "." + table.name();
    final TableStats stats = tableStatsMap.computeIfAbsent(key, k -> new TableStats());
    stats.eventCount.incrementAndGet();
    stats.totalBytes.addAndGet(byteSize);
    if (isDdl) {
      stats.ddlCount.incrementAndGet();
    }
    if (op != null) {
      stats.dmlCount.incrementAndGet();
      stats.trackDml(op);
    }
  }

  // ==================== 内部类 ====================

  /** 内部统计容器，与 {@link TableMetrics} 字段一一对应。 */
  private static final class TableStats {
    final AtomicLong eventCount = new AtomicLong(0);
    final AtomicLong dmlCount = new AtomicLong(0);
    final AtomicLong ddlCount = new AtomicLong(0);
    final AtomicLong insertCount = new AtomicLong(0);
    final AtomicLong updateCount = new AtomicLong(0);
    final AtomicLong deleteCount = new AtomicLong(0);
    final AtomicLong chunkCount = new AtomicLong(0);
    final AtomicLong totalBytes = new AtomicLong(0);
    final AtomicLong chunkBytes = new AtomicLong(0);

    void trackDml(final CdcOperationType op) {
      switch (op) {
        case INSERT:
          insertCount.incrementAndGet();
          break;
        case UPDATE:
          updateCount.incrementAndGet();
          break;
        case DELETE:
          deleteCount.incrementAndGet();
          break;
        default:
          break;
      }
    }

    TableMetrics toMetrics() {
      return new TableMetrics(
          eventCount.get(),
          dmlCount.get(),
          ddlCount.get(),
          insertCount.get(),
          updateCount.get(),
          deleteCount.get(),
          chunkCount.get(),
          totalBytes.get(),
          chunkBytes.get());
    }
  }

  /**
   * 会话任务，模拟一个数据库连接的 CDC 事件产生循环。
   *
   * <p>每次执行时先判断是否产生 DDL 事件（低概率）， 否则生成一个事务，然后根据当前流量画像决定下次调度延迟：
   *
   * <ol>
   *   <li>根据当前小时的 rate 计算实际间隔
   *   <li>随机概率触发突发（间隔缩短）
   *   <li>随机概率触发空闲（间隔拉长到秒级）
   * </ol>
   */
  private final class SessionTask implements Runnable {

    private final int sessionId;

    SessionTask(final int sessionId) {
      this.sessionId = sessionId;
    }

    @Override
    public void run() {
      if (closed || scheduler == null) {
        return;
      }
      final ThreadLocalRandom rnd = ThreadLocalRandom.current();
      final LocalTime now = LocalTime.now();
      final double rate = profile.rateAt(now);

      // 空闲判定：低谷时段有概率进入长静默
      if (rate < 0.1 && rnd.nextDouble() < profile.idleChance() * 5) {
        final long idleMs = rnd.nextLong(5000, profile.idleMaxSeconds() * 1000L + 1);
        LOG.debug("MockCdcSource[{}] 会话 {} 进入空闲期 {}ms", sourceId, sessionId, idleMs);
        if (!closed) {
          try {
            scheduler.schedule(this, idleMs, TimeUnit.MILLISECONDS);
          } catch (final java.util.concurrent.RejectedExecutionException ignored) {
            // 调度器已关闭，静默退出
          }
        }
        return;
      }

      // 随机空闲（任何时段都可能短暂停顿）
      if (rnd.nextDouble() < profile.idleChance()) {
        final long idleMs = rnd.nextLong(1000, profile.idleMaxSeconds() * 1000L + 1);
        LOG.debug("MockCdcSource[{}] 会话 {} 短暂空闲 {}ms", sourceId, sessionId, idleMs);
        if (!closed) {
          try {
            scheduler.schedule(this, idleMs, TimeUnit.MILLISECONDS);
          } catch (final java.util.concurrent.RejectedExecutionException ignored) {
            // 调度器已关闭，静默退出
          }
        }
        return;
      }

      // DDL 判定：低概率在事务间产生 DDL 事件
      if (profile.ddlChance() > 0 && rnd.nextDouble() < profile.ddlChance()) {
        generateDdlEvent();
      }

      // 生成一个事务
      generateTransaction();

      // 计算下次调度延迟
      final long delayMs = computeDelay(rnd, rate);
      LOG.trace(
          "MockCdcSource[{}] 会话 {} 下次调度延迟 {}ms (rate={})", sourceId, sessionId, delayMs, rate);
      if (!closed) {
        try {
          scheduler.schedule(this, delayMs, TimeUnit.MILLISECONDS);
        } catch (final java.util.concurrent.RejectedExecutionException ignored) {
          // 调度器已关闭，静默退出
        }
      }
    }

    private long computeDelay(final ThreadLocalRandom rnd, final double rate) {
      // 基础间隔 = (并发数 / baseTps) * 1000ms
      final double baseIntervalMs =
          profile.baseTps() > 0
              ? (profile.concurrentSessions() / profile.baseTps()) * 1000.0
              : 1000.0;
      // 根据流量倍率调整
      final double adjustedMs = rate > 0.01 ? baseIntervalMs / rate : baseIntervalMs * 10;

      // 突发判定
      if (rnd.nextDouble() < profile.burstChance()) {
        final long burstDelay = Math.max(1, (long) (adjustedMs / profile.burstMultiplier()));
        LOG.debug("MockCdcSource[{}] 会话 {} 触发突发，间隔缩短为 {}ms", sourceId, sessionId, burstDelay);
        return burstDelay;
      }

      // 正常抖动：±30%
      final double jitter = 0.7 + rnd.nextDouble() * 0.6;
      return Math.max(1, (long) (adjustedMs * jitter));
    }
  }
}
