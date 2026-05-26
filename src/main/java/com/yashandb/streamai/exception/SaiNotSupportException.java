/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** 不支持的操作运行时异常。 */
public final class SaiNotSupportException extends SaiRuntimeException {
  private static final int ERROR_CODE = ErrorCode.getErrorCode(SaiNotSupportException.class);
  private static final String MSG = String.format("SAI-%05d Not supported", ERROR_CODE);

  /** 构造方法。 */
  public SaiNotSupportException() {
    super(ExceptionHelper.format(MSG));
  }

  /** 构造方法。 */
  public SaiNotSupportException(final String message) {
    super(ExceptionHelper.format(MSG, message));
  }

  /** 构造方法。 */
  public SaiNotSupportException(final String message, final Throwable cause) {
    super(ExceptionHelper.format(MSG, message), cause);
  }

  /** 构造方法。 */
  public SaiNotSupportException(final Throwable cause) {
    super(ExceptionHelper.format(MSG), cause);
  }
}
