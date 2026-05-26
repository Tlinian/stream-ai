/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource;

/**
 * Mock 数据源预置场景枚举。
 *
 * <p>外部通过枚举值创建 Mock 数据源，无需关注内部生成器和流量画像细节。 每个场景自带匹配的事件生成器和 24 小时流量画像。
 *
 * <h2>使用方式</h2>
 *
 * <pre>{@code
 * CdcSource source = MockCdcSource.of(MockSceneType.FINANCE_RISK);
 * source.start();
 * while (true) {
 *   CdcEvent event = source.next();
 *   // 处理事件...
 * }
 * }</pre>
 *
 * <h2>场景说明</h2>
 *
 * <table>
 * <caption>预置场景列表</caption>
 * <tr>
 *   <th>枚举</th><th>场景</th><th>典型 TPS</th><th>流量特征</th>
 * </tr>
 * <tr>
 *   <td>TPCC</td><td>TPC-C 订单处理</td><td>~100</td>
 *   <td>工作日白天高峰，夜间低谷</td>
 * </tr>
 * <tr>
 *   <td>FINANCE_RISK</td><td>金融实时风控</td><td>~200</td>
 *   <td>交易时段高峰，凌晨极低</td>
 * </tr>
 * <tr>
 *   <td>TELECOM_RECON</td><td>电信计费对账</td><td>~500</td>
 *   <td>凌晨批量结算高峰，白天平稳</td>
 * </tr>
 * <tr>
 *   <td>IOT_MANUFACTURING</td><td>IoT 制造质检</td><td>~300</td>
 *   <td>生产班次内平稳，停机时段下降</td>
 * </tr>
 * <tr>
 *   <td>ECOMMERCE</td><td>电商交易</td><td>~150</td>
 *   <td>午间和晚间双峰，凌晨极低</td>
 * </tr>
 * <tr>
 *   <td>FINANCIAL_TICK</td><td>金融实时行情</td><td>~1000</td>
 *   <td>开盘时段极高频，盘后近乎静默</td>
 * </tr>
 * </table>
 */
public enum MockSceneType {

  /** TPC-C 订单处理场景。 */
  TPCC("TPC-C 订单处理"),

  /** 金融实时风控场景。 */
  FINANCE_RISK("金融实时风控"),

  /** 电信计费对账场景。 */
  TELECOM_RECON("电信计费对账"),

  /** IoT 制造质检场景。 */
  IOT_MANUFACTURING("IoT 制造质检"),

  /** 电商交易场景。 */
  ECOMMERCE("电商交易"),

  /** 金融实时行情场景。 */
  FINANCIAL_TICK("金融实时行情");

  private final String description;

  MockSceneType(final String description) {
    this.description = description;
  }

  /** 场景中文描述。 */
  public String description() {
    return description;
  }
}
