@BodyProcessing
Feature: Body processing

  Scenario Outline: Strip tag and retain contents
    Given I have body text in Methode XML format containing <tagname>
    When I transform it into our Content Store format
    Then the start tag <tagname> should have been removed
    And the end tag <tagname> should have been removed
    But the text inside should not have been removed

  Examples:
    | tagname |

  Scenario Outline: Strip tag and contents
    Given I have body text in Methode XML format containing <tagname>
    When I transform it into our Content Store format
    Then the start tag <tagname> should have been removed
    And the end tag <tagname> should have been removed
    And the text inside should have been removed

  Examples:
    | tagname                    |
    | applet                     |
    | audio                      |
    | base                       |
    | basefont                   |
    | button                     |
    | canvas                     |
    | caption                    |
    | col                        |
    | colgroup                   |
    | command                    |
    | datalist                   |
    | del                        |
    | dir                        |
    | embed                      |
    | fieldset                   |
    | form                       |
    | frame                      |
    | frameset                   |
    | head                       |
    | iframe                     |
    | input                      |
    | keygen                     |
    | label                      |
    | legend                     |
    | link                       |
    | map                        |
    | menu                       |
    | meta                       |
    | nav                        |
    | noframes                   |
    | noscript                   |
    | object                     |
    | optgroup                   |
    | option                     |
    | output                     |
    | param                      |
    | progress                   |
    | rp                         |
    | rt                         |
    | ruby                       |
    | s                          |
    | script                     |
    | select                     |
    | source                     |
    | strike                     |
    | style                      |
    | table                      |
    | tbody                      |
    | td                         |
    | textarea                   |
    | tfoot                      |
    | th                         |
    | thead                      |
    | tr                         |
    | track                      |
    | video                      |
    | wbr                        |
    | byline                     |
    | editor-choice              |
    | headline                   |
    | inlineDwc                  |
    | interactive-chart          |
    | lead-body                  |
    | lead-text                  |
    | ln                         |
    | photo                      |
    | photo-caption              |
    | photo-group                |
    | plainHtml                  |
    | promo-box                  |
    | promo-headline             |
    | promo-image                |
    | promo-intro                |
    | promo-link                 |
    | promo-title                |
    | promobox-body              |
    | pull-quote                 |
    | pull-quote-header          |
    | pull-quote-text            |
    | readthrough                |
    | short-body                 |
    | skybox-body                |
    | stories                    |
    | story                      |
    | strap                      |
    | videoObject                |
    | videoPlayer                |
    | web-alt-picture            |
    | web-background-news        |
    | web-background-news-header |
    | web-background-news-text   |
    | web-picture                |
    | web-pull-quote             |
    | web-pull-quote-source      |
    | web-pull-quote-text        |
    | web-skybox-picture         |
    | web-subhead                |
    | web-thumbnail              |
    | xref                       |
    | xrefs                      |

  Scenario Outline: Retain tag and contents
    Given I have body text in Methode XML format containing <tagname>
    When I transform it into our Content Store format
    Then the start tag <tagname> should be present
    And the end tag <tagname> should be present
    And the text inside should not have been removed

  Examples:
    | tagname |
    | strong  |
    | em      |
    | sub     |
    | sup     |
    | h1      |
    | h2      |
    | h3      |
    | h4      |
    | h5      |
    | h6      |
    | p       |
    | ol      |
    | ul      |
    | li      |

  Scenario Outline: Some tag names are transformed to their valid HTML 5 equivalents
    Given I have body text in Methode XML format containing <tagname>
    When I transform it into our Content Store format
    Then the start tag <tagname> should have been replaced by <replacement>
    And the end tag <tagname> should have been replaced by <replacement>
    And the text inside should not have been removed

  Examples:
    | tagname | replacement |
    | b       | strong      |
    | i       | em          |


  @Technical
  Scenario Outline: Line Breaks
    Given I have body text in Methode XML like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before               | after                |
    | <p>Blah<br/>Blah</p> | <p>Blah<br/>Blah</p> |

  Scenario Outline: Subhead should become a h3 with the class attribute equal to ft-subhead
    Given I have body text in Methode XML like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                     | after                               |
    | <subhead>Duchess</subhead> | <h3 class="ft-subhead">Duchess</h3> |

  @Technical
  Scenario Outline: Empty Paragraphs
    Given I have body text in Methode XML like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                                                                      | after                                         |
    | <body><p>Some text</p><p></p><p>More text</p></body>                                                        | <body><p>Some text</p><p>More text</p></body> |
    | <body><p>Some text</p><p><xref>this xref is removed leaving an empty para</xref></p><p>More text</p></body> | <body><p>Some text</p><p>More text</p></body> |

  Scenario Outline: Handle strikeouts
    Given I have body text in Methode XML like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                                                                 | after                                          |
    | <body><p>Para with no strikeout</p><p channel="!">Para with strikeout</p></body>                       | <body><p>Para with no strikeout</p></body>     |
    | <body><p>Para containing <span channel="!">a strikeout and </span>other text</p></body>                | <body><p>Para containing other text</p></body> |
    | <body><p>Para with no strikeout</p><p channel="Financial Times">Para with strikeout</p></body>         | <body><p>Para with no strikeout</p></body>     |
    | <body><p>Para containing <span channel="Financial Times">a strikeout and </span>other text</p></body>  | <body><p>Para containing other text</p></body> |
    | <body><p>Para with no strikeout</p><p channel="!Financial Times">Para with strikeout</p></body>        | <body><p>Para with no strikeout</p></body>     |
    | <body><p>Para containing <span channel="!Financial Times">a strikeout and </span>other text</p></body> | <body><p>Para containing other text</p></body> |
    | <body><p>Para with no strikeout</p><p channel="FTcom">Para with strikeout</p></body>                   | <body><p>Para with no strikeout</p></body>     |
    | <body><p>Para containing <span channel="FTcom">a strikeout and </span>other text</p></body>            | <body><p>Para containing other text</p></body> |
    | <body><p>Para with no strikeout</p><p channel="!FTcom">Para with strikeout</p></body>                  | <body><p>Para with no strikeout</p></body>     |
    | <body><p>Para containing <span channel="!FTcom">a strikeout and </span>other text</p></body>           | <body><p>Para containing other text</p></body> |
    | <body><p>Para with no strikeout</p><p channel="">Para with strikeout</p></body>                        | <body><p>Para with no strikeout</p></body>     |
    | <body><p>Para containing <span channel="">a strikeout and </span>other text</p></body>                 | <body><p>Para containing other text</p></body> |

  Scenario Outline: Handle non-strikeouts
    Given I have body text in Methode XML like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                                          | after                                      |
    | <body><p>Para 1</p><p title="not a strikeout">Para 2</p></body>                 | <body><p>Para 1</p><p>Para 2</p></body>    |
    | <body><p>Part 1 <span title="not a strikeout">containing</span> text</p></body> | <body><p>Part 1 containing text</p></body> |

  @Technical
  Scenario Outline: Remove comments
    Given I have body text in Methode XML like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                | after                                       |
    | <body>Sentence <!--...-->ending. Next sentence</body> | <body>Sentence ending. Next sentence</body> |

  @Technical
  Scenario Outline: Empty body
    Given I have body text in Methode XML like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before        | after   |
    |               |         |
    | <body></body> | <body/> |

  @Technical
  Scenario Outline: Entity translation to unicode
    Given an entity reference <entity>
    When I transform it into our Content Store format
    Then the entity should be replace by unicode codepoint <codepoint>

  Examples:
    | entity | codepoint |
    | &euro; | 0x20AC    |
    | &nbsp; | 0x00A0    |

  @Technical
  Scenario Outline: Namespaces are ignored
    Given I have body text in Methode XML like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                       | after       |
    | <p v:vs="\|1\|" v:n="15" v:idx="11">Text</p> | <p>Text</p> |

  Scenario Outline: External hyperlinks are processed
    Given I have an "external hyperlink" in a Methode article body like <before>
    When I transform it into our Content Store format
    Then the hyperlink should be like <after>

  Examples:
    | before                                                         | after                                                         |
    # Text within the a tag is preserved:
    | <a href="http://www.google.com">google.com</a>                 | <a href="http://www.google.com">google.com</a>                |
    # Links within other tags are preserved:
    | <h1><a href="http://www.google.com">google.com</a></h1>        | <h1><a href="http://www.google.com">google.com</a></h1>       |
    # Title attribute is preserved:
    | <a href="http://www.google.com" title="Google">google.com</a>  | <a href="http://www.google.com" title="Google">google.com</a> |
    # Target attribute is dropped:
    | <a href="http://www.google.com" target="_blank">google.com</a> | <a href="http://www.google.com">google.com</a>                |
    # Alt attribute is preserved:
    | <a href="http://www.google.com" alt="Google">google.com</a>    | <a href="http://www.google.com" alt="Google">google.com</a>   |

  @Technical
  Scenario Outline: Relative hyperlinks are processed
    Given I have a "relative hyperlink" in a Methode article body like <before>
    When I transform it into our Content Store format
    Then the hyperlink should be like <after>

  Examples:
    | before                                              | after                                              |
    | <a href="../home/uk">google.com</a>                 | <a href="../home/uk">google.com</a>                |
    | <a href="../home/uk" title="Google">google.com</a>  | <a href="../home/uk" title="Google">google.com</a> |
    | <a href="../home/uk" target="_blank">google.com</a> | <a href="../home/uk">google.com</a>                |
    | <a href="../home/uk" alt="Google">google.com</a>    | <a href="../home/uk" alt="Google">google.com</a>   |

  @Technical
  Scenario Outline: Mailto hyperlinks are processed
    Given I have a "mailto hyperlink" in a Methode article body like <before>
    When I transform it into our Content Store format
    Then the hyperlink should be like <after>

  Examples:
    | before                                                               | after                                                               |
    | <a href="mailto:lionel.barber@ft.com">google.com</a>                 | <a href="mailto:lionel.barber@ft.com">google.com</a>                |
    | <a href="mailto:lionel.barber@ft.com" title="Google">google.com</a>  | <a href="mailto:lionel.barber@ft.com" title="Google">google.com</a> |
    | <a href="mailto:lionel.barber@ft.com" target="_blank">google.com</a> | <a href="mailto:lionel.barber@ft.com">google.com</a>                |
    | <a href="mailto:lionel.barber@ft.com" alt="Google">google.com</a>    | <a href="mailto:lionel.barber@ft.com" alt="Google">google.com</a>   |


  @Technical
  Scenario Outline: Anchors are removed
    Given I have an "anchor" in a Methode article body like <before>
    When I transform it into our Content Store format
    Then the hyperlink should be like <after>

  Examples:
    | before                                                                        | after                               |
    | <p><a name="top">1. google.com</a></p>                                        | <p>1. google.com</p>                |
    | <p><a name="top" type="boo">3. example.com</a></p>                            | <p>3. example.com</p>               |
    | <p><a name="top" type="slideshow">4. example.com</a><a name="foo">Foo</a></p> | <p>Foo</p>                          |
    | <p><a name="top">5. example.com</a><a name="foo">Foo</a></p>                  | <p>5. example.comFoo</p>            |
    | <p><a id="top">1. google.com</a></p>                                          | <p>1. google.com</p>                |
    | <p><a id="top" type="boo">2. example.com</a></p>                              | <p>2. example.com</p>               |
    | <p><a id="top" type="slideshow">4. example.com</a><a name="foo">Foo</a></p>   | <p>Foo</p>                          |
    | <p><a id="top">5. example.com</a><a name="foo">Foo</a></p>                    | <p>5. example.comFoo</p>            |


  @Technical
  Scenario Outline: Links to anchors only are removed
    Given I have an "link to an anchor" in a Methode article body like <before>
    When I transform it into our Content Store format
    Then the hyperlink should be like <after>

  Examples:
    | before                                                                         | after                               |
    | <p><a href="#top">1. google.com</a></p>                                        | <p>1. google.com</p>                |
    | <p><a href="#top" type="boo">2. example.com</a></p>                            | <p>2. example.com</p>               |
    | <p><a href="#top" type="slideshow">4. example.com</a><a name="foo">Foo</a></p> | <p>Foo</p>                          |
    | <p><a href="#top">5. example.com</a><a name="foo">Foo</a></p>                  | <p>5. example.comFoo</p>            |
    | <p><a href="" name="anchorname"/>Text</p>                                      | <p>Text</p>                         |
    | <p><a href="http://www.bbc.co.uk//article#menu">link</a></p>                   | <p><a href="http://www.bbc.co.uk//article#menu">link</a></p> |

  @Technical
  Scenario Outline: Images are removed
    Given I have an "img" in a Methode article body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                                                                                                                                                   | after                  |
    | <p>Embedded image.<img height="445" alt="Saloua Raouda Choucair's ‘Composition'" width="600" src="http://im.ft-static.com/content/images/7784185e-a888-11e2-8e5d-00144feabdc0.img"/></p> | <p>Embedded image.</p> |

Scenario Outline: Internal links are transformed
    Given I have an "internal link" in a Methode article body like <before>
    When I transform it into our Content Store format
    Then the body should be like <after>

  Examples:
    | before                                                                                            							| after                  																														    |
    # Bizarre pre-compound story link to even more bizarre content that does not exist in the content store should be preserved:
    | <p><a href="/FT/Production/Some Story.xml;uuid=446ae908-adf7-4809-a42e-d19a31af8c5d">Text</a></p> 							| <p><a href="/FT/Production/Some Story.xml;uuid=446ae908-adf7-4809-a42e-d19a31af8c5d">Text</a></p> 											    |
    # Bizarre link to content that exists in the content store should be converted into a <content>[...]</content> link:
    | <p><a href="/FT/Production/EOM::CompoundStory.xml;uuid=fbbee07f-5054-4a42-b596-64e0625d19a6">Text</a></p> 					| <p><content id="fbbee07f-5054-4a42-b596-64e0625d19a6" type="http://www.ft.com/ontology/content/Article">Text</content></p> 					    |
    # Bizarre link to content that exists in the content store should be converted into a <content>[...]</content> link, title preserved:
    | <p><a href="/FT/Production/EOM::CompoundStory.xml;uuid=fbbee07f-5054-4a42-b596-64e0625d19a6" title="ft.com">Text</a></p> 		| <p><content id="fbbee07f-5054-4a42-b596-64e0625d19a6" title="ft.com" type="http://www.ft.com/ontology/content/Article">Text</content></p> 	    |
    # Link to content that exists in the content store should be converted into a <content>[...]</content> link:
    | <p><a href="http://www.ft.com/cms/s/2/fbbee07f-5054-4a42-b596-64e0625d19a6.html" title="ft.com" >Text</a></p> 			 	| <p><content id="fbbee07f-5054-4a42-b596-64e0625d19a6" title="ft.com" type="http://www.ft.com/ontology/content/Article">Text</content></p> 	    |
    # Bizarre pre-compound story link to FT content not in the content store should be converted into an FT.com link:
    | <p><a href="/FT/Production/EOM::Story.xml;uuid=2d5f0ee9-09b3-4b09-af1b-e340276c7d6b" title="ft.com">Text</a></p> 				| <p><a href="http://www.ft.com/cms/s/2d5f0ee9-09b3-4b09-af1b-e340276c7d6b.html" title="ft.com">Text</a></p> 									    |
    # Link to FT content not in the content store is preserved:
    | <p><a href="http://www.ft.com/cms/s/2/2d5f0ee9-09b3-4b09-af1b-e340276c7d6b.html" title="ft.com">Text</a></p> 			 		| <p><a href="http://www.ft.com/cms/s/2/2d5f0ee9-09b3-4b09-af1b-e340276c7d6b.html" title="ft.com">Text</a></p> 									    |
    # The anchor part of an FT link is removed:
    | <p><a href="http://www.ft.com/cms/s/2/2d5f0ee9-09b3-4b09-af1b-e340276c7d6b.html#slide0" title="ft.com">Text</a></p> 			 		| <p><a href="http://www.ft.com/cms/s/2/2d5f0ee9-09b3-4b09-af1b-e340276c7d6b.html" title="ft.com">Text</a></p> 									    |
    # International link is no longer international, and the anchor part of it is removed:
    | <p><a href="http://www.ft.com/intl/cms/s/2/2d5f0ee9-09b3-4b09-af1b-e340276c7d6b.html#slide0" title="ft.com">Text</a></p> 			 		| <p><a href="http://www.ft.com/cms/s/2/2d5f0ee9-09b3-4b09-af1b-e340276c7d6b.html" title="ft.com">Text</a></p> 									    |
    # International link to a PDF is no longer international, title is preserved, target is dropped:
    | <p><a href="http://www.ft.com/intl/cms/5e231aca-a42b-11e1-a701-00144feabdc0.pdf" title="Open Report" target="_blank">Gartner Report</a></p>  | <p><a href="http://www.ft.com/cms/5e231aca-a42b-11e1-a701-00144feabdc0.pdf" title="Open Report">Gartner Report</a></p> 		    |
    # Query parameters are dropped:
    | <p><a href="http://www.ft.com/cms/s/2/2d5f0ee9-09b3-4b09-af1b-e340276c7d6b.html?siteedition=uk&amp;siteedition=uk" title="ft.com">Text</a></p> 			 		| <p><a href="http://www.ft.com/cms/s/2/2d5f0ee9-09b3-4b09-af1b-e340276c7d6b.html" title="ft.com">Text</a></p> 	|
    # International link is no longer international, target is dropped:
    | <p><a href="http://www.ft.com/intl/cms/s/5e231aca-a42b-11e1-a701-00144feabdc0.html" title="Title text" target="_blank">Link with intl and suffix</a></p> | <p><a href="http://www.ft.com/cms/s/5e231aca-a42b-11e1-a701-00144feabdc0.html" title="Title text">Link with intl and suffix</a></p> |
    # Slideshow link is converted into a regular link with slide0 added, data-asset-type attribute is added, data-embedded attribute is added, and title attribute is preserved:
    | <p><a href="/FT/Content/Companies/Stories/Live/Copy%20of%20PlainSlideshow.gallery.xml?uuid=5e231aca-a42b-11e1-a701-00144feabdc0" type="slideshow" dtxInsert="slideshow" title="Title text" target="_blank"><DIHeadlineCopy>Link with just suffix</DIHeadlineCopy></a></p> | <p><a data-asset-type="slideshow" data-embedded="true" href="http://www.ft.com/cms/s/5e231aca-a42b-11e1-a701-00144feabdc0.html#slide0" title="Title text"/></p> |
    # Slideshow link is converted into a regular link with slide0 added, data-asset-type attribute is added, data-embedded attribute is added, and alt attribute is dropped:
    | <p><a href="/FT/Content/Companies/Stories/Live/Copy%20of%20PlainSlideshow.gallery.xml?uuid=5e231aca-a42b-11e1-a701-00144feabdc0" type="slideshow" dtxInsert="slideshow" alt="Alt text" target="_blank"><DIHeadlineCopy>Link with just suffix</DIHeadlineCopy></a></p> | <p><a data-asset-type="slideshow" data-embedded="true" href="http://www.ft.com/cms/s/5e231aca-a42b-11e1-a701-00144feabdc0.html#slide0"/></p> |
    # Slideshow-like link without type="slideshow" is treated like a regular link:
    | <p><a href="/FT/Content/Companies/Stories/Live/Copy%20of%20PlainSlideshow.gallery.xml?uuid=5e231aca-a42b-11e1-a701-00144feabdc0" title="Title text" dtxInsert="slideshow" target="_blank"><DIHeadlineCopy>Link with just suffix</DIHeadlineCopy></a></p> | <p><a href="http://www.ft.com/cms/s/5e231aca-a42b-11e1-a701-00144feabdc0.html" title="Title text">Link with just suffix</a></p> |
    # Slideshow link is converted into a regular link with #slide0 added, data-asset-type attribute is added, data-embedded attribute is added, and query parameter is preserved:
    | <p><a href="/FT/Content/Companies/Stories/Live/Copy%20of%20PlainSlideshow.gallery.xml?uuid=5e231aca-a42b-11e1-a701-00144feabdc0&amp;query=value" type="slideshow" dtxInsert="slideshow" title="Title text" target="_blank"><DIHeadlineCopy>Link with just suffix</DIHeadlineCopy></a></p> | <p><a data-asset-type="slideshow" data-embedded="true" href="http://www.ft.com/cms/s/5e231aca-a42b-11e1-a701-00144feabdc0.html#slide0?query=value" title="Title text"/></p> |
    # Slideshow link is converted into a regular link with #slide0 added, data-asset-type attribute is added, data-embedded attribute is added, and query parameters are preserved:
    | <p><a href="/FT/Content/Companies/Stories/Live/Copy%20of%20PlainSlideshow.gallery.xml?uuid=5e231aca-a42b-11e1-a701-00144feabdc0&amp;query=value&amp;kartik=patel" type="slideshow" dtxInsert="slideshow" title="Title text" target="_blank"><DIHeadlineCopy>Link with just suffix</DIHeadlineCopy></a></p> | <p><a data-asset-type="slideshow" data-embedded="true" href="http://www.ft.com/cms/s/5e231aca-a42b-11e1-a701-00144feabdc0.html#slide0?query=value&amp;kartik=patel" title="Title text"/></p> |
    # International link is no longer international, and query parameter is removed:
    | <p>An ft.com page: <a href="http://www.ft.com/intl/cms/ee08dbdc-cd25-11de-a748-00144feabdc0.html?hello" title="Title" target="_blank">Link with intl and param</a></p> | <p>An ft.com page: <a href="http://www.ft.com/cms/ee08dbdc-cd25-11de-a748-00144feabdc0.html" title="Title">Link with intl and param</a></p> |
    # Slideshow link is converted into a regular link with slide0 added, data-asset-type attribute is added, data-embedded attribute is added, and empty title attribute is removed:
    | <p><a href="/FT/Content/Companies/Stories/Live/Copy%20of%20PlainSlideshow.gallery.xml?uuid=5e231aca-a42b-11e1-a701-00144feabdc0" type="slideshow" dtxInsert="slideshow" title="" target="_blank"><DIHeadlineCopy>Link with just suffix</DIHeadlineCopy></a></p> | <p><a data-asset-type="slideshow" data-embedded="true" href="http://www.ft.com/cms/s/5e231aca-a42b-11e1-a701-00144feabdc0.html#slide0"/></p> |

Scenario Outline: Inline image links are transformed
  Given I have an "inline image link" in a Methode article body like <before>
  When I transform it into our Content Store format
  Then the body should be like <after>

  Examples:
    | before                                                                                                                                                                                                               | after                                                                                                                               |
    # Link to an image set that exists in the content store should be converted into a <content>[...]</content> link and all the attributes and any text between the <web-inline-picture> opening and closing tags should be dropped:
    | <p><web-inline-picture fileref="/FT/Graphics/Online/Master_2048x1152/2014/01/img26.jpg?uuid=3b630b4a-4d51-11e4-a7d4-002128161462" alt="All attributes will be dropped">Text will be dropped</web-inline-picture></p> | <p><content data-embedded="true" id="3b630b4a-4d51-11e4-39b2-97bbf262bf2b" type="http://www.ft.com/ontology/content/ImageSet"/></p> |
