/*
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.dataflow.sdk.util;

import static com.google.cloud.dataflow.sdk.util.TimeUtil.fromCloudDuration;
import static com.google.cloud.dataflow.sdk.util.TimeUtil.fromCloudTime;
import static com.google.cloud.dataflow.sdk.util.TimeUtil.toCloudDuration;
import static com.google.cloud.dataflow.sdk.util.TimeUtil.toCloudTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link TimeUtil}. */
@RunWith(JUnit4.class)
public final class TimeUtilTest {
  @Test
  public void toCloudTimeShouldPrintTimeStrings() {
    assertEquals("1970-01-01T00:00:00Z", toCloudTime(new Instant(0)));
    assertEquals("1970-01-01T00:00:00.001Z", toCloudTime(new Instant(1)));
  }

  @Test
  public void fromCloudTimeShouldParseTimeStrings() {
    assertEquals(new Instant(0), fromCloudTime("1970-01-01T00:00:00Z"));
    assertEquals(new Instant(1), fromCloudTime("1970-01-01T00:00:00.001Z"));
    assertEquals(new Instant(1), fromCloudTime("1970-01-01T00:00:00.001000Z"));
    assertEquals(new Instant(1), fromCloudTime("1970-01-01T00:00:00.001001Z"));
    assertEquals(new Instant(1), fromCloudTime("1970-01-01T00:00:00.001000000Z"));
    assertEquals(new Instant(1), fromCloudTime("1970-01-01T00:00:00.001000001Z"));
    assertNull(fromCloudTime(""));
    assertNull(fromCloudTime("1970-01-01T00:00:00"));
  }

  @Test
  public void toCloudDurationShouldPrintDurationStrings() {
    assertEquals("0s", toCloudDuration(Duration.ZERO));
    assertEquals("4s", toCloudDuration(Duration.millis(4000)));
    assertEquals("4.001s", toCloudDuration(Duration.millis(4001)));
  }

  @Test
  public void fromCloudDurationShouldParseDurationStrings() {
    assertEquals(Duration.millis(4000), fromCloudDuration("4s"));
    assertEquals(Duration.millis(4001), fromCloudDuration("4.001s"));
    assertEquals(Duration.millis(4001), fromCloudDuration("4.001000s"));
    assertEquals(Duration.millis(4001), fromCloudDuration("4.001001s"));
    assertEquals(Duration.millis(4001), fromCloudDuration("4.001000000s"));
    assertEquals(Duration.millis(4001), fromCloudDuration("4.001000001s"));
    assertNull(fromCloudDuration(""));
    assertNull(fromCloudDuration("4"));
    assertNull(fromCloudDuration("4.1"));
    assertNull(fromCloudDuration("4.1s"));
  }
}
