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
 * IoT 制造质检场景事件生成器。
 *
 * <p>对应商业企划中"制造业 — 传感器质检"场景。 模拟制造企业产线上传感器数据采集与质量检验的 CDC 事件流。传感器每天产生 TB 级数据， 传统阈值预警只能覆盖部分质量问题。
 * 涉及表：sensor_reading（传感器读数）、 quality_inspection（质检结果）、 equipment_status（设备状态）。
 *
 * <p>业务痛点：传感器数据量大、异常发现延迟， 阈值预警无法识别新型异常模式， 异常发现慢导致设备停机。
 *
 * <h2>DML 变更矩阵</h2>
 *
 * <table>
 * <caption>IoT 制造质检典型 DML 操作</caption>
 * <tr>
 *   <th>表</th><th>操作</th><th>before</th>
 *   <th>after</th><th>说明</th>
 * </tr>
 * <tr>
 *   <td>sensor_reading</td><td>INSERT</td>
 *   <td>空</td>
 *   <td>deviceId,metric,value,readAt</td>
 *   <td>传感器读数上报</td>
 * </tr>
 * <tr>
 *   <td>quality_inspection</td><td>INSERT</td>
 *   <td>空</td>
 *   <td>inspectionId,productId,result,defect</td>
 *   <td>质检结果记录</td>
 * </tr>
 * <tr>
 *   <td>equipment_status</td><td>UPDATE</td>
 *   <td>equipmentId,status</td>
 *   <td>equipmentId,newStatus</td>
 *   <td>设备状态变更</td>
 * </tr>
 * </table>
 */
public final class IotManufacturingEventGenerator implements MockEventGenerator {

  private static final String SCHEMA = "manufacturing";
  private static final int MOCK_BYTE_SIZE = 256;

  private static final TableInfo SENSOR = new TableInfo(SCHEMA, "sensor_reading");
  private static final TableInfo INSPECTION = new TableInfo(SCHEMA, "quality_inspection");
  private static final TableInfo EQUIPMENT = new TableInfo(SCHEMA, "equipment_status");

  private static final String[] DDL_TEMPLATES = {
    "ALTER TABLE %s ADD PARTITION (PARTITION p202605 VALUES LESS THAN ('2026-06-01'))",
    "ALTER TABLE %s ADD INDEX idx_equipment_time (equipment_id, read_time)",
    "ALTER TABLE %s ADD COLUMN anomaly_score DECIMAL(5,4) DEFAULT 0",
    "ALTER TABLE %s ADD COLUMN last_maintenance TIMESTAMP",
    "CREATE INDEX idx_inspection_product ON %s (product_id, inspection_time)"
  };

  @Override
  public CdcEvent generateDdl(final long position, final TableInfo table) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final String template = DDL_TEMPLATES[rnd.nextInt(DDL_TEMPLATES.length)];
    final String ddl = String.format(template, table.schema() + "." + table.name());
    final long now = System.currentTimeMillis();
    return new CdcDdlEvent(128, new LogPosition(String.valueOf(position)), table, ddl, now, now);
  }

  private static final String[] METRICS = {"temperature", "vibration", "pressure", "current"};
  private static final String[] RESULTS = {"pass", "fail", "rework"};
  private static final String[] DEFECTS = {"crack", "misalign", "overheat", "none"};
  private static final String[] EQ_STATUS = {"running", "idle", "maintenance", "fault"};

  @Override
  public CdcEvent generate(final long position) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final long now = System.currentTimeMillis();
    final LogPosition pos = new LogPosition(String.valueOf(position));

    // 传感器读数 80%、质检结果 15%、设备状态 5%
    final int op = rnd.nextInt(100);
    if (op < 80) {
      return buildSensorInsert(pos, now, rnd);
    } else if (op < 95) {
      return buildInspectionInsert(pos, now, rnd);
    } else {
      return buildEquipmentUpdate(pos, now, rnd);
    }
  }

  private CdcEvent buildSensorInsert(
      final LogPosition pos, final long now, final ThreadLocalRandom rnd) {
    final String deviceId = "EQ-" + rnd.nextInt(1, 501);
    final String metric = METRICS[rnd.nextInt(METRICS.length)];
    final double value = rnd.nextDouble(-20.0, 300.0);
    final List<ColumnValue> after =
        List.of(
            ColumnValue.of(0, deviceId),
            ColumnValue.of(1, metric),
            ColumnValue.of(2, value),
            ColumnValue.of(3, now));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, SENSOR, CdcOperationType.INSERT, List.of(), after, now, now);
  }

  private CdcEvent buildInspectionInsert(
      final LogPosition pos, final long now, final ThreadLocalRandom rnd) {
    final long inspectionId = rnd.nextLong(1, 999999);
    final String productId = "PRD-" + rnd.nextInt(1, 100001);
    final String result = RESULTS[rnd.nextInt(RESULTS.length)];
    final String defect = "fail".equals(result) ? DEFECTS[rnd.nextInt(3)] : "none";
    final List<ColumnValue> after =
        List.of(
            ColumnValue.of(0, inspectionId),
            ColumnValue.of(1, productId),
            ColumnValue.of(2, result),
            ColumnValue.of(3, defect));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, INSPECTION, CdcOperationType.INSERT, List.of(), after, now, now);
  }

  private CdcEvent buildEquipmentUpdate(
      final LogPosition pos, final long now, final ThreadLocalRandom rnd) {
    final String equipmentId = "EQ-" + rnd.nextInt(1, 501);
    final int oldIdx = rnd.nextInt(EQ_STATUS.length);
    int newIdx = rnd.nextInt(EQ_STATUS.length);
    while (newIdx == oldIdx) {
      newIdx = rnd.nextInt(EQ_STATUS.length);
    }
    final List<ColumnValue> before =
        List.of(ColumnValue.of(0, equipmentId), ColumnValue.of(1, EQ_STATUS[oldIdx]));
    final List<ColumnValue> after =
        List.of(ColumnValue.of(0, equipmentId), ColumnValue.of(1, EQ_STATUS[newIdx]));
    return new CdcDmlEvent(
        MOCK_BYTE_SIZE, pos, EQUIPMENT, CdcOperationType.UPDATE, before, after, now, now);
  }

  @Override
  public List<TableInfo> tables() {
    return List.of(SENSOR, INSPECTION, EQUIPMENT);
  }
}
