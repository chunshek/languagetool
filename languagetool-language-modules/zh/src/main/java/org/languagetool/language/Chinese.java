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
package org.languagetool.language;

import org.jetbrains.annotations.NotNull;
import org.languagetool.Language;
import org.languagetool.UserConfig;
import org.languagetool.languagemodel.LanguageModel;
import org.languagetool.languagemodel.LuceneLanguageModel;
import org.languagetool.rules.*;
import org.languagetool.rules.zh.*;
//import org.languagetool.rules.zh.ChineseConfusionProbabilityRule;
import org.languagetool.tagging.Tagger;
import org.languagetool.tagging.zh.ChineseTagger;
import org.languagetool.tokenizers.SentenceTokenizer;
import org.languagetool.tokenizers.Tokenizer;
import org.languagetool.tokenizers.zh.ChineseSentenceTokenizer;
import org.languagetool.tokenizers.zh.ChineseWordTokenizer;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Chinese extends Language implements AutoCloseable {

  private LuceneLanguageModel languageModel;

  @Override
  public String getShortCode() {
    return "zh";
  }

  @Override
  public String getName() {
    return "Chinese";
  }

  @Override
  public String[] getCountries() {
    return new String[] { "", "CN", "TW" };
  }

  @Override
  public Contributor[] getMaintainers() {
    return new Contributor[] {
      new Contributor("Tao Lin"),
      new Contributor("Chunshek Chan")
    };
  }

  @Override
  public List<Rule> getRelevantRules(ResourceBundle messages, UserConfig userConfig, Language motherTongue, List<Language> altLanguages) throws IOException {
    List<Rule> rules = new ArrayList<>();
    rules.add(new ChineseVariantCoherencyRule(messages, this));
    return rules;
  }

  @NotNull
  @Override
  public Tagger createDefaultTagger() {
    return new ChineseTagger();
  }

  @Override
  public Tokenizer createDefaultWordTokenizer() {
    return new ChineseWordTokenizer();
  }

  @Override
  public SentenceTokenizer createDefaultSentenceTokenizer() {
    return new ChineseSentenceTokenizer();
  }

  /** @since 3.1 */
  @Override
  public synchronized LanguageModel getLanguageModel(File indexDir) throws IOException {
    return initLanguageModel(indexDir, languageModel);
  }

  /** @since 3.1 */
  @Override
  public List<Rule> getRelevantLanguageModelRules(ResourceBundle messages, LanguageModel languageModel, UserConfig userConfig) throws IOException {
    return Arrays.asList(
      new ChineseConfusionProbabilityRule(messages, languageModel, this)
    );
  }

  /** @since 5.3 */
  public String getOpeningDoubleQuote() {
    return "“";
  }

  /** @since 5.3 */
  public String getClosingDoubleQuote() {
    return "”";
  }

  /** @since 5.3 */
  @Override
  public String getOpeningSingleQuote() {
    return "‘";
  }

  /** @since 5.3 */
  @Override
  public String getClosingSingleQuote() {
    return "’";
  }

  /** @since 5.3 */
  @Override
  public boolean isAdvancedTypographyEnabled() {
    return true;
  }

  /** @since 5.3 */
  @Override
  public String toAdvancedTypography (String input) {
    String output = super.toAdvancedTypography(input);

    // changing "smart quote" used in English contractions to apostrophes
    // since the "right single quotation mark" appears as full-width
    // in most CJK fonts
    output = output.replaceAll("([\\p{script=LATIN}\\d-])’([\\p{script=LATIN}«])", "$1'$2");
    return output;
  }

  /**
   * Closes the language model, if any.
   * @since 3.1
   */
  @Override
  public void close() throws Exception {
    if (languageModel != null) {
      languageModel.close();
    }
  }


}
