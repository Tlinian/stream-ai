/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** 数据源启动失败异常。 */
public final class SaiStartFailedException extends SaiException {
  private static final int ERROR_CODE = ErrorCode.getErrorCode(SaiStartFailedException.class);
  private static final String MSG = String.format("SAI-%05d Start failed", ERROR_CODE);

  /** 构造方法。 */
  public SaiStartFailedException() {
    super(ExceptionHelper.format(MSG));
  }

  /** 构造方法。 */
  public SaiStartFailedException(final String message) {
    super(ExceptionHelper.format(MSG, message));
  }

  /** 构造方法。 */
  public SaiStartFailedException(final String message, final Throwable cause) {
    super(ExceptionHelper.format(MSG, message), cause);
  }

  /** 构造方法。 */
  public SaiStartFailedException(final Throwable cause) {
    super(ExceptionHelper.format(MSG), cause);
  }
}
