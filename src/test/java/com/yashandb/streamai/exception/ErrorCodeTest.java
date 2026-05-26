/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** ErrorCode 枚举单元测试。 */
public class ErrorCodeTest {

  @Test
  public void allErrorCodesPresent() {
    assertEquals(9, ErrorCode.values().length);
  }

  @Test
  public void errorCodeForConnectionFailed() {
    final int code = ErrorCode.getErrorCode(SaiConnectionFailedException.class);
    assertEquals(1, code);
  }

  @Test
  public void errorCodeForClosed() {
    assertEquals(2, ErrorCode.getErrorCode(SaiClosedException.class));
  }

  @Test
  public void errorCodeForStartFailed() {
    assertEquals(3, ErrorCode.getErrorCode(SaiStartFailedException.class));
  }

  @Test
  public void errorCodeForSubscribeFailed() {
    assertEquals(4, ErrorCode.getErrorCode(SaiSubscribeFailedException.class));
  }

  @Test
  public void errorCodeForConfigMissing() {
    assertEquals(5, ErrorCode.getErrorCode(SaiConfigMissingException.class));
  }

  @Test
  public void errorCodeForConfigInvalid() {
    assertEquals(6, ErrorCode.getErrorCode(SaiConfigInvalidException.class));
  }

  @Test
  public void errorCodeForIllegalArg() {
    assertEquals(7, ErrorCode.getErrorCode(SaiIllegalArgumentException.class));
  }

  @Test
  public void errorCodeForNotSupport() {
    assertEquals(101, ErrorCode.getErrorCode(SaiNotSupportException.class));
  }

  @Test
  public void errorCodeForUnexpected() {
    assertEquals(102, ErrorCode.getErrorCode(SaiUnexpectedException.class));
  }

  @Test(expected = SaiUnexpectedException.class)
  public void getErrorCodeForUnknownClassThrows() {
    ErrorCode.getErrorCode(Exception.class);
  }

  @Test
  public void errorCodeFormat5Digits() {
    // 每个异常的消息应包含 SAI-XXXXX 格式的5位错误码
    final SaiConnectionFailedException ex1 = new SaiConnectionFailedException();
    assertTrue(ex1.getMessage().contains("SAI-00001"));

    final SaiNotSupportException ex2 = new SaiNotSupportException();
    assertTrue(ex2.getMessage().contains("SAI-00101"));

    final SaiUnexpectedException ex3 = new SaiUnexpectedException();
    assertTrue(ex3.getMessage().contains("SAI-00102"));
  }

  @Test
  public void checkedExceptionsHaveLowCodes() {
    // 检查型异常消息中包含 SAI-00001 ~ SAI-00007
    assertTrue(new SaiConnectionFailedException().getMessage().contains("SAI-00001"));
    assertTrue(new SaiClosedException().getMessage().contains("SAI-00002"));
    assertTrue(new SaiStartFailedException().getMessage().contains("SAI-00003"));
    assertTrue(new SaiSubscribeFailedException().getMessage().contains("SAI-00004"));
    assertTrue(new SaiConfigMissingException().getMessage().contains("SAI-00005"));
    assertTrue(new SaiConfigInvalidException().getMessage().contains("SAI-00006"));
    assertTrue(new SaiIllegalArgumentException().getMessage().contains("SAI-00007"));
  }

  @Test
  public void runtimeExceptionsHaveHighCodes() {
    // 运行时异常消息中包含 SAI-00101 ~ SAI-00102
    assertTrue(new SaiNotSupportException().getMessage().contains("SAI-00101"));
    assertTrue(new SaiUnexpectedException().getMessage().contains("SAI-00102"));
  }
}
