/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource;

import com.yashandb.streamai.exception.SaiException;

/**
 * CDC 数据源接口（拉取模式）。
 *
 * <p>调用方通过 {@link #next()} 逐条获取事件，数据源负责阻塞等待或返回下一条事件。 实现 {@link AutoCloseable} 以支持
 * try-with-resources 自动资源释放。
 *
 * <p>通过 {@link #metrics()} 可随时获取数据源运行指标， 包括 DML/DDL 事件统计、吞吐量等，用于监控和运维。
 */
public interface CdcSource extends AutoCloseable {

  /** 启动数据源，开始产生 CDC 事件流。 */
  void start();

  /**
   * 获取下一条 CDC 事件。
   *
   * <p>此方法会阻塞直到有可用事件或数据源被关闭。 数据源关闭后调用应返回 {@code null}。
   *
   * @return 下一条 CDC 事件，数据源已关闭时返回 {@code null}
   * @throws SaiException 获取事件过程中发生数据源错误
   */
  CdcEvent next() throws SaiException;

  /**
   * 获取数据源运行指标。
   *
   * <p>返回从启动到当前的累计统计信息， 包括各类型事件计数、字节总量、吞吐量等。 数据源未启动时返回 {@link SourceMetrics#EMPTY}。
   *
   * @return 当前运行指标快照
   */
  SourceMetrics metrics();

  /** 停止数据源，不再产生新的 CDC 事件。 */
  void stop();

  @Override
  void close();
}
