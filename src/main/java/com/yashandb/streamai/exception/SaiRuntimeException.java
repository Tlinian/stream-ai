/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** StreamAI 基础运行时异常（sealed），所有业务运行时异常的父类。 */
public sealed class SaiRuntimeException extends RuntimeException
    permits SaiNotSupportException, SaiUnexpectedException {

  /** 构造方法。 */
  protected SaiRuntimeException(final String message) {
    super(message);
  }

  /** 构造方法。 */
  protected SaiRuntimeException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
