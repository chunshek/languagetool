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

import java.io.InputStream;
import java.lang.Character;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.languagetool.AnalyzedSentence;
import org.languagetool.JLanguageTool;

/**
 * @since 5.3
 */
public class ChineseVariantDetector {

  public final String TAIWAN_CHINESE = "Chinese (Taiwan)";
  public final String CHINA_CHINESE = "Chinese (China)";

  private static final List<char[]> simplifiedCharacters = loadCharacters("/zh/SimplifiedCharacters.txt");
  private static final List<char[]> traditionalCharacters = loadCharacters("/zh/TraditionalCharacters.txt");
  private String variant;

  public ChineseVariantDetector(List<AnalyzedSentence> sentences) {
    int nSimplifiedCharacters = 0;
    int nTraditionalCharacters = 0;

    for (AnalyzedSentence sentence : sentences) {
      String s = sentence.getText();
      for (int i = 0; i < s.length(); i++) {
        if (simplifiedCharacters.contains(Character.toChars(s.codePointAt(i)))) {
          nSimplifiedCharacters++;
        }
        if (traditionalCharacters.contains(Character.toChars(s.codePointAt(i)))) {
          nTraditionalCharacters++;
        }
      }
    }

    if (nTraditionalCharacters > nSimplifiedCharacters) {
      variant = TAIWAN_CHINESE;
    } else {
      variant = CHINA_CHINESE;
    }
  }

  public String getVariant() {
    return variant;
  }

  private static List<char[]> loadCharacters(String path) {
    InputStream stream = JLanguageTool.getDataBroker().getFromResourceDirAsStream(path);
    List<char[]> charset = new ArrayList<>();
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
        if (parts[0].length() == 1) {
          if (!parts[1].contains(parts[0])) {
            charset.add(Character.toChars(parts[0].codePointAt(0)));
          }
        }
      }
    }
    return charset;
  }

}
