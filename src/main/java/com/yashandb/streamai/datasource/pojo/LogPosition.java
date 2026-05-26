/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.datasource.pojo;

/**
 * 日志位点，用于断点续传和检查点恢复。
 *
 * @param position 原始位点字符串（底层 CDC 引擎的序列化形式）
 */
public record LogPosition(String position) {}
