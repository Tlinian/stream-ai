/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.pojo;

/**
 * 事务信息。
 *
 * @param transactionId 事务 ID（xid）
 * @param transactionPart 事务分片号（非并行事务时为 {@link #NOT_PARALLEL_TRANSACTION_PART}）
 * @param isBigTransaction 是否为大事务
 */
public record TransactionInfo(
    String transactionId, long transactionPart, boolean isBigTransaction) {

  /** 非并行事务的分片标识。 */
  public static final int NOT_PARALLEL_TRANSACTION_PART = -1;
}
