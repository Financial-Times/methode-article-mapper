@BodyProcessing
Feature: Body processing rules

  This is an overview of how the various configuration rules work.

  For details of which rules apply for particular tags, see bodyprocessing.feature

  Scenario Outline:
    Given the tag <name> adheres to the <rule>
    When it is transformed, <before> becomes <after>

  Examples:
    | rule                                  | name           | before                                                | after             |
    | STRIP ELEMENT AND CONTENTS            | applet         | pretext <applet id="myApplet">Text</applet>posttext   | pretext posttext  |
    | STRIP ELEMENT AND LEAVE CONTENT       | unknown        | <unknown id="myUnknown">Some unknown text</unknown>   | Some unknown text |
    | RETAIN ELEMENT AND REMOVE ATTRIBUTES  | h1             | <h1 id="attr1" class="attr2">Text</h1>                | <h1>Text</h1>     |
    | TRANSFORM THE TAG TO PULL QUOTE       | web-pull-quote | pretext <applet id="myApplet">Text</applet>posttext   | pretext posttext  |
#    | TRANSFORM TAG IF BIG NUMBER          |                |                                                       |                   |
#    | TRANSFORM THE TAG TO TABLE           |                |                                                       |                   |
#    | STRIP PODCAST ELEMENT                |                |                                                       |                   |
#    | TRANSFORM THE TAG TO VIDEO           |                |                                                       |                   |

  Scenario Outline: Transform one tag into another
    Given the before tag <beforename> and the after tag <aftername> adheres to the TRANSFORM THE TAG rule
    When it is transformed, <before> becomes <after>

  Examples:
    | beforename  | aftername | before                     | after                            |
    | b           | strong    | He said <b>what?</b>       | He said <strong>what?</strong>   |

  Scenario Outline: Convert HTML entities to unicode
    Given I have a rule to CONVERT HTML ENTITY TO UNICODE and an entity <entity>
    When it is transformed the entity <entity> should be replaced by the unicode codepoint <codepoint>

  Examples:
    | entity | codepoint |
    | &euro; | 0x20AC    |
    | &nbsp; | 0x00A0    |

  Scenario Outline: Remove empty paragraphs
    Given there are empty paragraphs in the body
    When it is transformed, <before> becomes <after>

  Examples: Remove empty paragraphs
    | before                                          | after                                  |
    | <p>Some text</p><p></p><p>Some more text</p>    | <p>Some text</p><p>Some more text</p>  |