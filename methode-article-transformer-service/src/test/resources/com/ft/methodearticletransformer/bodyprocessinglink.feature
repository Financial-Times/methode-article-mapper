@BodyProcessing
Feature: Body processing links

#  separate scenarios of one generic scenario with internal/external/relative etc examples
#  NOTE: do not merge. This is not the final specification - internal links will eventually be processed down to UUIDs
  Scenario Outline:
    Given I have a body <internal link html>
    When I transform it
    Then it is left unmodified

  Examples:
    | internal link html                                                               |
    | <p>Good stories <a href="http://on.ft.com/1v6P55X">link to</a> other stories</p> |


  Scenario Outline:
    Given I have a body <external link html>
    When I transform it
    Then it is left unmodified

  Examples:
    | external link html                                                                                    |
    | <p>Better stories <a href="http://example.com/fascinating-insights.html">link to other</a> sites</p>  |
