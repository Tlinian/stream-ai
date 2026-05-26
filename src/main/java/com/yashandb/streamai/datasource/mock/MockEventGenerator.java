/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.mock;

import com.yashandb.streamai.datasource.CdcEvent;
import com.yashandb.streamai.datasource.pojo.TableInfo;
import java.util.List;

/** Mock 事件生成器接口，定义不同业务场景的事件生成策略。 */
public interface MockEventGenerator {

  /**
   * 生成下一条 CDC 事件。
   *
   * @param position 当前位点序号
   * @return 生成的 CDC 事件
   */
  CdcEvent generate(long position);

  /**
   * 返回该生成器涉及的表列表。
   *
   * <p>用于 MockCdcSource 在产生 DDL 事件时随机选取目标表。
   *
   * @return 表列表，不应为空
   */
  default List<TableInfo> tables() {
    return List.of();
  }

  /**
   * 生成一条 DDL 事件。
   *
   * <p>各场景生成器须实现此方法，提供与业务模型匹配的 DDL 模板（如分区操作、索引创建、字段增删等）。
   *
   * @param position 当前位点序号
   * @param table 目标表（由 MockCdcSource 从 {@link #tables()} 中随机选取）
   * @return DDL 事件
   */
  CdcEvent generateDdl(long position, TableInfo table);
}
