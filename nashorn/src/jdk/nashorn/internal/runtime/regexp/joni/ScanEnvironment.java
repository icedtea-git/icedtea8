/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package jdk.nashorn.internal.runtime.regexp.joni;

import static jdk.nashorn.internal.runtime.regexp.joni.BitStatus.bsClear;

import jdk.nashorn.internal.runtime.regexp.joni.ast.Node;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ErrorMessages;
import jdk.nashorn.internal.runtime.regexp.joni.exception.InternalException;

public final class ScanEnvironment {

    private static final int SCANENV_MEMNODES_SIZE = 8;

    int option;
    final int caseFoldFlag;
    final public Syntax syntax;
    int captureHistory;
    int btMemStart;
    int btMemEnd;
    int backrefedMem;

    final public Regex reg;

    int numCall;
    public int numMem;

    public Node memNodes[];

    int currMaxRegNum;
    boolean hasRecursion;

    public ScanEnvironment(Regex regex, Syntax syntax) {
        this.reg = regex;
        option = regex.options;
        caseFoldFlag = regex.caseFoldFlag;
        this.syntax = syntax;
    }

    public void clear() {
        captureHistory = bsClear();
        btMemStart = bsClear();
        btMemEnd = bsClear();
        backrefedMem = bsClear();

        numCall = 0;
        numMem = 0;

        memNodes = null;

        currMaxRegNum = 0;
        hasRecursion = false;
    }

    public int addMemEntry() {
        if (numMem++ == 0) {
            memNodes = new Node[SCANENV_MEMNODES_SIZE];
        } else if (numMem >= memNodes.length) {
            Node[]tmp = new Node[memNodes.length << 1];
            System.arraycopy(memNodes, 0, tmp, 0, memNodes.length);
            memNodes = tmp;
        }

        return numMem;
    }

    public void setMemNode(int num, Node node) {
        if (numMem >= num) {
            memNodes[num] = node;
        } else {
            throw new InternalException(ErrorMessages.ERR_PARSER_BUG);
        }
    }

    public int convertBackslashValue(int c) {
        if (syntax.opEscControlChars()) {
            switch (c) {
            case 'n': return '\n';
            case 't': return '\t';
            case 'r': return '\r';
            case 'f': return '\f';
            case 'a': return '\007';
            case 'b': return '\010';
            case 'e': return '\033';
            case 'v':
                if (syntax.op2EscVVtab()) return 11; // ???
                break;
            default:
                break;
            }
        }
        return c;
    }

    void ccEscWarn(String s) {
        if (Config.USE_WARN) {
            if (syntax.warnCCOpNotEscaped() && syntax.backSlashEscapeInCC()) {
                reg.warnings.warn("character class has '" + s + "' without escape");
            }
        }
    }

    void closeBracketWithoutEscapeWarn(String s) {
        if (Config.USE_WARN) {
            if (syntax.warnCCOpNotEscaped()) {
                reg.warnings.warn("regular expression has '" + s + "' without escape");
            }
        }
    }
}