package com.ft.methodearticlemapper.model.concordance;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Concordances
 *
 * @author Simon.Gibbs
 */
public class Concordances {

  private List<Concordance> concordances;

  public Concordances(@JsonProperty("concordances") List<Concordance> concordances) {
    this.concordances = concordances;
  }

  public List<Concordance> getConcordances() {
    return concordances;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("concordances", concordances).toString();
  }
}
