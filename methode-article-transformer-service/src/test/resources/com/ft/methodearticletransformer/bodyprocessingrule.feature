@BodyProcessing
Feature: Body processing rules

  This is an overview of how the various configuration rules work.

  For details of which rules apply for particular tags, see bodyprocessing.feature

  Scenario Outline: Rule transformations
    Given the tag <name> adheres to the <rule>
    When it is transformed, <before> becomes <after>

  Examples:
    | rule                                            | name           | before                                                                                                                                                                                                                                                                                                | after                                                                                                                                                                               |
    | STRIP ELEMENT AND CONTENTS                      | applet         | pretext <applet id="myApplet">Text</applet>posttext                                                                                                                                                                                                                                                   | pretext posttext                                                                                                                                                                    |
    | STRIP ELEMENT AND LEAVE CONTENT                 | unknown        | <unknown id="myUnknown">Some unknown text</unknown>                                                                                                                                                                                                                                                   | Some unknown text                                                                                                                                                                   |
    | RETAIN ELEMENT AND REMOVE ATTRIBUTES            | h1             | <h1 id="attr1" class="attr2">Text</h1>                                                                                                                                                                                                                                                                | <h1>Text</h1>                                                                                                                                                                       |
    | TRANSFORM THE TAG                               | i              | He said <i>what?</i>                                                                                                                                                                                                                                                                                  | He said <em>what?</em>                                                                                                                                                              |
    | TRANSFORM THE WEB-PULL-QUOTE TO PULL-QUOTE      | web-pull-quote | <web-pull-quote channel="FTcom"><table><tr><td><web-pull-quote-text>It suits the extremists to encourage healthy eating.</web-pull-quote-text></td></tr><tr><td><web-pull-quote-source>source</web-pull-quote-source></td></tr></table></web-pull-quote>                                              | <pull-quote><pull-quote-text>It suits the extremists to encourage healthy eating.</pull-quote-text><pull-quote-source>source</pull-quote-source></pull-quote>                       |
    | TRANSFORM TAG IF BIG NUMBER                     | promo-box      | <promo-box class="numbers-component"><table width="170px" align="left" cellpadding="6px"><tr><td><promo-headline><p class="title">£350M</p></promo-headline></td></tr><tr><td><promo-intro><p>The cost of eating at Leon and Tossed every single day.</p></promo-intro></td></tr></table></promo-box> | <big-number><big-number-headline><p>£350M</p></big-number-headline><big-number-intro><p>The cost of eating at Leon and Tossed every single day.</p></big-number-intro></big-number> |
    | RETAIN ELEMENT AND REMOVE FORMATTING ATTRIBUTES | table          | <table class="data-table" id="U1817116616509jH"><caption id="k63G"><span id="U181711661650mIC">KarCrash Q1  02/2014- period from to 09/2014</span></caption><tr><th>Sales</th></tr><tr><td>324↑ ↓324</td></tr></table>                                                                                | <table class="data-table"><caption>KarCrash Q1  02/2014- period from to 09/2014</caption><tr><th>Sales</th></tr><tr><td>324↑ ↓324</td></tr></table>                                 |
    | TRANSFORM THE SCRIPT ELEMENT TO PODCAST         | script         | <script type="text/javascript">/* <![CDATA[ */window.onload=function(){embedLink('podcast.ft.com','2463','18','lucy060115.mp3','Golden Flannel of the year award','Under Tim Cook’s leadership, Apple succumbed to drivel, says Lucy Kellaway','ep_2463','share_2463');}/* ]]> ></script>             | <a data-asset-type="podcast" data-embedded="true" href="http://podcast.ft.com/p/2463" title="Golden Flannel of the year award"></a>                                                 |
    | TRANSFORM THE TAG TO VIDEO                      | videoPlayer    | <videoPlayer videoID="3920663836001"></videoPlayer>                                                                                                                                                                                                                                                   | <a data-asset-type="video" data-embedded="true" href="http://video.ft.com/3920663836001"></a>                                                                                       |
    | TRANSFORM OTHER VIDEO TYPES                     | iframe         | <p channel="FTcom">Youtube Video<iframe src="http://www.youtube.com/embed/77761436"></iframe></p>                                                                                                                                                                                                     | <p>Youtube Video<a data-asset-type="video" data-embedded="true" href="https://www.youtube.com/watch?v=77761436"></a></p>                                                            |

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

  Scenario Outline: Line Breaks
    Given I have a "line break" in a Methode XML body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before               | after                |
    | <p>Blah<br/>Blah</p> | <p>Blah<br/>Blah</p> |

  Scenario Outline: Subhead should become a h3 with the class attribute equal to ft-subhead
    Given I have a "subhead" in a Methode XML body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                     | after                               |
    | <subhead>Duchess</subhead> | <h3 class="ft-subhead">Duchess</h3> |

Scenario Outline: Empty Paragraphs
    Given I have a "empty paragraph" in a Methode XML body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                                                                      | after                                         |
    | <body><p>Some text</p><p></p><p>More text</p></body>                                                        | <body><p>Some text</p><p>More text</p></body> |
    | <body><p>Some text</p><p><xref>this xref is removed leaving an empty para</xref></p><p>More text</p></body> | <body><p>Some text</p><p>More text</p></body> |


  Scenario Outline: Handle block elements inside paragraph tags
    Given I have a "block element" in a Methode XML body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                                                                                                                                                                                                                                                                                                                                                                | after                                                                                                                                                                                                                                                                                                                                     |
    | <body><p>This is a line of text<promo-box class="numbers-component"><table><tr><td><promo-headline><p class="title">£350m</p></promo-headline></td></tr><tr><td><promo-intro><p>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</p></promo-intro></td></tr></table></promo-box>this is another line of text</p></body> | <body><p>This is a line of text</p><big-number><big-number-headline><p>£350m</p></big-number-headline><big-number-intro><p>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</p></big-number-intro></big-number><p>this is another line of text</p></body> |
    | <body><p>This is a line of text</p><p><web-pull-quote align="left" channel="FTcom"><table align="left" cellpadding="6px" width="170px"><tr><td><web-pull-quote-text><p>It suits the extremists to encourage healthy eating.</p></web-pull-quote-text></td></tr><tr><td><web-pull-quote-source>source1</web-pull-quote-source></td></tr></table></web-pull-quote></p></body>                           | <body><p>This is a line of text</p><pull-quote><pull-quote-text><p>It suits the extremists to encourage healthy eating.</p></pull-quote-text><pull-quote-source>source1</pull-quote-source></pull-quote></body>|

  Scenario Outline: Handle videos
    Given I have a "video" in a Methode XML body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                                                                                                                                                                                                                 | after                                                                                                                                      |
    | <body><videoPlayer videoID="3920663836001"><web-inline-picture id="U2113113643377jlC" width="150" fileref="/FT/Graphics/Online/Z_Undefined/FT-video-story.jpg?uuid=91b39ae8-ccff-11e1-92c1-00144feabdc0" tmx="150 100 150 100"/></videoPlayer></body>  | <body><a data-asset-type="video" data-embedded="true" href="http://video.ft.com/3920663836001"></a></body>                                 |
    | <body><p align="left" channel="FTcom">Youtube Video<iframe height="245" frameborder="0" allowfullscreen="" src="http://www.youtube.com/embed/YoB8t0B4jx4" width="600"></iframe></p></body>                                                             | <body><p>Youtube Video<a data-asset-type="video" data-embedded="true" href="https://www.youtube.com/watch?v=YoB8t0B4jx4"></a></p></body>   |
    | <body><p align="left" channel="FTcom">Vimeo Video<iframe height="245" frameborder="0" src="//player.vimeo.com/video/77761436" width="600"></iframe></p></body>                                                                                         | <body><p>Vimeo Video<a data-asset-type="video" data-embedded="true" href="https://www.vimeo.com/77761436"></a></p></body>                   |
    | <body><p>Vimeo Video<iframe height="245" frameborder="0" src="//player.vimeo.com/video/77761436" width="600"></iframe></p></body>                                                                                                                      | <body><p>Vimeo Video<a data-asset-type="video" data-embedded="true" href="https://www.vimeo.com/77761436"></a></p></body>                   |
    | <body><p channel="FTcom">Vimeo Video<iframe height="245" frameborder="0" src="http://player.bbc.com/video/77761436" width="600"></iframe></p></body>                                                                                                   | <body><p>Vimeo Video</p></body>                                                                                                            |


  Scenario Outline: Handle strikeouts
    Given I have a "strikeout element" in a Methode XML body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples: Strikeout channels that should exclude the content from the API
    | before                                                                                                                        | after                                            |
    | <body><p channel="!">Para with strikeout channel that should be removed </p></body>                                           | <body></body>                                    |
    | <body><p>Para containing <span channel="!">a strikeout that should be removed and </span>other text</p></body>                | <body><p>Para containing other text</p></body>   |
    | <body><p channel="Financial Times">Para with strikeout channel that should be removed </p></body>                             | <body></body>                                    |
    | <body><p>Para containing <span channel="Financial Times">a strikeout that should be removed and </span>other text</p></body>  | <body><p>Para containing other text</p></body>   |
    | <body><p channel="!FTcom">Para with strikeout channel that should be removed </p></body>                                      | <body></body>                                    |
    | <body><p>Para containing <span channel="!FTcom">a strikeout that should be removed and </span>other text</p></body>           | <body><p>Para containing other text</p></body>   |
    | <body><p channel="">Para with strikeout channel that should be removed </p></body>                                            | <body></body>                                    |
    | <body><p>Para containing <span channel="">a strikeout that should be removed and </span>other text</p></body>                 | <body><p>Para containing other text</p></body>   |

  Examples: Strikeout channels that should be included in the content from the API
    | before                                                                                                                          | after                                                                                    |
    | <body><p channel="!Financial Times">Para with strikeout channel that should be retained</p></body>                              | <body><p>Para with strikeout channel that should be retained</p></body>                  |
    | <body><p>Para containing <span channel="!Financial Times">a strikeout that should be retained</span> and other text</p></body>  | <body><p>Para containing a strikeout that should be retained and other text</p></body>   |
    | <body><p channel="FTcom">Para with strikeout channel that should be retained</p></body>                                         | <body><p>Para with strikeout channel that should be retained</p></body>                  |
    | <body><p>Para containing <span channel="FTcom">a strikeout that should be retained </span>and other text</p></body>             | <body><p>Para containing a strikeout that should be retained and other text</p></body>   |


  Scenario Outline: Handle non-strikeouts
    Given I have a "non-strikeout element" in a Methode XML body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                                          | after                                      |
    | <body><p>Para 1</p><p title="not a strikeout">Para 2</p></body>                 | <body><p>Para 1</p><p>Para 2</p></body>    |
    | <body><p>Part 1 <span title="not a strikeout">containing</span> text</p></body> | <body><p>Part 1 containing text</p></body> |

  Scenario Outline: Remove comments
    Given I have a "comment" in a Methode XML body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                | after                                       |
    | <body>Sentence <!--...-->ending. Next sentence</body> | <body>Sentence ending. Next sentence</body> |

  Scenario Outline: Empty body
    Given I have a "empty body" in a Methode XML body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before        | after   |
    | <body></body> | <body></body> |

  Scenario Outline: Namespaces are ignored
    Given I have a "namespace" in a Methode XML body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                       | after       |
    | <p v:vs="\|1\|" v:n="15" v:idx="11">Text</p> | <p>Text</p> |

  Scenario Outline: Images are retained with valid attributes
    Given I have an "img" in a Methode article body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                                                                                                                                                   | after                  |
    | <p>Embedded image.<img height="445" alt="Saloua Raouda Choucair's ‘Composition'" width="600" src="http://im.ft-static.com/content/images/7784185e-a888-11e2-8e5d-00144feabdc0.img" align="left"/></p> | <p>Embedded image.<img alt="Saloua Raouda Choucair's ‘Composition'" height="445" src="http://im.ft-static.com/content/images/7784185e-a888-11e2-8e5d-00144feabdc0.img" width="600"/></p> |
