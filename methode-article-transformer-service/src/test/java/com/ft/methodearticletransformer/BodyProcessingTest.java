package com.ft.methodearticletransformer;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(tags="@BodyProcessing", monochrome=true, format = { "pretty", "html:target/cucumber-html-report", "json:target/cucumber-json-report/publishing.json" })
public class BodyProcessingTest {
}
