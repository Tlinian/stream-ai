/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** 缺少必需配置项异常。 */
public final class SaiConfigMissingException extends SaiException {
  private static final int ERROR_CODE = ErrorCode.getErrorCode(SaiConfigMissingException.class);
  private static final String MSG = String.format("SAI-%05d Config missing", ERROR_CODE);

  /** 构造方法。 */
  public SaiConfigMissingException() {
    super(ExceptionHelper.format(MSG));
  }

  /** 构造方法。 */
  public SaiConfigMissingException(final String message) {
    super(ExceptionHelper.format(MSG, message));
  }

  /** 构造方法。 */
  public SaiConfigMissingException(final String message, final Throwable cause) {
    super(ExceptionHelper.format(MSG, message), cause);
  }

  /** 构造方法。 */
  public SaiConfigMissingException(final Throwable cause) {
    super(ExceptionHelper.format(MSG), cause);
  }
}
