/* LanguageTool, a natural language style checker
 * Copyright (C) 2012 Daniel Naber (http://www.danielnaber.de)
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

import org.languagetool.Language;
import org.languagetool.UserConfig;
import org.languagetool.rules.Rule;
import org.languagetool.rules.*;
import org.languagetool.rules.zh.*;

import java.io.IOException;
import java.util.*;

public class TaiwanChinese extends Chinese {

  @Override
  public String getName() {
    return "Chinese (Taiwan)";
  }

  @Override
  public String[] getCountries() {
    return new String[]{ "TW" };
  }

  @Override
  public List<Rule> getRelevantRules(ResourceBundle messages, UserConfig userConfig, Language motherTongue, List<Language> altLanguages) throws IOException {
    List<Rule> rules = new ArrayList<>(super.getRelevantRules(messages, userConfig, motherTongue, altLanguages));
    return rules;
  }

  /** @since 5.3 */
  public String getOpeningDoubleQuote() {
    return "「";
  }

  /** @since 5.3 */
  public String getClosingDoubleQuote() {
    return "」";
  }

  /** @since 5.3 */
  @Override
  public String getOpeningSingleQuote() {
    return "『";
  }

  /** @since 5.3 */
  @Override
  public String getClosingSingleQuote() {
    return "』";
  }

}
