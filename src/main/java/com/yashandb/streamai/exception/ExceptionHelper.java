/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** 异常消息格式化工具。 */
final class ExceptionHelper {
  private ExceptionHelper() {}

  static String format(final String msg0) {
    return msg0 + ".";
  }

  static String format(final String msg0, final String msg1) {
    return msg0 + ": " + msg1;
  }
}
