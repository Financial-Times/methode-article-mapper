package com.ft.methodearticletransformer.transformation;

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

    private FieldTransformer bylineTransformer;
	private static final String TRANSACTION_ID = "tid_test";

	@Before
    public void setup() {
        bylineTransformer = new BylineProcessingFieldTransformerFactory().newInstance();
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
	public void shouldRemoveFinancialTimesChannelText() {
		checkTransformation("By <author-name>Martin <b channel=\"Financial Times\">Rodders</b> Roddam</author-name>", "By Martin  Roddam");
	}

	@Test
	public void shouldRemoveNotFtComChannelText() {
		checkTransformation("By <author-name>Martin <b channel=\"!FTcom\">Rodders</b> Roddam</author-name>", "By Martin  Roddam");
	}

	@Test
	public void shouldRemoveNotAnythingChannelText() {
		checkTransformation("By <author-name>Martin <b channel=\"!\">Rodders</b> Roddam</author-name>", "By Martin  Roddam");
	}

	@Test
	public void shouldRemoveEmptyChannelText() {
		checkTransformation("By <author-name>Martin <b channel=\"!\">Rodders</b> Roddam</author-name>", "By Martin  Roddam");
	}

	@Test
	public void shouldKeepNotFinancialTimesChannelText() {
		checkTransformation("By <author-name>Martin <b channel=\"!Financial Times\">Rodders</b> Roddam</author-name>", "By Martin Rodders Roddam");
	}

	@Test
	public void shouldKeepFtComChannelText() {
		checkTransformation("By <author-name>Martin <b channel=\"FTcom\">Rodders</b> Roddam</author-name>", "By Martin Rodders Roddam");
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
    public void notesShouldBeRemoved() {
        checkTransformation("Text with a note <span class=\"@notes\"></span>in the middle", "Text with a note in the middle");
    }

    @Test
    public void annotationsShouldBeRemoved() {
        checkTransformation("This is <annotation c=\"roddamm\" cd=\"20150224170716\">A new annotation </annotation>annotated", "This is annotated");
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

    private void checkTransformation(String originalByline, String expectedTransformedByline) {
        String actualTransformedByline = bylineTransformer.transform(originalByline, TRANSACTION_ID);
        assertThat(actualTransformedByline, is(equalTo(expectedTransformedByline)));
    }

}
