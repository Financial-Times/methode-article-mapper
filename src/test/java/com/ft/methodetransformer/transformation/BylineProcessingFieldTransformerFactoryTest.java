package com.ft.methodetransformer.transformation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.ft.bodyprocessing.BodyProcessingException;

public class BylineProcessingFieldTransformerFactoryTest {
	
	@Rule
    public ExpectedException expectedException = ExpectedException.none();

    private FieldTransformer bodyTransformer;
	private static final String TRANSACTION_ID = "tid_test";

	@Before
    public void setup() {
        bodyTransformer = new BylineProcessingFieldTransformerFactory().newInstance();
    }
     
    @Test
    public void shouldThrowExceptionIfBylineNull() {
    	expectedException.expect(BodyProcessingException.class);
    	//TODO - we may want this to say byline instead??
        expectedException.expect(hasProperty("message", equalTo("Body is null")));
    	checkTransformation(null, "");
    }

    @Test
    public void authorNameTagShouldBeRemovedLeavingContent() {
        checkTransformation("By <author-name>Martin Roddam</author-name>", "By Martin Roddam");
    }

    @Test
    public void formattingTagsShouldBeRemovedLeavingContent() {
        checkTransformation("By <author-name>Martin <b>Roddam</b></author-name>", "By Martin Roddam");
    }
    
    @Test
    public void defaultBylineShouldBeReturnedAsEmptyByline() {
    	checkTransformation("<byline>By <author-name><?EM-dummyText [Insert author]?></author-name></byline>", "");
    }
    
    @Test
    public void partlyEditedDefaultBylineShouldBeReturnedAsEmptyByline() {
    	checkTransformation("<byline>By <author-name></author-name></byline>", "");
    }
    
    @Test
    public void emptyBylineShouldBeReturnedAsEmptyByline() {
    	checkTransformation("", "");
    }

    @Test
    public void commentsShouldBeRemoved() {
        checkTransformation("Sentence <!--...-->ending. Next sentence",
                "Sentence ending. Next sentence");
    }

    @Test
    public void nbspShouldBeReplacedWithSpace() {
        checkTransformation("This is a sentence&nbsp;.",
                String.format("This is a sentence%s.", String.valueOf('\u00A0')));
    }

    @Test
    public void encodedNbspShouldBeReplacedWithSpace() {
        checkTransformation("This is a sentence .",
                String.format("This is a sentence%s.", String.valueOf('\u00A0')));
    }

    @Test
    public void htmlEntityReferencesShouldBeUnescaped() {
        checkTransformation("This is a sentence&euro;.",
                String.format("This is a sentence%s.", String.valueOf('\u20AC')));
    }

    @Test
    public void xmlEntitiesShouldBeUnencoded() {
        checkTransformation("Standard &amp; Poor &lt; Yahoo", "Standard & Poor < Yahoo");
    }

    private void checkTransformation(String originalBody, String expectedTransformedBody) {
        String actualTransformedBody = bodyTransformer.transform(originalBody, TRANSACTION_ID);
        assertThat(actualTransformedBody, is(equalTo(expectedTransformedBody)));
    }

}
