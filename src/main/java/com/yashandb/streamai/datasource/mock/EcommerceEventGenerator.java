/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.mock;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.event.CdcDdlEvent;
import com.yashandb.streamai.datasource.event.CdcDmlEvent;
import com.yashandb.streamai.datasource.pojo.CdcOperationType;
import com.yashandb.streamai.datasource.pojo.ColumnValue;
import com.yashandb.streamai.datasource.pojo.LogPosition;
import com.yashandb.streamai.datasource.pojo.TableInfo;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 电商交易场景事件生成器。
 *
 * <p>对应商业企划中"企业私有数据"场景。 模拟电商平台的订单、支付、库存核心交易链路 CDC 事件流。涵盖客户数据、供应链数据等 企业私有数据的实时变更。
 * 涉及表：orders（订单）、payments（支付）、 inventory（库存）。
 *
 * <p>业务场景：订单创建、支付完成、库存扣减， 构成电商核心交易闭环， 实时感知业务数据脉搏。
 *
 * <h2>DML 变更矩阵</h2>
 *
 * <table>
 * <caption>电商交易典型 DML 操作</caption>
 * <tr>
 *   <th>表</th><th>操作</th><th>before</th>
 *   <th>after</th><th>说明</th>
 * </tr>
 * <tr>
 *   <td>orders</td><td>INSERT</td>
 *   <td>空</td>
 *   <td>orderId,userId,status,amount,createdAt</td>
 *   <td>新订单创建</td>
 * </tr>
 * <tr>
 *   <td>payments</td><td>INSERT</td>
 *   <td>空</td>
 *   <td>payId,orderId,amount,channel,paidAt</td>
 *   <td>支付记录</td>
 * </tr>
 * <tr>
 *   <td>inventory</td><td>UPDATE</td>
 *   <td>skuId,quantity</td>
 *   <td>skuId,newQuantity</td>
 *   <td>库存扣减</td>
 * </tr>
 * </table>
 */
public final class EcommerceEventGenerator implements MockEventGenerator {

  private static final String SCHEMA = "ecommerce";
  private static final int MOCK_BYTE_SIZE = 320;

  private static final TableInfo ORDERS = new TableInfo(SCHEMA, "orders");
  private static final TableInfo PAYMENTS = new TableInfo(SCHEMA, "payments");
  private static final TableInfo INVENTORY = new TableInfo(SCHEMA, "inventory");

  private static final String[] DDL_TEMPLATES = {
    "ALTER TABLE %s ADD INDEX idx_user_time (user_id, created_at)",
    "ALTER TABLE %s ADD COLUMN shipping_address VARCHAR(512)",
    "ALTER TABLE %s ADD COLUMN risk_level VARCHAR(16)",
    "ALTER TABLE %s ADD INDEX idx_order_amount (order_id, amount)",
    "ALTER TABLE %s ADD COLUMN warehouse_location VARCHAR(64)"
  };

  @Override
  public CdcEvent generateDdl(final long position, final TableInfo table) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final String template = DDL_TEMPLATES[rnd.nextInt(DDL_TEMPLATES.length)];
    final String ddl = String.format(template, table.schema() + "." + table.name());
    final long now = System.currentTimeMillis();
    return new CdcDdlEvent(128, new LogPosition(String.valueOf(position)), table, ddl, now, now);
  }

  private static final String[] CHANNELS = {"alipay", "wechat", "unionpay", "credit_card"};

  @Override
  public CdcEvent generate(final long position) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final long now = System.currentTimeMillis();
    final LogPosition pos = new LogPosition(String.valueOf(position));

    // 订单 40%、支付 35%、库存 25%
    final int op = rnd.nextInt(100);
    if (op < 40) {
      return buildOrderInsert(pos, position, now, rnd);
    } else if (op < 75) {
      return buildPaymentInsert(pos, position, now, rnd);
    } else {
      return buildInventoryUpdate(pos, now, rnd);
    }
  }

  private CdcEvent buildOrderInsert(
      final LogPosition pos, final long position, final long now, final ThreadLocalRandom rnd) {
    final long orderId = 20000000 + position;
    final long userId = rnd.nextLong(1, 100001);
    final double amount = rnd.nextDouble(10.0, 5000.0);
    final List<ColumnValue> after =
        List.of(
            ColumnValue.of(0, orderId),
            ColumnValue.of(1, userId),
            ColumnValue.of(2, "pending"),
            ColumnValue.of(3, amount),
            ColumnValue.of(4, now));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, ORDERS, CdcOperationType.INSERT, List.of(), after, now, now);
  }

  private CdcEvent buildPaymentInsert(
      final LogPosition pos, final long position, final long now, final ThreadLocalRandom rnd) {
    final long payId = rnd.nextLong(100000, 999999);
    final long orderId = 20000000 + position;
    final double amount = rnd.nextDouble(10.0, 5000.0);
    final String channel = CHANNELS[rnd.nextInt(CHANNELS.length)];
    final List<ColumnValue> after =
        List.of(
            ColumnValue.of(0, payId),
            ColumnValue.of(1, orderId),
            ColumnValue.of(2, amount),
            ColumnValue.of(3, channel),
            ColumnValue.of(4, now));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, PAYMENTS, CdcOperationType.INSERT, List.of(), after, now, now);
  }

  private CdcEvent buildInventoryUpdate(
      final LogPosition pos, final long now, final ThreadLocalRandom rnd) {
    final long skuId = rnd.nextLong(1, 50001);
    final int oldQty = rnd.nextInt(50, 1000);
    final int delta = rnd.nextInt(1, 10);
    final List<ColumnValue> before = List.of(ColumnValue.of(0, skuId), ColumnValue.of(1, oldQty));
    final List<ColumnValue> after =
        List.of(ColumnValue.of(0, skuId), ColumnValue.of(1, Math.max(0, oldQty - delta)));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, INVENTORY, CdcOperationType.UPDATE, before, after, now, now);
  }

  @Override
  public List<TableInfo> tables() {
    return List.of(ORDERS, PAYMENTS, INVENTORY);
  }
}
