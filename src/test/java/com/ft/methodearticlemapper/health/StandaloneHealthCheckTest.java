package com.ft.methodearticlemapper.health;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ft.platform.dropwizard.AdvancedHealthCheck;
import com.ft.platform.dropwizard.AdvancedResult;
import org.junit.Test;

public class StandaloneHealthCheckTest {
  @Test
  public void thatHealthCheckShouldPass() throws Exception {
    String url = "http://www.example.com/panic.html";
    AdvancedHealthCheck hc = new StandaloneHealthCheck(url);
    AdvancedResult actual = hc.executeAdvanced();

    assertThat(actual.status(), is(equalTo(AdvancedResult.Status.OK)));
    assertThat(((StandaloneHealthCheck) hc).panicGuideUrl(), is(equalTo(url)));
  }
}
