package dev.vality.testcontainers.annotations.clickhouse;

import java.util.ArrayList;
import java.util.List;

final class SqlScriptParser {

    private SqlScriptParser() {
    }

    static List<String> splitStatements(String script) {
        var parser = new Parser();
        var index = 0;
        while (index < script.length()) {
            index = parser.consume(script, index);
        }
        return parser.finish();
    }

    private static String findDollarQuoteDelimiter(String script, int start) {
        var end = script.indexOf('$', start + 1);
        if (end < 0) {
            return null;
        }
        var tag = script.substring(start + 1, end);
        if (!tag.chars().allMatch(character -> Character.isLetterOrDigit(character) || character == '_')) {
            return null;
        }
        return script.substring(start, end + 1);
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        var statement = current.toString().trim();
        current.setLength(0);
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }

    private static final class Parser {

        private final List<String> statements = new ArrayList<>();
        private final StringBuilder current = new StringBuilder();
        private boolean singleQuoted;
        private boolean doubleQuoted;
        private boolean backtickQuoted;
        private boolean lineComment;
        private boolean blockComment;
        private String dollarQuote;

        private int consume(String script, int index) {
            if (lineComment) {
                return consumeLineComment(script, index);
            }
            if (blockComment) {
                return consumeBlockComment(script, index);
            }
            if (dollarQuote != null) {
                return consumeDollarQuote(script, index);
            }
            if (!isQuoted()) {
                var nextIndex = consumeUnquotedToken(script, index);
                if (nextIndex >= 0) {
                    return nextIndex;
                }
            }
            return consumeText(script, index);
        }

        private int consumeLineComment(String script, int index) {
            var currentChar = script.charAt(index);
            if (currentChar == '\n') {
                current.append(currentChar);
                lineComment = false;
            }
            return index + 1;
        }

        private int consumeBlockComment(String script, int index) {
            if (startsWith(script, index, "*/")) {
                current.append(' ');
                blockComment = false;
                return index + 2;
            }
            return index + 1;
        }

        private int consumeDollarQuote(String script, int index) {
            if (script.startsWith(dollarQuote, index)) {
                current.append(dollarQuote);
                var nextIndex = index + dollarQuote.length();
                dollarQuote = null;
                return nextIndex;
            }
            current.append(script.charAt(index));
            return index + 1;
        }

        private int consumeUnquotedToken(String script, int index) {
            if (startsWith(script, index, "--")) {
                current.append(' ');
                lineComment = true;
                return index + 2;
            }
            if (startsWith(script, index, "/*")) {
                current.append(' ');
                blockComment = true;
                return index + 2;
            }
            if (script.charAt(index) == '$') {
                return consumeDollarQuoteStart(script, index);
            }
            if (script.charAt(index) == ';') {
                addStatement(statements, current);
                return index + 1;
            }
            return -1;
        }

        private int consumeDollarQuoteStart(String script, int index) {
            var delimiter = findDollarQuoteDelimiter(script, index);
            if (delimiter == null) {
                return -1;
            }
            current.append(delimiter);
            dollarQuote = delimiter;
            return index + delimiter.length();
        }

        private int consumeText(String script, int index) {
            var currentChar = script.charAt(index);
            var nextChar = nextChar(script, index);
            current.append(currentChar);
            if (currentChar == '\\' && isQuoted() && nextChar != '\0') {
                current.append(nextChar);
                return index + 2;
            }
            return consumeQuote(currentChar, nextChar, index);
        }

        private int consumeQuote(char currentChar, char nextChar, int index) {
            if (currentChar == '\'' && !doubleQuoted && !backtickQuoted) {
                return consumeSingleQuote(nextChar, index);
            }
            if (currentChar == '"' && !singleQuoted && !backtickQuoted) {
                return consumeDoubleQuote(nextChar, index);
            }
            if (currentChar == '`' && !singleQuoted && !doubleQuoted) {
                return consumeBacktickQuote(nextChar, index);
            }
            return index + 1;
        }

        private int consumeSingleQuote(char nextChar, int index) {
            if (singleQuoted && nextChar == '\'') {
                current.append(nextChar);
                return index + 2;
            }
            singleQuoted = !singleQuoted;
            return index + 1;
        }

        private int consumeDoubleQuote(char nextChar, int index) {
            if (doubleQuoted && nextChar == '"') {
                current.append(nextChar);
                return index + 2;
            }
            doubleQuoted = !doubleQuoted;
            return index + 1;
        }

        private int consumeBacktickQuote(char nextChar, int index) {
            if (backtickQuoted && nextChar == '`') {
                current.append(nextChar);
                return index + 2;
            }
            backtickQuoted = !backtickQuoted;
            return index + 1;
        }

        private boolean isQuoted() {
            return singleQuoted || doubleQuoted || backtickQuoted;
        }

        private List<String> finish() {
            if (isQuoted() || blockComment || dollarQuote != null) {
                throw new IllegalArgumentException("SQL script contains an unterminated quoted value or comment");
            }
            addStatement(statements, current);
            return List.copyOf(statements);
        }

        private static boolean startsWith(String script, int index, String value) {
            return script.startsWith(value, index);
        }

        private static char nextChar(String script, int index) {
            return index + 1 < script.length() ? script.charAt(index + 1) : '\0';
        }
    }
}
