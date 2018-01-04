package com.ft.methodearticlemapper.transformation;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessor;

import org.junit.Test;

import java.util.EnumSet;


public class ModalBodyProcessorTest {
    @Test
    public void thatWrappedProcessorIsInvokedForIncludedMode() {
        String input = "foo";
        String output = "bar";
        BodyProcessor p = mock(BodyProcessor.class);
        when(p.process(eq(input), any(BodyProcessingContext.class))).thenReturn(output);
        
        ModalBodyProcessor modal = new ModalBodyProcessor(p, EnumSet.of(TransformationMode.PUBLISH));
        ModalBodyProcessingContext ctx = mock(ModalBodyProcessingContext.class);
        when(ctx.getTransformationMode()).thenReturn(TransformationMode.PUBLISH);
        
        String actual = modal.process(input, ctx);
        assertThat(actual, equalTo(output));
    }
    
    @Test
    public void thatWrappedProcessorIsNotInvokedForExcludedMode() {
        String input = "foo";
        String output = "bar";
        BodyProcessor p = mock(BodyProcessor.class);
        when(p.process(eq(input), any(BodyProcessingContext.class))).thenReturn(output);
        
        ModalBodyProcessor modal = new ModalBodyProcessor(p, EnumSet.of(TransformationMode.PUBLISH));
        ModalBodyProcessingContext ctx = mock(ModalBodyProcessingContext.class);
        when(ctx.getTransformationMode()).thenReturn(TransformationMode.PREVIEW);
        
        String actual = modal.process(input, ctx);
        assertThat(actual, equalTo(input));
        verifyZeroInteractions(p);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void thatModalBodyProcessingContextIsRequired() {
        String input = "foo";
        BodyProcessor p = mock(BodyProcessor.class);
        
        ModalBodyProcessor modal = new ModalBodyProcessor(p, EnumSet.of(TransformationMode.PUBLISH));
        BodyProcessingContext ctx = mock(BodyProcessingContext.class);
        
        modal.process(input, ctx);
    }
}
