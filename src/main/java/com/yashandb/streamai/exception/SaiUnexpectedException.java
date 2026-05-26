/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** 未知/意外运行时异常。 */
public final class SaiUnexpectedException extends SaiRuntimeException {
  private static final int ERROR_CODE = ErrorCode.getErrorCode(SaiUnexpectedException.class);
  private static final String MSG = String.format("SAI-%05d Unexpected error", ERROR_CODE);

  /** 构造方法。 */
  public SaiUnexpectedException() {
    super(ExceptionHelper.format(MSG));
  }

  /** 构造方法。 */
  public SaiUnexpectedException(final String message) {
    super(ExceptionHelper.format(MSG, message));
  }

  /** 构造方法。 */
  public SaiUnexpectedException(final String message, final Throwable cause) {
    super(ExceptionHelper.format(MSG, message), cause);
  }

  /** 构造方法。 */
  public SaiUnexpectedException(final Throwable cause) {
    super(ExceptionHelper.format(MSG), cause);
  }
}
