@BodyProcessing
Feature: Body processing rules

  This is an overview of how the various configuration rules work.

  For details of which rules apply for particular tags, see bodyprocessing.feature

  Scenario Outline:
    Given the tag <name> adheres to the <rule>
    When it is transformed, <before> becomes <after>

  Examples:
    | rule                                 | name           | before                                                                                                                                                                                                                                                                                                | after                                                                                                                                                                 |
    | STRIP ELEMENT AND CONTENTS           | applet         | pretext <applet id="myApplet">Text</applet>posttext                                                                                                                                                                                                                                                   | pretext posttext                                                                                                                                                      |
    | STRIP ELEMENT AND LEAVE CONTENT      | unknown        | <unknown id="myUnknown">Some unknown text</unknown>                                                                                                                                                                                                                                                   | Some unknown text                                                                                                                                                     |
    | RETAIN ELEMENT AND REMOVE ATTRIBUTES | h1             | <h1 id="attr1" class="attr2">Text</h1>                                                                                                                                                                                                                                                                | <h1>Text</h1>                                                                                                                                                         |
    | TRANSFORM THE TAG                    | i              | He said <i>what?</i>                                                                                                                                                                                                                                                                                  | He said <em>what?</em>                                                                                                                                                |
    | TRANSFORM THE TAG TO PULL QUOTE      | web-pull-quote | <web-pull-quote channel="FTcom"><table><tr><td><web-pull-quote-text>It suits the extremists to encourage healthy eating.</web-pull-quote-text></td></tr><tr><td><web-pull-quote-source>source</web-pull-quote-source></td></tr></table></web-pull-quote>                                              | <pull-quote><pull-quote-text>It suits the extremists to encourage healthy eating.</pull-quote-text><pull-quote-source>source</pull-quote-source></pull-quote>         |
    | TRANSFORM TAG IF BIG NUMBER          | promo-box      | <promo-box class="numbers-component"><table width="170px" align="left" cellpadding="6px"><tr><td><promo-headline><p class="title">£350M</p></promo-headline></td></tr><tr><td><promo-intro><p>The cost of eating at Leon and Tossed every single day.</p></promo-intro></td></tr></table></promo-box> | <big-number><big-number-headline>£350M</big-number-headline><big-number-intro>The cost of eating at Leon and Tossed every single day.</big-number-intro></big-number> |
    | TRANSFORM THE TAG TO TABLE           | table          | <table class="data-table" id="U1817116616509jH"><caption id="k63G"><span id="U181711661650mIC">KarCrash Q1  02/2014- period from to 09/2014</span></caption><tr><th>Sales</th></tr><tr><td>324↑ ↓324</td></tr></table>                                                                                | <table class="data-table"><caption>KarCrash Q1  02/2014- period from to 09/2014</caption><tr><th>Sales</th></tr><tr><td>324↑ ↓324</td></tr></table>                   |
    | TRANSFORM PODCAST ELEMENT            | script         | <script type="text/javascript">/* <![CDATA[ */window.onload=function(){embedLink('podcast.ft.com','2463','18','lucy060115.mp3','Golden Flannel of the year award','Under Tim Cook’s leadership, Apple succumbed to drivel, says Lucy Kellaway','ep_2463','share_2463');}/* ]]> ></script>             | <a data-asset-type="podcast" data-embedded="true" href="http://podcast.ft.com/p/2463" title="Golden Flannel of the year award"/>                                      |
    | TRANSFORM THE TAG TO VIDEO           | videoPlayer    | <videoPlayer videoID="3920663836001"></videoPlayer>                                                                                                                                                                                                                                                   | <a href="http://video.ft.com/3920663836001"/>                                                                                                                         |
    | TRANSFORM OTHER VIDEO TYPES          | p              | <p channel="FTcom">Youtube Video<iframe src="http://www.youtube.com/embed/77761436"></iframe></p>                                                                                                                                                                                                     | <p><a href="http://www.youtube.com/embed/77761436"/></p>                                                                                                              |

  Scenario Outline: Transform one tag into another
    Given the before tag <beforename> and the after tag <aftername> adheres to the TRANSFORM THE TAG rule
    When it is transformed, <before> becomes <after>

  Examples:
    | beforename | aftername | before               | after                          |
    | b          | strong    | He said <b>what?</b> | He said <strong>what?</strong> |

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
    | before                                       | after                                 |
    | <p>Some text</p><p></p><p>Some more text</p> | <p>Some text</p><p>Some more text</p> |
  
