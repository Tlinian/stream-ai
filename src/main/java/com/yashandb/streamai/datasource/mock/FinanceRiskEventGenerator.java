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
 * 金融实时风控场景事件生成器。
 *
 * <p>对应商业企划中"金融行业 — 实时风控"场景。 模拟银行/支付机构的实时交易与风控 CDC 事件流： 每笔交易写入后触发风控规则评估，产生告警或账户余额变动。
 * 涉及表：transaction（交易流水）、risk_alert（风控告警）、 account（账户余额）。
 *
 * <p>业务痛点：每天处理百万笔交易，规则引擎报警数千次， 仅 5-20% 为真实欺诈，人工审核来不及，漏掉危险交易。
 *
 * <h2>DML 变更矩阵</h2>
 *
 * <table>
 * <caption>金融风控典型 DML 操作</caption>
 * <tr>
 *   <th>表</th><th>操作</th><th>before</th>
 *   <th>after</th><th>说明</th>
 * </tr>
 * <tr>
 *   <td>transaction</td><td>INSERT</td>
 *   <td>空</td>
 *   <td>txnId,accountId,amount,type,txnTime</td>
 *   <td>新交易流水</td>
 * </tr>
 * <tr>
 *   <td>risk_alert</td><td>INSERT</td>
 *   <td>空</td>
 *   <td>alertId,txnId,riskLevel,ruleId,alertTime</td>
 *   <td>风控规则触发告警</td>
 * </tr>
 * <tr>
 *   <td>account</td><td>UPDATE</td>
 *   <td>accountId,balance</td>
 *   <td>accountId,newBalance</td>
 *   <td>账户余额扣减</td>
 * </tr>
 * </table>
 */
public final class FinanceRiskEventGenerator implements MockEventGenerator {

  private static final String SCHEMA = "finance";
  private static final int MOCK_BYTE_SIZE = 384;

  private static final TableInfo TRANSACTION = new TableInfo(SCHEMA, "transaction");
  private static final TableInfo RISK_ALERT = new TableInfo(SCHEMA, "risk_alert");
  private static final TableInfo ACCOUNT = new TableInfo(SCHEMA, "account");

  private static final String[] DDL_TEMPLATES = {
    "ALTER TABLE %s ADD COLUMN risk_score DECIMAL(10,4) DEFAULT 0",
    "ALTER TABLE %s ADD INDEX idx_alert_time (alert_time)",
    "ALTER TABLE %s ADD COLUMN frozen_flag TINYINT DEFAULT 0",
    "CREATE INDEX idx_txn_account_time ON %s (account_id, txn_time)",
    "ALTER TABLE %s ADD COLUMN fraud_indicator VARCHAR(32)"
  };

  @Override
  public CdcEvent generateDdl(final long position, final TableInfo table) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final String template = DDL_TEMPLATES[rnd.nextInt(DDL_TEMPLATES.length)];
    final String ddl = String.format(template, table.schema() + "." + table.name());
    final long now = System.currentTimeMillis();
    return new CdcDdlEvent(128, new LogPosition(String.valueOf(position)), table, ddl, now, now);
  }

  private static final String[] TXN_TYPES = {"transfer", "payment", "withdraw", "deposit"};
  private static final String[] RISK_LEVELS = {"low", "medium", "high", "critical"};

  @Override
  public CdcEvent generate(final long position) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final long now = System.currentTimeMillis();
    final LogPosition pos = new LogPosition(String.valueOf(position));
    final long txnId = 8000000 + position;
    final long accountId = rnd.nextLong(1, 50001);

    // 交易流水 60%、风控告警 10%、账户余额 30%
    final int op = rnd.nextInt(100);
    if (op < 60) {
      return buildTransactionInsert(pos, txnId, accountId, now, rnd);
    } else if (op < 70) {
      return buildRiskAlertInsert(pos, txnId, now, rnd);
    } else {
      return buildAccountUpdate(pos, accountId, rnd);
    }
  }

  private CdcEvent buildTransactionInsert(
      final LogPosition pos,
      final long txnId,
      final long accountId,
      final long now,
      final ThreadLocalRandom rnd) {
    final double amount = rnd.nextDouble(1.0, 500000.0);
    final String type = TXN_TYPES[rnd.nextInt(TXN_TYPES.length)];
    final List<ColumnValue> after =
        List.of(
            ColumnValue.of(0, txnId),
            ColumnValue.of(1, accountId),
            ColumnValue.of(2, amount),
            ColumnValue.of(3, type),
            ColumnValue.of(4, now));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, TRANSACTION, CdcOperationType.INSERT, List.of(), after, now, now);
  }

  private CdcEvent buildRiskAlertInsert(
      final LogPosition pos, final long txnId, final long now, final ThreadLocalRandom rnd) {
    final long alertId = rnd.nextLong(1, 999999);
    final String level = RISK_LEVELS[rnd.nextInt(RISK_LEVELS.length)];
    final long ruleId = rnd.nextLong(1, 201);
    final List<ColumnValue> after =
        List.of(
            ColumnValue.of(0, alertId),
            ColumnValue.of(1, txnId),
            ColumnValue.of(2, level),
            ColumnValue.of(3, ruleId),
            ColumnValue.of(4, now));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, RISK_ALERT, CdcOperationType.INSERT, List.of(), after, now, now);
  }

  private CdcEvent buildAccountUpdate(
      final LogPosition pos, final long accountId, final ThreadLocalRandom rnd) {
    final long now = System.currentTimeMillis();
    final double oldBalance = rnd.nextDouble(1000.0, 1000000.0);
    final double delta = rnd.nextDouble(1.0, 50000.0);
    final List<ColumnValue> before =
        List.of(ColumnValue.of(0, accountId), ColumnValue.of(1, oldBalance));
    final List<ColumnValue> after =
        List.of(ColumnValue.of(0, accountId), ColumnValue.of(1, Math.max(0, oldBalance - delta)));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, ACCOUNT, CdcOperationType.UPDATE, before, after, now, now);
  }

  @Override
  public List<TableInfo> tables() {
    return List.of(TRANSACTION, RISK_ALERT, ACCOUNT);
  }
}
