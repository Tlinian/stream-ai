/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** 数据源订阅失败异常。 */
public final class SaiSubscribeFailedException extends SaiException {
  private static final int ERROR_CODE = ErrorCode.getErrorCode(SaiSubscribeFailedException.class);
  private static final String MSG = String.format("SAI-%05d Subscribe failed", ERROR_CODE);

  /** 构造方法。 */
  public SaiSubscribeFailedException() {
    super(ExceptionHelper.format(MSG));
  }

  /** 构造方法。 */
  public SaiSubscribeFailedException(final String message) {
    super(ExceptionHelper.format(MSG, message));
  }

  /** 构造方法。 */
  public SaiSubscribeFailedException(final String message, final Throwable cause) {
    super(ExceptionHelper.format(MSG, message), cause);
  }

  /** 构造方法。 */
  public SaiSubscribeFailedException(final Throwable cause) {
    super(ExceptionHelper.format(MSG), cause);
  }
}
