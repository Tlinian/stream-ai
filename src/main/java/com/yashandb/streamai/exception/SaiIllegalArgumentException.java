/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

/** 非法参数异常。 */
public final class SaiIllegalArgumentException extends SaiException {
  private static final int ERROR_CODE = ErrorCode.getErrorCode(SaiIllegalArgumentException.class);
  private static final String MSG = String.format("SAI-%05d Illegal argument", ERROR_CODE);

  /** 构造方法。 */
  public SaiIllegalArgumentException() {
    super(ExceptionHelper.format(MSG));
  }

  /** 构造方法。 */
  public SaiIllegalArgumentException(final String message) {
    super(ExceptionHelper.format(MSG, message));
  }

  /** 构造方法。 */
  public SaiIllegalArgumentException(final String message, final Throwable cause) {
    super(ExceptionHelper.format(MSG, message), cause);
  }

  /** 构造方法。 */
  public SaiIllegalArgumentException(final Throwable cause) {
    super(ExceptionHelper.format(MSG), cause);
  }
}
