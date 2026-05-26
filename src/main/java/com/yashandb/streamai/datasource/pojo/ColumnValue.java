/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.pojo;

/**
 * CDC 事件中的列值。
 *
 * @param index 列在表中的位置下标（从 0 开始）
 * @param value 列值的字符串表示，可为 null
 */
public record ColumnValue(int index, String value) {

  private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

  /**
   * 便捷构造方法，将任意类型值转为字符串。
   *
   * <p>特殊处理：{@code byte[]} 转为小写十六进制字符串（如 {@code "0a1b2c"}）。
   *
   * @param index 列下标
   * @param value 原始值
   * @return ColumnValue 实例
   */
  public static ColumnValue of(final int index, final Object value) {
    if (value == null) {
      return new ColumnValue(index, null);
    }
    if (value instanceof final byte[] b) {
      return new ColumnValue(index, bytesToHex(b));
    }
    return new ColumnValue(index, String.valueOf(value));
  }

  /** byte[] 转十六进制字符串。 */
  private static String bytesToHex(final byte[] bytes) {
    final char[] hex = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      hex[i * 2] = HEX_DIGITS[(bytes[i] >> 4) & 0x0F];
      hex[i * 2 + 1] = HEX_DIGITS[bytes[i] & 0x0F];
    }
    return new String(hex);
  }
}
