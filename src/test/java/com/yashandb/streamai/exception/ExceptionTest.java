/*
  Copyright (c) 2026, YashanDB Development Group.
*/

package com.yashandb.streamai.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** 异常体系单元测试。 */
public class ExceptionTest {

  // ==================== SaiConnectionFailedException ====================

  @Test
  public void connectionFailedNoArg() {
    final SaiConnectionFailedException ex = new SaiConnectionFailedException();
    assertTrue(ex.getMessage().contains("SAI-00001"));
    assertTrue(ex.getMessage().contains("Connection failed"));
  }

  @Test
  public void connectionFailedWithMessage() {
    final SaiConnectionFailedException ex = new SaiConnectionFailedException("192.168.1.1:3306");
    assertTrue(ex.getMessage().contains("SAI-00001"));
    assertTrue(ex.getMessage().contains("192.168.1.1:3306"));
  }

  @Test
  public void connectionFailedWithCause() {
    final RuntimeException cause = new RuntimeException("timeout");
    final SaiConnectionFailedException ex = new SaiConnectionFailedException("host", cause);
    assertEquals(cause, ex.getCause());
    assertTrue(ex.getMessage().contains("host"));
  }

  @Test
  public void connectionFailedCauseOnly() {
    final RuntimeException cause = new RuntimeException("timeout");
    final SaiConnectionFailedException ex = new SaiConnectionFailedException(cause);
    assertEquals(cause, ex.getCause());
    assertTrue(ex.getMessage().contains("SAI-00001"));
  }

  @Test
  public void connectionFailedIsSaiException() {
    final SaiConnectionFailedException ex = new SaiConnectionFailedException();
    assertTrue(ex instanceof SaiException);
    assertTrue(ex instanceof Exception);
  }

  @Test(expected = SaiConnectionFailedException.class)
  public void connectionFailedCanBeThrown() throws SaiException {
    throw new SaiConnectionFailedException("test");
  }

  // ==================== SaiClosedException ====================

  @Test
  public void closedWithMessage() {
    final SaiClosedException ex = new SaiClosedException("source-1");
    assertTrue(ex.getMessage().contains("SAI-00002"));
    assertTrue(ex.getMessage().contains("source-1"));
  }

  @Test
  public void closedWithCause() {
    final IllegalStateException cause = new IllegalStateException();
    final SaiClosedException ex = new SaiClosedException("x", cause);
    assertEquals(cause, ex.getCause());
  }

  @Test(expected = SaiClosedException.class)
  public void closedCanBeThrown() throws SaiException {
    throw new SaiClosedException();
  }

  // ==================== SaiStartFailedException ====================

  @Test
  public void startFailedWithMessage() {
    final SaiStartFailedException ex = new SaiStartFailedException("权限不足");
    assertTrue(ex.getMessage().contains("SAI-00003"));
    assertTrue(ex.getMessage().contains("权限不足"));
  }

  @Test(expected = SaiStartFailedException.class)
  public void startFailedCanBeThrown() throws SaiException {
    throw new SaiStartFailedException("test");
  }

  // ==================== SaiSubscribeFailedException ====================

  @Test
  public void subscribeFailedWithMessage() {
    final SaiSubscribeFailedException ex = new SaiSubscribeFailedException("table not found");
    assertTrue(ex.getMessage().contains("SAI-00004"));
    assertTrue(ex.getMessage().contains("table not found"));
  }

  @Test(expected = SaiSubscribeFailedException.class)
  public void subscribeFailedCanBeThrown() throws SaiException {
    throw new SaiSubscribeFailedException();
  }

  // ==================== SaiConfigMissingException ====================

  @Test
  public void configMissingWithMessage() {
    final SaiConfigMissingException ex = new SaiConfigMissingException("host");
    assertTrue(ex.getMessage().contains("SAI-00005"));
    assertTrue(ex.getMessage().contains("host"));
  }

  @Test(expected = SaiConfigMissingException.class)
  public void configMissingCanBeThrown() throws SaiException {
    throw new SaiConfigMissingException("test");
  }

  // ==================== SaiConfigInvalidException ====================

  @Test
  public void configInvalidWithMessage() {
    final SaiConfigInvalidException ex = new SaiConfigInvalidException("端口非法");
    assertTrue(ex.getMessage().contains("SAI-00006"));
    assertTrue(ex.getMessage().contains("端口非法"));
  }

  @Test(expected = SaiConfigInvalidException.class)
  public void configInvalidCanBeThrown() throws SaiException {
    throw new SaiConfigInvalidException("test");
  }

  // ==================== SaiIllegalArgumentException ====================

  @Test
  public void illegalArgWithMessage() {
    final SaiIllegalArgumentException ex = new SaiIllegalArgumentException("position < 0");
    assertTrue(ex.getMessage().contains("SAI-00007"));
    assertTrue(ex.getMessage().contains("position < 0"));
  }

  @Test(expected = SaiIllegalArgumentException.class)
  public void illegalArgCanBeThrown() throws SaiException {
    throw new SaiIllegalArgumentException("test");
  }

  // ==================== SaiNotSupportException（运行时） ====================

  @Test
  public void notSupportWithMessage() {
    final SaiNotSupportException ex = new SaiNotSupportException("DELETE");
    assertTrue(ex.getMessage().contains("SAI-00101"));
    assertTrue(ex.getMessage().contains("DELETE"));
  }

  @Test
  public void notSupportWithCause() {
    final UnsupportedOperationException cause = new UnsupportedOperationException();
    final SaiNotSupportException ex = new SaiNotSupportException("x", cause);
    assertEquals(cause, ex.getCause());
  }

  @Test(expected = SaiNotSupportException.class)
  public void notSupportCanBeThrown() {
    throw new SaiNotSupportException("test");
  }

  @Test
  public void notSupportIsRuntimeException() {
    final SaiNotSupportException ex = new SaiNotSupportException();
    assertTrue(ex instanceof SaiRuntimeException);
    assertTrue(ex instanceof RuntimeException);
  }

  // ==================== SaiUnexpectedException（运行时） ====================

  @Test
  public void unexpectedWithMessage() {
    final SaiUnexpectedException ex = new SaiUnexpectedException("NPE");
    assertTrue(ex.getMessage().contains("SAI-00102"));
    assertTrue(ex.getMessage().contains("NPE"));
  }

  @Test
  public void unexpectedCauseOnly() {
    final NullPointerException cause = new NullPointerException();
    final SaiUnexpectedException ex = new SaiUnexpectedException(cause);
    assertEquals(cause, ex.getCause());
    assertTrue(ex.getMessage().contains("SAI-00102"));
  }

  @Test(expected = SaiUnexpectedException.class)
  public void unexpectedCanBeThrown() {
    throw new SaiUnexpectedException("test");
  }

  // ==================== sealed 约束验证 ====================

  @Test
  public void allCheckedExceptionsExtendSaiException() {
    assertTrue(SaiConnectionFailedException.class.getSuperclass() == SaiException.class);
    assertTrue(SaiClosedException.class.getSuperclass() == SaiException.class);
    assertTrue(SaiStartFailedException.class.getSuperclass() == SaiException.class);
    assertTrue(SaiSubscribeFailedException.class.getSuperclass() == SaiException.class);
    assertTrue(SaiConfigMissingException.class.getSuperclass() == SaiException.class);
    assertTrue(SaiConfigInvalidException.class.getSuperclass() == SaiException.class);
    assertTrue(SaiIllegalArgumentException.class.getSuperclass() == SaiException.class);
  }

  @Test
  public void allRuntimeExceptionsExtendSaiRuntimeException() {
    assertTrue(SaiNotSupportException.class.getSuperclass() == SaiRuntimeException.class);
    assertTrue(SaiUnexpectedException.class.getSuperclass() == SaiRuntimeException.class);
  }

  @Test
  public void messageFormatWithDetail() {
    // 无 detail 时以 "." 结尾
    final SaiClosedException noDetail = new SaiClosedException();
    assertTrue(noDetail.getMessage().endsWith("."));
    // 有 detail 时以 ": detail" 拼接
    final SaiClosedException withDetail = new SaiClosedException("src-1");
    assertTrue(withDetail.getMessage().contains(": src-1"));
  }
}
