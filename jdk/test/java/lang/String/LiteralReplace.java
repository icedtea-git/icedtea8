/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/* @test
 * @bug 8058779
 * @library /lib/testlibrary/
 * @build jdk.testlibrary.RandomFactory
 * @run testng LiteralReplace
 * @summary Basic tests of String.replace(CharSequence, CharSequence)
 * @key randomness
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;

import jdk.testlibrary.RandomFactory;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.fail;

public class LiteralReplace {

    @Test(dataProvider="sourceTargetReplacementExpected")
    public void testExpected(String source, String target,
             String replacement, String expected)
    {
        String canonical = canonicalReplace(source, target, replacement);
        if (!canonical.equals(expected)) {
            fail("Canonical: " + canonical + " != " + expected);
        }
        test0(source, target, replacement, expected);
    }

    @Test(dataProvider="sourceTargetReplacement")
    public void testCanonical(String source, String target,
            String replacement)
    {
        String canonical = canonicalReplace(source, target, replacement);
        test0(source, target, replacement, canonical);
    }

    private void test0(String source, String target, String replacement,
            String expected)
    {
        String result = source.replace(target, replacement);
        if (!result.equals(expected)) {
            fail(result + " != " + expected);
        }
    }

    @Test(dataProvider="sourceTargetReplacementWithNull",
            expectedExceptions = {NullPointerException.class})
    public void testNPE(String source, String target, String replacement) {
        source.replace(target, replacement);
    }


    @DataProvider
    public static Object[][] sourceTargetReplacementExpected() {
        return new Object[][] {
            {"aaa", "aa", "b", "ba"},
            {"abcdefgh", "def", "DEF", "abcDEFgh"},
            {"abcdefgh", "123", "DEF", "abcdefgh"},
            {"abcdefgh", "abcdefghi", "DEF", "abcdefgh"},
            {"abcdefghabc", "abc", "DEF", "DEFdefghDEF"},
            {"abcdefghdef", "def", "", "abcgh"},
            {"abcdefgh", "", "_", "_a_b_c_d_e_f_g_h_"},
            {"", "", "", ""},
            {"", "a", "b", ""},
            {"", "", "abc", "abc"},
            {"abcdefgh", "abcdefgh", "abcdefgh", "abcdefgh"},
            {"abcdefgh", "abcdefgh", "abcdefghi", "abcdefghi"},
            {"abcdefgh", "abcdefgh", "", ""},
            {"abcdabcd", "abcd", "", ""},
            {"aaaaaaaaa", "aa", "_X_", "_X__X__X__X_a"},
            {"aaaaaaaaa", "aa", "aaa", "aaaaaaaaaaaaa"},
            {"aaaaaaaaa", "aa", "aa", "aaaaaaaaa"},
            {"a.c.e.g.", ".", "-", "a-c-e-g-"},
            {"abcdefgh", "[a-h]", "X", "abcdefgh"},
            {"aa+", "a+", "", "a"},
            {"^abc$", "abc", "x", "^x$"},
        };
    }

    @DataProvider
    public static Iterator<Object[]> sourceTargetReplacement() {
        ArrayList<Object[]> list = new ArrayList<>();
        for (int maxSrcLen = 1; maxSrcLen <= (1 << 10); maxSrcLen <<= 1) {
            for (int maxTrgLen = 1; maxTrgLen <= (1 << 10); maxTrgLen <<= 1) {
                for (int maxPrlLen = 1; maxPrlLen <= (1 << 10); maxPrlLen <<= 1) {
                    list.add(makeArray(makeRandomString(maxSrcLen),
                                       makeRandomString(maxTrgLen),
                                       makeRandomString(maxPrlLen)));

                    String source = makeRandomString(maxSrcLen);
                    list.add(makeArray(source,
                                       mekeRandomSubstring(source, maxTrgLen),
                                       makeRandomString(maxPrlLen)));
                }
            }
        }
        return list.iterator();
    }

    @DataProvider
    public static Iterator<Object[]> sourceTargetReplacementWithNull() {
        ArrayList<Object[]> list = new ArrayList<>();
        Object[] arr = {null, "", "a", "b", "string", "str", "ababstrstr"};
        for (int i = 0; i < arr.length; ++i) {
            for (int j = 0; j < arr.length; ++j) {
                for (int k = 0; k < arr.length; ++k) {
                    if (arr[i] != null && (arr[j] == null || arr[k] == null)) {
                        list.add(makeArray(arr[i], arr[j], arr[k]));
                    }
                }
            }
        }
        return list.iterator();
    }

    // utilities

    /**
     * How the String.replace(CharSequence, CharSequence) used to be implemented
     */
    private static String canonicalReplace(String source, String target, String replacement) {
        return Pattern.compile(target.toString(), Pattern.LITERAL).matcher(
                source).replaceAll(Matcher.quoteReplacement(replacement.toString()));
    }

    private static final Random random = RandomFactory.getRandom();

    private static final char[] CHARS = ("qwertyuiop[]12345678" +
        "90-=\\`asdfghjkl;'zxcvbnm,./~!@#$%^&*()_+|QWERTYUIOP{" +
        "}ASDFGHJKL:\"ZXCVBNM<>?\n\r\t\u0444\u044B\u0432\u0430").toCharArray();

    private static String makeRandomString(int maxLen) {
        int len = random.nextInt(maxLen);
        char[] buf = new char[len];
        for (int i = 0; i < len; ++i) {
            buf[i] = CHARS[random.nextInt(CHARS.length)];
        }
        return new String(buf);
    }

    private static String mekeRandomSubstring(String source, int maxLen) {
        if (source.isEmpty()) {
            return source;
        }
        int pos = random.nextInt(source.length());
        int len = Integer.min(source.length() - pos,
                              random.nextInt(maxLen));
        return source.substring(pos, pos + len);
    }

    private static Object[] makeArray(Object... array) {
         return array;
    }
}
