/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.dev.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.rules.IncorrectExample;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.patterns.Element;
import org.languagetool.rules.patterns.PatternRule;

public class IndexerSearcherTest extends LuceneTestCase {

  private final File ruleFile = new File("src/rules/en/grammar.xml");
  private final Searcher errorSearcher = new Searcher();

  private IndexSearcher searcher;
  private Directory directory;

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    if (searcher != null) {
      searcher.close();
    }
    directory.close();
    super.tearDown();
  }

  // TODO: ignored as long as it doesn't work 100%
  public void IGNOREtestAllRules() throws Exception {
    directory = newDirectory();
    //directory = FSDirectory.open(new File("/tmp/lucenetest"));
    // TODO: make this work for all languages
    final Language language = Language.ENGLISH;
    final JLanguageTool lt = new JLanguageTool(language);
    lt.activateDefaultPatternRules();
    System.out.println("Creating index...");
    createIndex(language, lt);
    System.out.println("Done.");

    int ruleCounter = 0;
    int ruleProblems = 0;
    int relaxedQueryCount = 0;
    searcher = new IndexSearcher(directory);
    final List<Rule> rules = lt.getAllActiveRules();
    for (Rule rule : rules) {
      if (rule instanceof PatternRule && !rule.isDefaultOff()) {
        final PatternRule patternRule = (PatternRule) rule;
        try {
          ruleCounter++;
          final SearcherResult searcherResult = errorSearcher.findRuleMatchesOnIndex(patternRule, language, searcher);
          if (searcherResult.isRelaxedQuery()) {
            relaxedQueryCount++;
          }
          final List<MatchingSentence> matchingSentences = searcherResult.getMatchingSentences();
          boolean foundExpectedMatch = false;
          for (MatchingSentence matchingSentence : matchingSentences) {
            final List<RuleMatch> ruleMatches = matchingSentence.getRuleMatches();
            final List<String> ruleMatchIds = getRuleMatchIds(ruleMatches);
            if (ruleMatchIds.contains(getFullId(patternRule))) {
              // TODO: there can be more than one expected match, can't it?
              foundExpectedMatch = true;
              break;
            }
          }
          if (!foundExpectedMatch) {
            System.out.println("Error: No match found for " + patternRule);
            System.out.println("Query   : " + searcherResult.getPossiblyRelaxedQuery());
            System.out.println("Matches : " + matchingSentences);
            System.out.println("Examples: " + rule.getIncorrectExamples());
            System.out.println();
            ruleProblems++;
          }
        } catch (NullPointerException e) {
          // happens when a rule has only inflected tokens or only tokens with exceptions
          System.out.println("NullPointerException searching for rule " + getFullId(patternRule));
          ruleProblems++;
        }
      }
    }
    System.out.println(language + ": problems: " + ruleProblems + ", total rules: " + ruleCounter);
    System.out.println(language + ": relaxedQueryCount: " + relaxedQueryCount);

  }

  private String getFullId(PatternRule patternRule) {
    return patternRule.getId() + "[" + patternRule.getSubId() + "]";
  }

  private List<String> getRuleMatchIds(List<RuleMatch> ruleMatches) {
    final List<String> ids = new ArrayList<String>();
    for (RuleMatch ruleMatch : ruleMatches) {
      if (ruleMatch.getRule() instanceof PatternRule) {
        final PatternRule patternRule = (PatternRule) ruleMatch.getRule();
        ids.add(getFullId(patternRule));
      }
    }
    return ids;
  }

  private void createIndex(Language language, JLanguageTool lt) throws IOException {
    final Indexer indexer = new Indexer(directory, language);
    try {
      final List<Rule> rules = lt.getAllActiveRules();
      for (Rule rule : rules) {
        if (rule instanceof PatternRule && !rule.isDefaultOff()) {
          final PatternRule patternRule = (PatternRule) rule;
          //final List<String> correctExamples = rule.getCorrectExamples();   // TODO: also check non-match for correct sentences
          final List<IncorrectExample> incorrectExamples = rule.getIncorrectExamples();
          final Document doc = new Document();
          doc.add(new Field("ruleId", getFullId(patternRule), Field.Store.YES, Field.Index.NOT_ANALYZED));
          for (IncorrectExample incorrectExample : incorrectExamples) {
            final String example = incorrectExample.getExample().replaceAll("</?marker>", "");
            doc.add(new Field(PatternRuleQueryBuilder.FIELD_NAME, example, Field.Store.YES, Field.Index.ANALYZED));
          }
          indexer.add(doc);
        }
      }
    } finally {
      indexer.close();
    }
  }

  /** for manual debugging only */
  public void IGNOREtestForDebugging() throws Exception {
    // Note that the second sentence ends with "lid" instead of "lids" (the inflated one)
    createIndex("I thin so");
    final PatternRule rule = getRule("I_THIN", new File("src/rules/en/grammar.xml"));
    final SearcherResult searcherResult = errorSearcher.findRuleMatchesOnIndex(rule, Language.GERMAN, searcher);
    System.out.println("Matches: " + searcherResult.getMatchingSentences());
    assertEquals(1, searcherResult.getMatchingSentences().size());
  }

  public void testIndexerSearcherWithEnglish() throws Exception {
    // Note that the second sentence ends with "lid" instead of "lids" (the inflated one)
    createIndex("How to move back and fourth from linux to xmb? Calcium deposits on eye lid.");
    SearcherResult searcherResult =
            errorSearcher.findRuleMatchesOnIndex(getRule("BACK_AND_FOURTH"), Language.ENGLISH, searcher);
    assertEquals(2, searcherResult.getCheckedSentences());
    assertEquals(false, searcherResult.isResultIsTimeLimited());
    assertEquals(1, searcherResult.getMatchingSentences().size());
    assertEquals(false, searcherResult.isRelaxedQuery());

    searcherResult = errorSearcher.findRuleMatchesOnIndex(getRule("EYE_BROW"), Language.ENGLISH, searcher);
    assertEquals(2, searcherResult.getCheckedSentences());
    assertEquals(false, searcherResult.isResultIsTimeLimited());
    assertEquals(1, searcherResult.getMatchingSentences().size());
    assertEquals(true, searcherResult.isRelaxedQuery());

    searcherResult = errorSearcher.findRuleMatchesOnIndex(getRule("ALL_OVER_THE_WORD"), Language.ENGLISH, searcher);
    assertEquals(2, searcherResult.getCheckedSentences());
    assertEquals(false, searcherResult.isResultIsTimeLimited());
    assertEquals(0, searcherResult.getMatchingSentences().size());
    assertEquals(false, searcherResult.isRelaxedQuery());

    try {
      errorSearcher.findRuleMatchesOnIndex(getRule("Invalid Rule Id"), Language.ENGLISH, searcher);
      fail("Exception should be thrown for invalid rule id.");
    } catch (PatternRuleNotFoundException expected) {}
  }

  private PatternRule getRule(String ruleId) throws IOException {
    return errorSearcher.getRuleById(ruleId, ruleFile);
  }

  private PatternRule getRule(String ruleId, File grammarFile) throws IOException {
    return errorSearcher.getRuleById(ruleId, grammarFile);
  }

  public void testWithNewRule() throws Exception {
    createIndex("How to move back and fourth from linux to xmb?");
    final Searcher errorSearcher = new Searcher();
    final List<Element> elements = Arrays.asList(
            new Element("move", false, false, false),
            new Element("back", false, false, false)
    );
    final PatternRule rule1 = new PatternRule("RULE1", Language.ENGLISH, elements, "desc", "msg", "shortMsg");
    final IndexSearcher indexSearcher = new IndexSearcher(directory);
    try {
      final SearcherResult searcherResult = errorSearcher.findRuleMatchesOnIndex(rule1, Language.ENGLISH, indexSearcher);
      assertEquals(1, searcherResult.getCheckedSentences());
      assertEquals(1, searcherResult.getMatchingSentences().size());
      assertEquals(false, searcherResult.isRelaxedQuery());
      final List<RuleMatch> ruleMatches = searcherResult.getMatchingSentences().get(0).getRuleMatches();
      assertEquals(1, ruleMatches.size());
      final Rule rule = ruleMatches.get(0).getRule();
      assertEquals("RULE1", rule.getId());
    } finally {
      indexSearcher.close();
    }
  }

  public void testWithRegexRule() throws Exception {
    createIndex("How to move back and fourth from linux to xmb?");
    final Searcher errorSearcher = new Searcher();
    final List<Element> elements = Arrays.asList(
            new Element("move", false, false, false),
            new Element("forth|back", false, true, false)
    );
    final PatternRule rule1 = new PatternRule("RULE1", Language.ENGLISH, elements, "desc", "msg", "shortMsg");
    final IndexSearcher indexSearcher = new IndexSearcher(directory);
    try {
      final SearcherResult searcherResult = errorSearcher.findRuleMatchesOnIndex(rule1, Language.ENGLISH, indexSearcher);
      assertEquals(1, searcherResult.getCheckedSentences());
      assertEquals(1, searcherResult.getMatchingSentences().size());
      assertEquals(false, searcherResult.isRelaxedQuery());
      final List<RuleMatch> ruleMatches = searcherResult.getMatchingSentences().get(0).getRuleMatches();
      assertEquals(1, ruleMatches.size());
      final Rule rule = ruleMatches.get(0).getRule();
      assertEquals("RULE1", rule.getId());
    } finally {
      indexSearcher.close();
    }
  }

  public void testApostropheElement() throws Exception {
    createIndex("Daily Bleed's Anarchist Encyclopedia");
    final List<Element> elements1 = Arrays.asList(
            new Element("Bleed", false, false, false),
            new Element("'", false, false, false),
            new Element("s", false, false, false)
    );
    final PatternRule rule1 = new PatternRule("RULE1", Language.ENGLISH, elements1, "desc", "msg", "shortMsg");

    final List<Element> elements2 = Arrays.asList(
            new Element("Bleed", false, false, false),
            new Element("'", false, false, false),
            new Element("x", false, false, false)
    );
    final PatternRule rule2 = new PatternRule("RULE", Language.ENGLISH, elements2, "desc", "msg", "shortMsg");

    final IndexSearcher indexSearcher = new IndexSearcher(directory);
    try {
      final SearcherResult searcherResult1 = errorSearcher.findRuleMatchesOnIndex(rule1, Language.ENGLISH, indexSearcher);
      assertEquals(1, searcherResult1.getMatchingSentences().size());
      final List<RuleMatch> ruleMatches = searcherResult1.getMatchingSentences().get(0).getRuleMatches();
      assertEquals(1, ruleMatches.size());
      final Rule rule = ruleMatches.get(0).getRule();
      assertEquals("RULE1", rule.getId());

      final SearcherResult searcherResult2 = errorSearcher.findRuleMatchesOnIndex(rule2, Language.ENGLISH, indexSearcher);
      assertEquals(0, searcherResult2.getMatchingSentences().size());

    } finally {
      indexSearcher.close();
    }
  }

  public void testWithException() throws Exception {
    createIndex("How to move back and fourth from linux to xmb?");
    final Searcher errorSearcher = new Searcher();
    final Element exceptionElem = new Element("forth|back", false, true, false);
    exceptionElem.setStringPosException("exception", false, false, false, false, false, "POS", false, false);
    final List<Element> elements = Arrays.asList(
            new Element("move", false, false, false),
            exceptionElem
    );
    final PatternRule rule1 = new PatternRule("RULE1", Language.ENGLISH, elements, "desc", "msg", "shortMsg");
    final IndexSearcher indexSearcher = new IndexSearcher(directory);
    try {
      final SearcherResult searcherResult = errorSearcher.findRuleMatchesOnIndex(rule1, Language.ENGLISH, indexSearcher);
      assertEquals(1, searcherResult.getCheckedSentences());
      assertEquals(1, searcherResult.getMatchingSentences().size());
      assertEquals(true, searcherResult.isRelaxedQuery());
      final List<RuleMatch> ruleMatches = searcherResult.getMatchingSentences().get(0).getRuleMatches();
      assertEquals(1, ruleMatches.size());
      final Rule rule = ruleMatches.get(0).getRule();
      assertEquals("RULE1", rule.getId());
    } finally {
      indexSearcher.close();
    }
  }

  public void testNegatedMatchAtSentenceStart() throws Exception {
    createIndex("How to move?");
    final Searcher errorSearcher = new Searcher();
    final Element negatedElement = new Element("Negated", false, false, false);
    negatedElement.setNegation(true);
    final List<Element> elements = Arrays.asList(
            negatedElement,
            new Element("How", false, false, false)
    );
    final PatternRule rule1 = new PatternRule("RULE1", Language.ENGLISH, elements, "desc", "msg", "shortMsg");
    final IndexSearcher indexSearcher = new IndexSearcher(directory);
    try {
      final SearcherResult searcherResult = errorSearcher.findRuleMatchesOnIndex(rule1, Language.ENGLISH, indexSearcher);
      assertEquals(1, searcherResult.getCheckedSentences());
      assertEquals(1, searcherResult.getMatchingSentences().size());
      assertEquals(true, searcherResult.isRelaxedQuery());
      final List<RuleMatch> ruleMatches = searcherResult.getMatchingSentences().get(0).getRuleMatches();
      assertEquals(1, ruleMatches.size());
      final Rule rule = ruleMatches.get(0).getRule();
      assertEquals("RULE1", rule.getId());
    } finally {
      indexSearcher.close();
    }
  }

  public void testWithOneElementWithException() throws Exception {
    createIndex("How to move back and fourth from linux to xmb?");
    final Searcher errorSearcher = new Searcher();
    final Element exceptionElem = new Element("forth|back", false, true, false);
    exceptionElem.setStringPosException("exception", false, false, false, false, false, "POS", false, false);
    final List<Element> elements = Arrays.asList(
            exceptionElem
    );
    final PatternRule rule1 = new PatternRule("RULE1", Language.ENGLISH, elements, "desc", "msg", "shortMsg");
    final IndexSearcher indexSearcher = new IndexSearcher(directory);
    try {
      final SearcherResult searcherResult = errorSearcher.findRuleMatchesOnIndex(rule1, Language.ENGLISH, indexSearcher);
      assertEquals(1, searcherResult.getCheckedSentences());
      assertEquals(1, searcherResult.getMatchingSentences().size());
      assertEquals(true, searcherResult.isRelaxedQuery());
      final List<RuleMatch> ruleMatches = searcherResult.getMatchingSentences().get(0).getRuleMatches();
      assertEquals(1, ruleMatches.size());
      final Rule rule = ruleMatches.get(0).getRule();
      assertEquals("RULE1", rule.getId());
    } finally {
      indexSearcher.close();
    }
  }

  private void createIndex(String content) throws IOException {
    directory = newDirectory();
    //directory = FSDirectory.open(new File("/tmp/lucenetest"));
    Indexer.run(content, directory, Language.ENGLISH, false);
    searcher = new IndexSearcher(directory);
  }

  /*public void testForManualDebug() throws Exception {
    createIndex("How to move back and fourth from linux to xmb?");
    final Searcher errorSearcher = new Searcher();
    SearcherResult searcherResult = errorSearcher.findRuleMatchesOnIndex(getRule("IS_EVEN_WORST"), Language.ENGLISH, searcher);
    System.out.println(searcherResult.getCheckedSentences());
    System.out.println(searcherResult.isRelaxedQuery());
  }*/

}
