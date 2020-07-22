package com.ft.methodearticlemapper.http;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;

import org.junit.Test;

public class VersionNumberTest {

  @Test
  public void shouldReportNextBuildAsGreater() {
    VersionNumber versionA = new VersionNumber("1.2.3");
    VersionNumber versionB = new VersionNumber("1.2.4");

    assertThat(versionA.compareTo(versionB), lessThan(0));
    assertThat(versionB.compareTo(versionA), greaterThan(0));
  }

  @Test
  public void shouldReportNewPatchVersionWithExtraDigitAsGreater() {
    VersionNumber versionA = new VersionNumber("1.2.3");
    VersionNumber versionB = new VersionNumber("1.2.3.1");

    assertThat(versionA.compareTo(versionB), lessThan(0));
    assertThat(versionB.compareTo(versionA), greaterThan(0));
  }

  @Test
  public void shouldReportOldPatchVersionWithExtraDigitAsSmaller() {
    VersionNumber versionA = new VersionNumber("1.2.4");
    VersionNumber versionB = new VersionNumber("1.2.3.1");

    assertThat(versionA.compareTo(versionB), greaterThan(0));
    assertThat(versionB.compareTo(versionA), lessThan(0));
  }

  @Test
  public void shouldReportIdenticalVersionAsIdentical() {
    VersionNumber versionA = new VersionNumber("1.2.4");
    VersionNumber versionB = new VersionNumber("1.2.4");

    assertThat(versionA.compareTo(versionB), equalTo(0));
    assertThat(versionB.compareTo(versionA), equalTo(0));
  }

  @Test
  public void shouldReportNextMinorVersionAsGreater() {
    VersionNumber versionA = new VersionNumber("1.3.3");
    VersionNumber versionB = new VersionNumber("1.2.3");

    assertThat(versionA.compareTo(versionB), greaterThan(0));
    assertThat(versionB.compareTo(versionA), lessThan(0));
  }

  @Test
  public void shouldReportNextMajorVersionAsGreater() {
    VersionNumber versionA = new VersionNumber("2.0.0");
    VersionNumber versionB = new VersionNumber("1.2.3");

    assertThat(versionA.compareTo(versionB), greaterThan(0));
    assertThat(versionB.compareTo(versionA), lessThan(0));
  }
}
