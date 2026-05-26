/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/**
 * 错误码注册表（包级私有）。
 *
 * <p>每个异常类对应唯一错误码编号，异常类通过 {@link #getErrorCode(Class)} 静态初始化时获取自身错误码。
 */
@SuppressWarnings("java:S115")
enum ErrorCode {
  SaiConnectionFailedException(SaiConnectionFailedException.class, 1),
  SaiClosedException(SaiClosedException.class, 2),
  SaiStartFailedException(SaiStartFailedException.class, 3),
  SaiSubscribeFailedException(SaiSubscribeFailedException.class, 4),
  SaiConfigMissingException(SaiConfigMissingException.class, 5),
  SaiConfigInvalidException(SaiConfigInvalidException.class, 6),
  SaiIllegalArgumentException(SaiIllegalArgumentException.class, 7),
  SaiNotSupportException(SaiNotSupportException.class, 101),
  SaiUnexpectedException(SaiUnexpectedException.class, 102),
  ;

  private static final ErrorCode[] ERROR_CODES = values();
  private final Class<? extends Exception> clazz;
  private final int code;

  ErrorCode(final Class<? extends Exception> clazz, final int code) {
    this.clazz = clazz;
    this.code = code;
  }

  /**
   * 根据异常类获取错误码编号。
   *
   * @param e 异常类
   * @return 错误码编号
   */
  static int getErrorCode(final Class<? extends Exception> e) {
    for (final ErrorCode errorCode : ERROR_CODES) {
      if (errorCode.clazz.equals(e)) {
        return errorCode.code;
      }
    }
    throw new SaiUnexpectedException("error code not defined for " + e.getSimpleName());
  }
}
