/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** StreamAI 基础检查型异常（sealed），所有业务检查型异常的父类。 */
public sealed class SaiException extends Exception
    permits SaiConnectionFailedException,
        SaiClosedException,
        SaiStartFailedException,
        SaiSubscribeFailedException,
        SaiConfigMissingException,
        SaiConfigInvalidException,
        SaiIllegalArgumentException {

  /** 构造方法。 */
  protected SaiException(final String message) {
    super(message);
  }

  /** 构造方法。 */
  protected SaiException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /** 构造方法。 */
  protected SaiException(
      final String message,
      final Throwable cause,
      final boolean enableSuppression,
      final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
