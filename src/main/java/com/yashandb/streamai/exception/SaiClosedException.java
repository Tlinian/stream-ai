/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** 数据源已关闭异常。 */
public final class SaiClosedException extends SaiException {
  private static final int ERROR_CODE = ErrorCode.getErrorCode(SaiClosedException.class);
  private static final String MSG = String.format("SAI-%05d Source closed", ERROR_CODE);

  /** 构造方法。 */
  public SaiClosedException() {
    super(ExceptionHelper.format(MSG));
  }

  /** 构造方法。 */
  public SaiClosedException(final String message) {
    super(ExceptionHelper.format(MSG, message));
  }

  /** 构造方法。 */
  public SaiClosedException(final String message, final Throwable cause) {
    super(ExceptionHelper.format(MSG, message), cause);
  }

  /** 构造方法。 */
  public SaiClosedException(final Throwable cause) {
    super(ExceptionHelper.format(MSG), cause);
  }
}
