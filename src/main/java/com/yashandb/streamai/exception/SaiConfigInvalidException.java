/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** 配置值无效异常。 */
public final class SaiConfigInvalidException extends SaiException {
  private static final int ERROR_CODE = ErrorCode.getErrorCode(SaiConfigInvalidException.class);
  private static final String MSG = String.format("SAI-%05d Config invalid", ERROR_CODE);

  /** 构造方法。 */
  public SaiConfigInvalidException() {
    super(ExceptionHelper.format(MSG));
  }

  /** 构造方法。 */
  public SaiConfigInvalidException(final String message) {
    super(ExceptionHelper.format(MSG, message));
  }

  /** 构造方法。 */
  public SaiConfigInvalidException(final String message, final Throwable cause) {
    super(ExceptionHelper.format(MSG, message), cause);
  }

  /** 构造方法。 */
  public SaiConfigInvalidException(final Throwable cause) {
    super(ExceptionHelper.format(MSG), cause);
  }
}
