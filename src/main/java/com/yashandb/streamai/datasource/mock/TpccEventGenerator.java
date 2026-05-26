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
 * TPC-C 订单处理场景事件生成器。
 *
 * <p>TPC-C 是 TPC 组织发布的 OLTP 基准测试，模拟一个批发商的订单处理系统。 包含仓库（warehouse）、地区（district）、客户（customer）、
 * 订单（orders）、订单项（order_line）、库存（stock）等核心表。
 *
 * <h2>DML 变更矩阵</h2>
 *
 * <table>
 * <caption>TPC-C 典型 DML 操作</caption>
 * <tr>
 *   <th>表</th><th>操作</th><th>before</th>
 *   <th>after</th><th>说明</th>
 * </tr>
 * <tr>
 *   <td>orders</td><td>INSERT</td><td>空</td>
 *   <td>orderId,districtId,warehouseId,customerId,orderTime,itemCount</td>
 *   <td>新订单</td>
 * </tr>
 * <tr>
 *   <td>order_line</td><td>INSERT</td><td>空</td>
 *   <td>orderId,districtId,warehouseId,lineNumber,itemId,amount</td>
 *   <td>订单明细</td>
 * </tr>
 * <tr>
 *   <td>stock</td><td>UPDATE</td>
 *   <td>itemId,warehouseId,quantity</td>
 *   <td>itemId,warehouseId,quantity-1</td>
 *   <td>库存扣减</td>
 * </tr>
 * </table>
 */
public final class TpccEventGenerator implements MockEventGenerator {

  private static final String SCHEMA = "tpcc";
  private static final int MOCK_BYTE_SIZE = 256;

  private static final TableInfo ORDERS = new TableInfo(SCHEMA, "orders");
  private static final TableInfo ORDER_LINE = new TableInfo(SCHEMA, "order_line");
  private static final TableInfo STOCK = new TableInfo(SCHEMA, "stock");

  private static final String[] DDL_TEMPLATES = {
    "ALTER TABLE %s ADD INDEX idx_customer_id (customer_id)",
    "ALTER TABLE %s ADD COLUMN priority VARCHAR(16) DEFAULT 'normal'",
    "ALTER TABLE %s ADD INDEX idx_warehouse_district (warehouse_id, district_id)",
    "ALTER TABLE %s ADD COLUMN reorder_point INT DEFAULT 100",
    "ALTER TABLE %s ADD COLUMN discount DECIMAL(5,2) DEFAULT 0"
  };

  @Override
  public CdcEvent generateDdl(final long position, final TableInfo table) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final String template = DDL_TEMPLATES[rnd.nextInt(DDL_TEMPLATES.length)];
    final String ddl = String.format(template, table.schema() + "." + table.name());
    final long now = System.currentTimeMillis();
    return new CdcDdlEvent(128, new LogPosition(String.valueOf(position)), table, ddl, now, now);
  }

  @Override
  public CdcEvent generate(final long position) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final long now = System.currentTimeMillis();
    final LogPosition pos = new LogPosition(String.valueOf(position));
    final long orderId = 100000 + position;
    final int warehouseId = rnd.nextInt(1, 11);
    final int districtId = rnd.nextInt(1, 11);
    final int customerId = rnd.nextInt(1, 3001);

    // 新订单 40%、订单明细 30%、库存扣减 30%
    final int op = rnd.nextInt(100);
    if (op < 40) {
      // 新订单 - INSERT orders
      final List<ColumnValue> after =
          List.of(
              ColumnValue.of(0, orderId),
              ColumnValue.of(1, districtId),
              ColumnValue.of(2, warehouseId),
              ColumnValue.of(3, customerId),
              ColumnValue.of(4, now),
              ColumnValue.of(5, rnd.nextInt(5, 16)));
      return new CdcDmlEvent(
          MOCK_BYTE_SIZE, pos, ORDERS, CdcOperationType.INSERT, List.of(), after, now, now);
    } else if (op < 70) {
      // 订单明细 - INSERT order_line
      final List<ColumnValue> after =
          List.of(
              ColumnValue.of(0, orderId),
              ColumnValue.of(1, districtId),
              ColumnValue.of(2, warehouseId),
              ColumnValue.of(3, rnd.nextInt(1, 16)),
              ColumnValue.of(4, rnd.nextInt(1, 100001)),
              ColumnValue.of(5, rnd.nextDouble(1.0, 100.0)));
      return new CdcDmlEvent(
          MOCK_BYTE_SIZE, pos, ORDER_LINE, CdcOperationType.INSERT, List.of(), after, now, now);
    } else {
      // 库存扣减 - UPDATE stock（主键列保持不变，quantity 减 1）
      final int itemId = rnd.nextInt(1, 100001);
      final int quantity = rnd.nextInt(100, 5000);
      final List<ColumnValue> before =
          List.of(
              ColumnValue.of(0, itemId),
              ColumnValue.of(1, warehouseId),
              ColumnValue.of(2, quantity));
      final List<ColumnValue> after =
          List.of(
              ColumnValue.of(0, itemId),
              ColumnValue.of(1, warehouseId),
              ColumnValue.of(2, quantity - 1));
      return new CdcDmlEvent(
          MOCK_BYTE_SIZE, pos, STOCK, CdcOperationType.UPDATE, before, after, now, now);
    }
  }

  @Override
  public List<TableInfo> tables() {
    return List.of(ORDERS, ORDER_LINE, STOCK);
  }
}
