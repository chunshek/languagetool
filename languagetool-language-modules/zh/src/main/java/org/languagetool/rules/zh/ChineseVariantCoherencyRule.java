/* LanguageTool, a natural language style checker
 * Copyright (C) 2015 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.rules.zh;

//import java.lang.Math;
import java.io.IOException;
//import java.io.InputStream;
import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
import java.util.List;
//import java.util.Map;
//import java.util.Objects;
import java.util.ResourceBundle;
//import java.util.Scanner;
//import java.util.Set;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
//import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.SuggestionWithMessage;
import org.languagetool.rules.TextLevelRule;
import org.languagetool.rules.zh.ChineseConverter;
import org.languagetool.rules.zh.ChineseVariantDetector;

/**
 * @since 5.3
 */
 // CAUTION:
 // {@link org.languagetool.rules.SameRuleGroupFilter} has been edited
 // so it will correctly leave in consecutive tokens matched on this rule.
 // The original filter caused skipping when two or more adjacent tokens
 // generate a RuleMatch under the same Rule ID.
 // Given that Chinese language text has no spaces between words,
 // this fix was necessary.
public class ChineseVariantCoherencyRule extends TextLevelRule {

  private final Language lang;

  private final String TAIWAN_CHINESE = "Chinese (Taiwan)";
  private final String CHINA_CHINESE = "Chinese (China)";

  private static final ChineseConverter simplifiedCharactersConverter = new ChineseConverter("zh/SimplifiedCharacters.txt");
  private static final ChineseConverter traditionalCharactersConverter = new ChineseConverter("zh/TraditionalCharacters.txt");
  private static final ChineseConverter kangxiRadicalsConverter = new ChineseConverter("zh/KangxiRadicals.txt");
  private static final ChineseConverter cjkCompatibilityIdeographsConverter = new ChineseConverter("zh/CJKCompatibilityIdeographs.txt");

  public ChineseVariantCoherencyRule(ResourceBundle messages, Language lang) throws IOException {
    super(messages);
    this.lang = lang;
  }

  @Override
  public String getId() {
    return "ZH_VARIANT_COHERENCY";
  }

  @Override
  public String getDescription() {
    String description = "检查文字的繁简写法是否一致";
    if (this.lang.getName() == TAIWAN_CHINESE) {
      description = "檢查文字的正簡寫法是否一致";
    }
    return description;  // shown in the configuration dialog
  }

  @Override
  public RuleMatch[] match(List<AnalyzedSentence> sentences) throws IOException {
    List<RuleMatch> ruleMatches = new ArrayList<>();
    String variant = "";
    int pos = 0;
    int startPos = 0;
    int endPos = 0;
    String msg = "";
    String replacement = "";
    List<SuggestionWithMessage> suggestions = new ArrayList<>();

    // Respect user's preference on Taiwan or China variant.
    // Otherwise, determine predominant variant used in text.

    switch (this.lang.getName()) {
      case TAIWAN_CHINESE: case CHINA_CHINESE:
        variant = this.lang.getName();
      break;
      default:
        variant = (new ChineseVariantDetector(sentences)).getVariant();
      break;
    }

    for (AnalyzedSentence sentence : sentences) {
      AnalyzedTokenReadings[] tokens = sentence.getTokensWithoutWhitespace();
      for (AnalyzedTokenReadings token : tokens) {
        String t = token.getToken();

        // VARIANT-SPECIFIC CHECKS
        // Check against Simplified characters in Traditional settings,
        // or Traditional characters in Simplified settings.
        // Note: This process only checks the characters themselves.
        // It does not check for usage of region-specific terminology.

        startPos = pos + token.getStartPos();
        endPos = pos + token.getEndPos();

        if (variant == TAIWAN_CHINESE) {
          suggestions = simplifiedCharactersConverter.suggestVariants(t);
          replacement = suggestions.get(0).getSuggestion();
          if (!replacement.equals(t)) {
            msg = "『" + t + "』是簡體字詞，正體的寫法為<suggestion>" + replacement + "</suggestion>";
            RuleMatch ruleMatch = new RuleMatch(this, sentence, startPos, endPos, msg);
            if (suggestions.size() > 1) {
              for (int i = 1; i < suggestions.size(); i++) {
                ruleMatch.addSuggestedReplacement(suggestions.get(i).getSuggestion());
              }
            }
            ruleMatches.add(ruleMatch);
          }

        } else {
          suggestions = traditionalCharactersConverter.suggestVariants(t);
          replacement = suggestions.get(0).getSuggestion();
          if (!replacement.equals(t)) {
            msg = "‘" + t + "’是繁体字词，简体的写法为<suggestion>" + replacement + "</suggestion>";
            RuleMatch ruleMatch = new RuleMatch(this, sentence, startPos, endPos, msg);
            if (suggestions.size() > 1) {
              for (int i = 1; i < suggestions.size(); i++) {
                ruleMatch.addSuggestedReplacement(suggestions.get(i).getSuggestion());
              }
            }
            ruleMatches.add(ruleMatch);
          }

        } // end of variant-specific checks

        // NON-SPECIFIC CHECKS
        // Check against characters in the Kangxi Radicals block (U+2F00-2FDF)
        // and Unicode CJK Compatibility Ideographs blocks
        // (U+F900-FAFF, U+2F800-2FA1F).
        // Suggest replacements using standard characters.

        suggestions = kangxiRadicalsConverter.suggestVariants(t);
        replacement = suggestions.get(0).getSuggestion();
        if (!replacement.equals(t)) {
          if (variant == TAIWAN_CHINESE) {
            msg = "『" + t + "』用了 Unicode 康熙部首區的字元，如非必要，請改用中日韓統一表意文字：<suggestion>" + replacement + "</suggestion>";
          } else {
            msg = "‘" + t + "’用了 Unicode 康熙部首区的字符，如非必要，请改用中日韩统一表意文字：<suggestion>" + replacement + "</suggestion>";
          }
          RuleMatch ruleMatch = new RuleMatch(this, sentence, startPos, endPos, msg);
          if (suggestions.size() > 1) {
            for (int i = 1; i < suggestions.size(); i++) {
              ruleMatch.addSuggestedReplacement(suggestions.get(i).getSuggestion());
            }
          }
          ruleMatches.add(ruleMatch);
        }

        suggestions = cjkCompatibilityIdeographsConverter.suggestVariants(t);
        replacement = suggestions.get(0).getSuggestion();
        if (!replacement.equals(t)) {
          if (variant == TAIWAN_CHINESE) {
            msg = "『" + t + "』用了 Unicode 相容表意文字區的字元，請改用中日韓統一表意文字：<suggestion>" + replacement + "</suggestion>";
          } else {
            msg = "‘" + t + "’用了 Unicode 兼容表意文字区的字符，请改用中日韩统一表意文字：<suggestion>" + replacement + "</suggestion>";
          }
          RuleMatch ruleMatch = new RuleMatch(this, sentence, startPos, endPos, msg);
          if (suggestions.size() > 1) {
            for (int i = 1; i < suggestions.size(); i++) {
              ruleMatch.addSuggestedReplacement(suggestions.get(i).getSuggestion());
            }
          }
          ruleMatches.add(ruleMatch);
        }

        // end of non-specific checks
      }
      pos += sentence.getCorrectedTextLength();
    }
    return toRuleMatchArray(ruleMatches);
  }

  @Override
  public int minToCheckParagraph() {
    return -1;
  }

}
