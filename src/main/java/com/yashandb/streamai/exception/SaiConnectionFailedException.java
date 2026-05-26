/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** 数据源连接失败异常。 */
public final class SaiConnectionFailedException extends SaiException {
  private static final int ERROR_CODE = ErrorCode.getErrorCode(SaiConnectionFailedException.class);
  private static final String MSG = String.format("SAI-%05d Connection failed", ERROR_CODE);

  /** 构造方法。 */
  public SaiConnectionFailedException() {
    super(ExceptionHelper.format(MSG));
  }

  /** 构造方法。 */
  public SaiConnectionFailedException(final String message) {
    super(ExceptionHelper.format(MSG, message));
  }

  /** 构造方法。 */
  public SaiConnectionFailedException(final String message, final Throwable cause) {
    super(ExceptionHelper.format(MSG, message), cause);
  }

  /** 构造方法。 */
  public SaiConnectionFailedException(final Throwable cause) {
    super(ExceptionHelper.format(MSG), cause);
  }
}
