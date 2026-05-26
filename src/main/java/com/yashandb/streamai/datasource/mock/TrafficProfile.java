/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.mock;

import java.time.LocalTime;

/**
 * 流量画像，模拟真实业务的 24 小时流量波动。
 *
 * <p>不同场景有不同的流量特征：
 *
 * <ul>
 *   <li>金融风控：工作日 9:00-16:00 高峰，凌晨低谷
 *   <li>电商：午间和晚间高峰，凌晨低谷
 *   <li>IoT 制造：工作日白天平稳，夜间略降
 *   <li>电信对账：全天相对平稳，凌晨有批量结算尖刺
 * </ul>
 *
 * <p>通过 {@code baseTps} 和 {@code rate} 乘数控制实际吞吐量： {@code 实际 TPS ≈ baseTps × 当前时段 rate}
 *
 * <h2>用法</h2>
 *
 * <pre>{@code
 * TrafficProfile profile = TrafficProfile.builder()
 *     .baseTps(100)
 *     .peak(9, 0, 16, 0, 1.0)
 *     .normal(7, 0, 22, 0, 0.4)
 *     .low(22, 0, 7, 0, 0.05)
 *     .burstChance(0.02)
 *     .burstMultiplier(10)
 *     .idleChance(0.05)
 *     .idleMaxSeconds(30)
 *     .build();
 * }</pre>
 */
public final class TrafficProfile {

  private static final int HOURS_IN_DAY = 24;

  private final double baseTps;
  private final double[] hourRates;
  private final double burstChance;
  private final int burstMultiplier;
  private final double idleChance;
  private final int idleMaxSeconds;
  private final int concurrentSessions;
  private final int minDmlCount;
  private final int maxDmlCount;
  private final double lobChance;
  private final double ddlChance;
  private final int pullRps;

  private TrafficProfile(final Builder builder) {
    this.baseTps = builder.baseTps;
    this.hourRates = builder.hourRates.clone();
    this.burstChance = builder.burstChance;
    this.burstMultiplier = builder.burstMultiplier;
    this.idleChance = builder.idleChance;
    this.idleMaxSeconds = builder.idleMaxSeconds;
    this.concurrentSessions = builder.concurrentSessions;
    this.minDmlCount = builder.minDmlCount;
    this.maxDmlCount = builder.maxDmlCount;
    this.lobChance = builder.lobChance;
    this.ddlChance = builder.ddlChance;
    this.pullRps = builder.pullRps;
  }

  /**
   * 获取当前时刻的流量倍率。
   *
   * @param time 当前时间
   * @return 倍率（0.0 ~ peakRate）
   */
  public double rateAt(final LocalTime time) {
    return hourRates[time.getHour()];
  }

  /** 基础 TPS。 */
  public double baseTps() {
    return baseTps;
  }

  /** 突发概率（0.0 ~ 1.0）。 */
  public double burstChance() {
    return burstChance;
  }

  /** 突发倍率。 */
  public int burstMultiplier() {
    return burstMultiplier;
  }

  /** 空闲概率（0.0 ~ 1.0）。 */
  public double idleChance() {
    return idleChance;
  }

  /** 空闲最大时长（秒）。 */
  public int idleMaxSeconds() {
    return idleMaxSeconds;
  }

  /** 并发会话数。 */
  public int concurrentSessions() {
    return concurrentSessions;
  }

  /** 每个事务最少 DML 事件数。 */
  public int minDmlCount() {
    return minDmlCount;
  }

  /** 每个事务最多 DML 事件数。 */
  public int maxDmlCount() {
    return maxDmlCount;
  }

  /** DML 事件附带 LOB 大字段的概率（0.0 ~ 1.0）。 */
  public double lobChance() {
    return lobChance;
  }

  /** 事务间产生 DDL 事件的概率（0.0 ~ 1.0）。 */
  public double ddlChance() {
    return ddlChance;
  }

  /** 拉取端最大 RPS（每秒事件数），控制 {@code next()} 的返回速度。 */
  public int pullRps() {
    return pullRps;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** 金融风控场景默认画像。 */
  public static TrafficProfile financeRisk() {
    return builder()
        .baseTps(200)
        .low(22, 7, 0.05)
        .normal(7, 22, 0.4)
        .peak(9, 16, 1.0)
        .burstChance(0.03)
        .burstMultiplier(8)
        .idleChance(0.02)
        .idleMaxSeconds(10)
        .concurrentSessions(5)
        .dmlCount(2, 5)
        .lobChance(0.05)
        .ddlChance(0.003)
        .build();
  }

  /** 电信对账场景默认画像。 */
  public static TrafficProfile telecomRecon() {
    return builder()
        .baseTps(500)
        .low(22, 0, 0.3)
        .normal(6, 22, 0.6)
        .peak(0, 6, 1.5)
        .burstChance(0.05)
        .burstMultiplier(20)
        .idleChance(0.01)
        .idleMaxSeconds(5)
        .concurrentSessions(3)
        .dmlCount(1, 3)
        .lobChance(0.03)
        .ddlChance(0.002)
        .build();
  }

  /** IoT 制造质检场景默认画像。 */
  public static TrafficProfile iotManufacturing() {
    return builder()
        .baseTps(300)
        .low(22, 6, 0.2)
        .normal(6, 22, 0.5)
        .peak(8, 18, 0.9)
        .burstChance(0.01)
        .burstMultiplier(5)
        .idleChance(0.03)
        .idleMaxSeconds(15)
        .concurrentSessions(10)
        .dmlCount(1, 6)
        .lobChance(0.15)
        .ddlChance(0.001)
        .build();
  }

  /** 电商交易场景默认画像。 */
  public static TrafficProfile ecommerce() {
    return builder()
        .baseTps(150)
        .low(23, 8, 0.08)
        .normal(8, 23, 0.5)
        .peak(10, 14, 1.0)
        .burstChance(0.04)
        .burstMultiplier(15)
        .idleChance(0.03)
        .idleMaxSeconds(20)
        .concurrentSessions(8)
        .dmlCount(3, 6)
        .lobChance(0.08)
        .ddlChance(0.002)
        .build();
  }

  /** 金融行情场景默认画像。 */
  public static TrafficProfile financialTick() {
    return builder()
        .baseTps(1000)
        .low(16, 8, 0.02)
        .normal(8, 16, 0.3)
        .peak(9, 15, 1.0)
        .burstChance(0.08)
        .burstMultiplier(30)
        .idleChance(0.01)
        .idleMaxSeconds(5)
        .concurrentSessions(2)
        .dmlCount(1, 2)
        .lobChance(0.0)
        .ddlChance(0.001)
        .build();
  }

  /** TPC-C 场景默认画像。 */
  public static TrafficProfile tpcc() {
    return builder()
        .baseTps(100)
        .low(22, 7, 0.1)
        .normal(7, 22, 0.5)
        .peak(9, 18, 1.0)
        .burstChance(0.02)
        .burstMultiplier(10)
        .idleChance(0.02)
        .idleMaxSeconds(10)
        .concurrentSessions(4)
        .dmlCount(5, 15)
        .lobChance(0.0)
        .ddlChance(0.005)
        .build();
  }

  /** 流量画像构建器。 */
  public static final class Builder {

    private double baseTps = 100;
    private final double[] hourRates = new double[HOURS_IN_DAY];
    private double burstChance = 0.02;
    private int burstMultiplier = 10;
    private double idleChance = 0.02;
    private int idleMaxSeconds = 10;
    private int concurrentSessions = 1;
    private int minDmlCount = 1;
    private int maxDmlCount = 8;
    private double lobChance = 0.0;
    private double ddlChance = 0.0;
    private int pullRps = 300_000;

    private Builder() {
      for (int i = 0; i < HOURS_IN_DAY; i++) {
        hourRates[i] = 0.5;
      }
    }

    /** 基础 TPS。 */
    public Builder baseTps(final double tps) {
      this.baseTps = tps;
      return this;
    }

    /**
     * 设置高峰期时段和倍率。
     *
     * @param startHour 开始小时（含）
     * @param endHour 结束小时（不含，跨日自动处理）
     * @param rate 倍率
     */
    public Builder peak(final int startHour, final int endHour, final double rate) {
      fillRates(startHour, endHour, rate);
      return this;
    }

    /** 设置常规期时段和倍率。 */
    public Builder normal(final int startHour, final int endHour, final double rate) {
      fillRates(startHour, endHour, rate);
      return this;
    }

    /** 设置低谷期时段和倍率。 */
    public Builder low(final int startHour, final int endHour, final double rate) {
      fillRates(startHour, endHour, rate);
      return this;
    }

    /** 突发概率。 */
    public Builder burstChance(final double chance) {
      this.burstChance = chance;
      return this;
    }

    /** 突发倍率。 */
    public Builder burstMultiplier(final int multiplier) {
      this.burstMultiplier = multiplier;
      return this;
    }

    /** 空闲概率。 */
    public Builder idleChance(final double chance) {
      this.idleChance = chance;
      return this;
    }

    /** 空闲最大时长（秒）。 */
    public Builder idleMaxSeconds(final int seconds) {
      this.idleMaxSeconds = seconds;
      return this;
    }

    /** 并发会话数。 */
    public Builder concurrentSessions(final int sessions) {
      this.concurrentSessions = sessions;
      return this;
    }

    /**
     * 每个事务的 DML 事件数范围。
     *
     * @param min 最少 DML 事件数（含）
     * @param max 最多 DML 事件数（含）
     */
    public Builder dmlCount(final int min, final int max) {
      this.minDmlCount = min;
      this.maxDmlCount = max;
      return this;
    }

    /**
     * DML 事件附带 LOB 大字段的概率。
     *
     * <p>真实 CDC 中，当 DML 涉及 BLOB/CLOB 字段时，大字段数据会拆分为多个 CHUNK 事件 紧随 DML 之后传输。此概率控制每条 DML 触发 LOB
     * 分片的可能性。
     *
     * @param chance 概率（0.0 ~ 1.0）
     */
    public Builder lobChance(final double chance) {
      this.lobChance = chance;
      return this;
    }

    /**
     * 事务间产生 DDL 事件的概率。
     *
     * <p>真实 CDC 中，DDL（如 ALTER TABLE）偶发出现在事务之间， 会导致隐式提交当前事务。此概率控制每次调度时产生 DDL 的可能性。
     *
     * @param chance 概率（0.0 ~ 1.0）
     */
    public Builder ddlChance(final double chance) {
      this.ddlChance = chance;
      return this;
    }

    /**
     * 拉取端最大 RPS（每秒事件数）。
     *
     * <p>控制 {@code next()} 返回速度，模拟真实 CDC 解析 redo log 的吞吐上限。 默认 300,000。
     */
    public Builder pullRps(final int rps) {
      this.pullRps = rps;
      return this;
    }

    public TrafficProfile build() {
      return new TrafficProfile(this);
    }

    private void fillRates(final int start, final int end, final double rate) {
      if (start < end) {
        for (int h = start; h < end; h++) {
          hourRates[h] = rate;
        }
      } else {
        for (int h = start; h < HOURS_IN_DAY; h++) {
          hourRates[h] = rate;
        }
        for (int h = 0; h < end; h++) {
          hourRates[h] = rate;
        }
      }
    }
  }
}
