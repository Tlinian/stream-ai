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
 * 电信计费对账场景事件生成器。
 *
 * <p>对应商业企划中"电信行业 — 实时对账"场景。 模拟电信运营商计费、CRM、结算等多系统之间的 数据对账 CDC 事件流。日交易数亿， 传统 T+1 对账导致差异发现滞后 24 小时。
 * 涉及表：billing_record（计费记录）、 settlement（结算单）、reconciliation_diff（对账差异）。
 *
 * <p>业务痛点：计费·CRM·结算等 6-8 个系统数据需对账， 发现计费差异时损失已发生， 如果能实时发现每年可节省 2000 万以上。
 *
 * <h2>DML 变更矩阵</h2>
 *
 * <table>
 * <caption>电信对账典型 DML 操作</caption>
 * <tr>
 *   <th>表</th><th>操作</th><th>before</th>
 *   <th>after</th><th>说明</th>
 * </tr>
 * <tr>
 *   <td>billing_record</td><td>INSERT</td>
 *   <td>空</td>
 *   <td>recordId,userId,callType,duration,amount</td>
 *   <td>新计费记录</td>
 * </tr>
 * <tr>
 *   <td>settlement</td><td>UPDATE</td>
 *   <td>settlementId,status(pending)</td>
 *   <td>settlementId,status(settled)</td>
 *   <td>结算状态变更</td>
 * </tr>
 * <tr>
 *   <td>reconciliation_diff</td><td>INSERT</td>
 *   <td>空</td>
 *   <td>diffId,billingSys,crmSys,diffAmount</td>
 *   <td>对账差异记录</td>
 * </tr>
 * </table>
 */
public final class TelecomReconEventGenerator implements MockEventGenerator {

  private static final String SCHEMA = "telecom";
  private static final int MOCK_BYTE_SIZE = 320;

  private static final TableInfo BILLING_RECORD = new TableInfo(SCHEMA, "billing_record");
  private static final TableInfo SETTLEMENT = new TableInfo(SCHEMA, "settlement");
  private static final TableInfo RECON_DIFF = new TableInfo(SCHEMA, "reconciliation_diff");

  private static final String[] DDL_TEMPLATES = {
    "ALTER TABLE %s ADD PARTITION (PARTITION p202605 VALUES LESS THAN ('2026-06-01'))",
    "ALTER TABLE %s ADD INDEX idx_billing_time (billing_time)",
    "ALTER TABLE %s ADD COLUMN audit_status VARCHAR(32) DEFAULT 'pending'",
    "ALTER TABLE %s ADD INDEX idx_diff_amount (diff_amount)",
    "ALTER TABLE %s ADD COLUMN settle_time TIMESTAMP"
  };

  @Override
  public CdcEvent generateDdl(final long position, final TableInfo table) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final String template = DDL_TEMPLATES[rnd.nextInt(DDL_TEMPLATES.length)];
    final String ddl = String.format(template, table.schema() + "." + table.name());
    final long now = System.currentTimeMillis();
    return new CdcDdlEvent(128, new LogPosition(String.valueOf(position)), table, ddl, now, now);
  }

  private static final String[] CALL_TYPES = {"voice", "data", "sms", "roaming"};
  private static final String[] SYSTEMS = {"billing", "crm", "settlement", "bss"};

  @Override
  public CdcEvent generate(final long position) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final long now = System.currentTimeMillis();
    final LogPosition pos = new LogPosition(String.valueOf(position));

    // 计费记录 70%、结算变更 20%、对账差异 10%
    final int op = rnd.nextInt(100);
    if (op < 70) {
      return buildBillingInsert(pos, now, rnd);
    } else if (op < 90) {
      return buildSettlementUpdate(pos, now, rnd);
    } else {
      return buildReconDiffInsert(pos, now, rnd);
    }
  }

  private CdcEvent buildBillingInsert(
      final LogPosition pos, final long now, final ThreadLocalRandom rnd) {
    final long recordId = rnd.nextLong(1, 9999999);
    final long userId = rnd.nextLong(1, 1000001);
    final String callType = CALL_TYPES[rnd.nextInt(CALL_TYPES.length)];
    final int duration = rnd.nextInt(1, 3601);
    final double amount = duration * rnd.nextDouble(0.01, 0.5);
    final List<ColumnValue> after =
        List.of(
            ColumnValue.of(0, recordId),
            ColumnValue.of(1, userId),
            ColumnValue.of(2, callType),
            ColumnValue.of(3, duration),
            ColumnValue.of(4, amount));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, BILLING_RECORD, CdcOperationType.INSERT, List.of(), after, now, now);
  }

  private CdcEvent buildSettlementUpdate(
      final LogPosition pos, final long now, final ThreadLocalRandom rnd) {
    final long settlementId = rnd.nextLong(1, 500001);
    final List<ColumnValue> before =
        List.of(ColumnValue.of(0, settlementId), ColumnValue.of(1, "pending"));
    final List<ColumnValue> after =
        List.of(ColumnValue.of(0, settlementId), ColumnValue.of(1, "settled"));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, SETTLEMENT, CdcOperationType.UPDATE, before, after, now, now);
  }

  private CdcEvent buildReconDiffInsert(
      final LogPosition pos, final long now, final ThreadLocalRandom rnd) {
    final long diffId = rnd.nextLong(1, 999999);
    final String sysA = SYSTEMS[rnd.nextInt(SYSTEMS.length)];
    String sysB = SYSTEMS[rnd.nextInt(SYSTEMS.length)];
    while (sysB.equals(sysA)) {
      sysB = SYSTEMS[rnd.nextInt(SYSTEMS.length)];
    }
    final double diffAmount = rnd.nextDouble(0.01, 1000.0);
    final List<ColumnValue> after =
        List.of(
            ColumnValue.of(0, diffId),
            ColumnValue.of(1, sysA),
            ColumnValue.of(2, sysB),
            ColumnValue.of(3, diffAmount));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, RECON_DIFF, CdcOperationType.INSERT, List.of(), after, now, now);
  }

  @Override
  public List<TableInfo> tables() {
    return List.of(BILLING_RECORD, SETTLEMENT, RECON_DIFF);
  }
}
