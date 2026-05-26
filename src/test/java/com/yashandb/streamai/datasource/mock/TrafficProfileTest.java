/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import org.junit.Test;

/** TrafficProfile 单元测试。 */
public class TrafficProfileTest {

  @Test
  public void builderDefaults() {
    final TrafficProfile p = TrafficProfile.builder().build();
    assertEquals(100.0, p.baseTps(), 0.001);
    assertEquals(0.02, p.burstChance(), 0.001);
    assertEquals(10, p.burstMultiplier());
    assertEquals(0.02, p.idleChance(), 0.001);
    assertEquals(10, p.idleMaxSeconds());
    assertEquals(1, p.concurrentSessions());
    assertEquals(1, p.minDmlCount());
    assertEquals(8, p.maxDmlCount());
    assertEquals(0.0, p.lobChance(), 0.001);
    assertEquals(0.0, p.ddlChance(), 0.001);
  }

  @Test
  public void builderCustomValues() {
    final TrafficProfile p =
        TrafficProfile.builder()
            .baseTps(500)
            .burstChance(0.05)
            .burstMultiplier(20)
            .idleChance(0.03)
            .idleMaxSeconds(30)
            .concurrentSessions(4)
            .dmlCount(2, 10)
            .lobChance(0.15)
            .ddlChance(0.005)
            .build();
    assertEquals(500.0, p.baseTps(), 0.001);
    assertEquals(0.05, p.burstChance(), 0.001);
    assertEquals(20, p.burstMultiplier());
    assertEquals(0.03, p.idleChance(), 0.001);
    assertEquals(30, p.idleMaxSeconds());
    assertEquals(4, p.concurrentSessions());
    assertEquals(2, p.minDmlCount());
    assertEquals(10, p.maxDmlCount());
    assertEquals(0.15, p.lobChance(), 0.001);
    assertEquals(0.005, p.ddlChance(), 0.001);
  }

  @Test
  public void defaultHourRatesAllHalf() {
    final TrafficProfile p = TrafficProfile.builder().build();
    for (int h = 0; h < 24; h++) {
      assertEquals("小时 " + h + " 应为默认 0.5", 0.5, p.rateAt(LocalTime.of(h, 0)), 0.001);
    }
  }

  @Test
  public void peakNormalLowNoOverlap() {
    // low→normal→peak 顺序，高峰应覆盖低/常规
    final TrafficProfile p =
        TrafficProfile.builder().low(22, 7, 0.05).normal(7, 22, 0.4).peak(9, 16, 1.0).build();
    assertEquals(0.05, p.rateAt(LocalTime.of(23, 0)), 0.001);
    assertEquals(0.4, p.rateAt(LocalTime.of(8, 0)), 0.001);
    assertEquals(1.0, p.rateAt(LocalTime.of(12, 0)), 0.001);
    assertEquals(0.4, p.rateAt(LocalTime.of(20, 0)), 0.001);
  }

  @Test
  public void crossMidnightRange() {
    // 跨日范围：low(22, 7) 应覆盖 22-23 和 0-6
    final TrafficProfile p = TrafficProfile.builder().low(22, 7, 0.1).build();
    assertEquals(0.1, p.rateAt(LocalTime.of(22, 0)), 0.001);
    assertEquals(0.1, p.rateAt(LocalTime.of(23, 0)), 0.001);
    assertEquals(0.1, p.rateAt(LocalTime.of(0, 0)), 0.001);
    assertEquals(0.1, p.rateAt(LocalTime.of(6, 0)), 0.001);
    // 7 点以后恢复默认
    assertEquals(0.5, p.rateAt(LocalTime.of(7, 0)), 0.001);
  }

  @Test
  public void financeRiskPreset() {
    final TrafficProfile p = TrafficProfile.financeRisk();
    assertEquals(200.0, p.baseTps(), 0.001);
    assertEquals(5, p.concurrentSessions());
    assertEquals(2, p.minDmlCount());
    assertEquals(5, p.maxDmlCount());
    assertEquals(0.05, p.lobChance(), 0.001);
    assertEquals(0.003, p.ddlChance(), 0.001);
    // 凌晨低谷
    assertTrue(p.rateAt(LocalTime.of(3, 0)) < 0.1);
    // 白天高峰
    assertEquals(1.0, p.rateAt(LocalTime.of(12, 0)), 0.001);
  }

  @Test
  public void telecomReconPreset() {
    final TrafficProfile p = TrafficProfile.telecomRecon();
    assertEquals(500.0, p.baseTps(), 0.001);
    assertEquals(3, p.concurrentSessions());
    assertEquals(1, p.minDmlCount());
    assertEquals(3, p.maxDmlCount());
    assertEquals(0.03, p.lobChance(), 0.001);
    // 凌晨批量结算高峰
    assertTrue(p.rateAt(LocalTime.of(2, 0)) > 1.0);
  }

  @Test
  public void iotManufacturingPreset() {
    final TrafficProfile p = TrafficProfile.iotManufacturing();
    assertEquals(300.0, p.baseTps(), 0.001);
    assertEquals(10, p.concurrentSessions());
    assertEquals(0.15, p.lobChance(), 0.001);
  }

  @Test
  public void ecommercePreset() {
    final TrafficProfile p = TrafficProfile.ecommerce();
    assertEquals(150.0, p.baseTps(), 0.001);
    assertEquals(8, p.concurrentSessions());
    assertEquals(0.08, p.lobChance(), 0.001);
  }

  @Test
  public void financialTickPreset() {
    final TrafficProfile p = TrafficProfile.financialTick();
    assertEquals(1000.0, p.baseTps(), 0.001);
    assertEquals(0.0, p.lobChance(), 0.001);
    // 盘后近乎静默
    assertTrue(p.rateAt(LocalTime.of(20, 0)) < 0.1);
  }

  @Test
  public void tpccPreset() {
    final TrafficProfile p = TrafficProfile.tpcc();
    assertEquals(100.0, p.baseTps(), 0.001);
    assertEquals(5, p.minDmlCount());
    assertEquals(15, p.maxDmlCount());
    assertEquals(0.0, p.lobChance(), 0.001);
    assertEquals(0.005, p.ddlChance(), 0.001);
  }

  @Test
  public void peakOverwritesNormalAndLow() {
    // 验证 low→normal→peak 调用顺序，peak 最终生效
    final TrafficProfile p =
        TrafficProfile.builder().low(0, 24, 0.1).normal(0, 24, 0.5).peak(0, 24, 1.0).build();
    for (int h = 0; h < 24; h++) {
      assertEquals("peak 应覆盖所有", 1.0, p.rateAt(LocalTime.of(h, 0)), 0.001);
    }
  }

  @Test
  public void builderChaining() {
    // 验证所有 builder 方法可链式调用
    final TrafficProfile p =
        TrafficProfile.builder()
            .baseTps(100)
            .peak(9, 17, 1.0)
            .normal(7, 22, 0.5)
            .low(22, 7, 0.1)
            .burstChance(0.02)
            .burstMultiplier(10)
            .idleChance(0.02)
            .idleMaxSeconds(10)
            .concurrentSessions(1)
            .dmlCount(1, 5)
            .lobChance(0.1)
            .ddlChance(0.01)
            .build();
    assertNotNull(p);
    assertEquals(100.0, p.baseTps(), 0.001);
  }
}
