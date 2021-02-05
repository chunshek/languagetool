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

import java.lang.Character;
import java.lang.Math;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.IntStream;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.SuggestionWithMessage;
import org.languagetool.rules.TextLevelRule;
import org.languagetool.rules.zh.ChineseVariantDetector;

/**
 * @since 5.3
 */
public class ChineseConverter {

  //private final Language lang;

  //private static final Map<String, List<SuggestionWithMessage>> simplifiedCharacters = loadConversionMap("/zh/SimplifiedCharacters.txt");
  //private static final Map<String, List<SuggestionWithMessage>> traditionalCharacters = loadConversionMap("/zh/TraditionalCharacters.txt");
  //private static final Map<String, List<SuggestionWithMessage>> kangxiRadicals = loadConversionMap("/zh/KangxiRadicals.txt");
  //private static final Map<String, List<SuggestionWithMessage>> cjkCompatibilityIdeographs = loadConversionMap("/zh/CJKCompatibilityIdeographs.txt");

  private Map<String, List<SuggestionWithMessage>> conversionMap;
  private String regex;

  public ChineseConverter(String path) {
    conversionMap = loadConversionMap(path);
    regex = ".*(" + String.join("|", conversionMap.keySet()) + ").*";
  }

  public List<SuggestionWithMessage> suggestVariants(String token) {
    int nVariants = 1;
    int tokenLength = 0;
    List<SuggestionWithMessage> output = new ArrayList<>();
    List<List<SuggestionWithMessage>> suggestions = new ArrayList<>();

    if (!token.matches(regex)) { // no alternatives to suggest
      output.add(new SuggestionWithMessage(token));
      return output;
    }

    // using IntStream.codePoints() to capture characters in the SIP
    token.codePoints().forEach(cp -> {
      List<SuggestionWithMessage> s = conversionMap.get(new String(Character.toChars(cp)));
      if (s == null) {
        s = new ArrayList<>();
        s.add(new SuggestionWithMessage(new String(Character.toChars(cp))));
      }
      suggestions.add(s);
    });

    tokenLength = suggestions.size();
    for (int i = 0; i < tokenLength; i++) {
      nVariants = nVariants * suggestions.get(i).size();
    }

    String replacements[] = new String[nVariants];
    String messages[] = new String[nVariants];
    int groupSize = nVariants;

    for (int i = 0; i < tokenLength; i++) {
      for (int n = 0; n < nVariants; n++) {
        replacements[n] = (replacements[n] != null ? replacements[n] : "") + suggestions.get(i).get(Math.floorMod(Math.floorDiv(n, Math.floorDiv(groupSize, suggestions.get(i).size())), groupSize)).getSuggestion();
        messages[n] = (messages[n] != null ? messages[n] + "；" : "") + suggestions.get(i).get(Math.floorMod(Math.floorDiv(n, Math.floorDiv(groupSize, suggestions.get(i).size())), groupSize)).getMessage();
      }
      groupSize = Math.floorDiv(groupSize, suggestions.get(i).size());
    }

    for (int n = 0; n < nVariants; n++) {
      output.add(new SuggestionWithMessage(replacements[n], messages[n]));
    }
    return output;

  }

  private static Map<String, List<SuggestionWithMessage>> loadConversionMap(String path) {
    //boolean includeMapping;
    InputStream stream = JLanguageTool.getDataBroker().getFromResourceDirAsStream(path);
    Map<String, List<SuggestionWithMessage>> map = new HashMap<>();
    try (Scanner scanner = new Scanner(stream, "utf-8")) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.isEmpty() || line.charAt(0) == '#') { // # = comment
          continue;
        }
        String[] parts = line.split("=");
        if (parts.length != 2) {
          throw new RuntimeException("Could not load replacement data from: " + path + ". " + "Error in line '" + line + "', expected format 'word=replacement'");
        }
        if (parts[1].trim().isEmpty()) {
          throw new RuntimeException("Could not load replacement data from: " + path + ". " + "Error in line '" + line + "', replacement cannot be empty");
        }
        List<SuggestionWithMessage> suggestions = new ArrayList<>();
        String[] replacements = parts[1].split("\\|");
        for (String replacement : replacements) {
          SuggestionWithMessage sugg;
          if (replacement.contains(":")) {
            String[] suggestionParts = replacement.split(":");
            if (suggestionParts.length != 2) {
              throw new RuntimeException("Invalid format - use only one color character to separate suggestion from the message: " + line);
            }
            sugg = new SuggestionWithMessage(suggestionParts[0], suggestionParts[1]);
          } else {
            sugg = new SuggestionWithMessage(replacement);
          }
          suggestions.add(sugg);
        }

        map.put(parts[0], suggestions);

        // Exclude mappings where a single character maps to both itself and other alternatives.
        // These are likely due to poor machine conversions.
        // Use a different rule to deal with these cases.
        /*
        includeMapping = true;
        if (parts[0].length() == 1) {
          for (SuggestionWithMessage sugg : suggestions) {
            if (sugg.getSuggestion().equals(parts[0])) {
              includeMapping = false;
            }
          }
        }

        if (includeMapping) {
          map.put(parts[0], suggestions);
        }
        */
      }
    }
    return Collections.unmodifiableMap(map);
  }

}
