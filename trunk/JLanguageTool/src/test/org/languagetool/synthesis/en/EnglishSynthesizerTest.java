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

package org.languagetool.synthesis.en;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.languagetool.AnalyzedToken;

public class EnglishSynthesizerTest extends TestCase {

  private final AnalyzedToken dummyToken(String tokenStr) {
    return new AnalyzedToken(tokenStr, tokenStr, tokenStr);
  }
  public final void testSynthesizeStringString() throws IOException {
    EnglishSynthesizer synth = new EnglishSynthesizer();
    assertEquals(synth.synthesize(dummyToken("blablabla"), 
        "blablabla").length, 0);
        
    assertEquals("[was, were]", Arrays.toString(synth.synthesize(dummyToken("be"), "VBD")));
    assertEquals("[presidents]", Arrays.toString(synth.synthesize(dummyToken("president"), "NNS")));
    assertEquals("[tested]", Arrays.toString(synth.synthesize(dummyToken("test"), "VBD")));
    assertEquals("[tested]", Arrays.toString(synth.synthesize(dummyToken("test"), "VBD", false)));
    //with regular expressions
    assertEquals("[tested]", Arrays.toString(synth.synthesize(dummyToken("test"), "VBD", true)));    
    assertEquals("[tested, testing]", Arrays.toString(synth.synthesize(dummyToken("test"), "VBD|VBG", true)));
    //with special indefinite article
    assertEquals("[a university, the university]", Arrays.toString(synth.synthesize(dummyToken("university"), "+DT", false)));
    assertEquals("[an hour, the hour]", Arrays.toString(synth.synthesize(dummyToken("hour"), "+DT", false)));
    assertEquals("[an hour]", Arrays.toString(synth.synthesize(dummyToken("hour"), "+INDT", false)));
  }

}
