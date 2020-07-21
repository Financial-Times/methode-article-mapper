package com.ft.methodearticlemapper.methode;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ft.api.jaxrs.errors.ErrorEntity;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class MethodeArticleTransformerErrorEntityFactoryTest {

  private MethodeArticleTransformerErrorEntityFactory factory;
  private final String MESSAGE = "test message";
  private final String UUID_STR = "c47734f2-4896-11e4-a7d4-002128161462";

  @Before
  public void setUp() {
    factory = new MethodeArticleTransformerErrorEntityFactory();
  }

  @Test
  public void shouldReturnAnIdentifiableErrorEntity() throws Exception {
    UUID uuid = UUID.fromString(UUID_STR);
    OffsetDateTime lastModified = OffsetDateTime.now();
    Map<String, Object> context = new HashMap<>();
    context.put("uuid", uuid);
    context.put("lastModified", lastModified);

    ErrorEntity errorEntity = factory.entity(MESSAGE, context);
    assertThat(
        "The Error Entity is incorrect",
        errorEntity,
        is(instanceOf(IdentifiableErrorEntity.class)));
    assertThat("The Message does not match", errorEntity.getMessage(), is(MESSAGE));
    assertThat(((IdentifiableErrorEntity) errorEntity).getUuid().toString(), is(equalTo(UUID_STR)));
    assertThat(
        ((IdentifiableErrorEntity) errorEntity).getLastModified(), is(equalTo(lastModified)));
  }

  @Test
  public void shouldReturnAnErrorEntity() throws Exception {
    Object obj = new Object();
    ErrorEntity errorEntity = factory.entity(MESSAGE, obj);
    assertThat("The Error Entity is incorrect", errorEntity, is(instanceOf(ErrorEntity.class)));
    assertThat(
        "The Error Entity is IdentifiableErrorEntity",
        errorEntity,
        is(not(instanceOf(IdentifiableErrorEntity.class))));
    assertThat("The Message does not match", errorEntity.getMessage(), is(MESSAGE));
  }
}
