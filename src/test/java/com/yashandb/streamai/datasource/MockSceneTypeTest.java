/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** MockSceneType 枚举单元测试。 */
public class MockSceneTypeTest {

  @Test
  public void allScenesPresent() {
    assertEquals(6, MockSceneType.values().length);
  }

  @Test
  public void descriptions() {
    assertEquals("TPC-C 订单处理", MockSceneType.TPCC.description());
    assertEquals("金融实时风控", MockSceneType.FINANCE_RISK.description());
    assertEquals("电信计费对账", MockSceneType.TELECOM_RECON.description());
    assertEquals("IoT 制造质检", MockSceneType.IOT_MANUFACTURING.description());
    assertEquals("电商交易", MockSceneType.ECOMMERCE.description());
    assertEquals("金融实时行情", MockSceneType.FINANCIAL_TICK.description());
  }

  @Test
  public void valueOfRoundTrip() {
    for (final MockSceneType scene : MockSceneType.values()) {
      assertEquals(scene, MockSceneType.valueOf(scene.name()));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void valueOfInvalid() {
    MockSceneType.valueOf("NON_EXISTENT");
  }
}
