package dev.vality.testcontainers.annotations.clickhouse;

import java.util.ArrayList;
import java.util.List;

final class SqlScriptParser {

    private SqlScriptParser() {
    }

    static List<String> splitStatements(String script) {
        var statements = new ArrayList<String>();
        var current = new StringBuilder();
        var singleQuoted = false;
        var doubleQuoted = false;
        var backtickQuoted = false;
        var lineComment = false;
        var blockComment = false;
        String dollarQuote = null;

        for (int index = 0; index < script.length(); index++) {
            var currentChar = script.charAt(index);
            var nextChar = index + 1 < script.length() ? script.charAt(index + 1) : '\0';

            if (lineComment) {
                if (currentChar == '\n') {
                    current.append(currentChar);
                    lineComment = false;
                }
                continue;
            }
            if (blockComment) {
                if (currentChar == '*' && nextChar == '/') {
                    current.append(' ');
                    index++;
                    blockComment = false;
                }
                continue;
            }
            if (dollarQuote != null) {
                if (script.startsWith(dollarQuote, index)) {
                    current.append(dollarQuote);
                    index += dollarQuote.length() - 1;
                    dollarQuote = null;
                } else {
                    current.append(currentChar);
                }
                continue;
            }

            if (!singleQuoted && !doubleQuoted && !backtickQuoted) {
                if (currentChar == '-' && nextChar == '-') {
                    current.append(' ');
                    index++;
                    lineComment = true;
                    continue;
                }
                if (currentChar == '/' && nextChar == '*') {
                    current.append(' ');
                    index++;
                    blockComment = true;
                    continue;
                }
                if (currentChar == '$') {
                    var delimiter = findDollarQuoteDelimiter(script, index);
                    if (delimiter != null) {
                        current.append(delimiter);
                        index += delimiter.length() - 1;
                        dollarQuote = delimiter;
                        continue;
                    }
                }
                if (currentChar == ';') {
                    addStatement(statements, current);
                    continue;
                }
            }

            current.append(currentChar);
            if (currentChar == '\\' && (singleQuoted || doubleQuoted || backtickQuoted) && nextChar != '\0') {
                current.append(nextChar);
                index++;
                continue;
            }
            if (currentChar == '\'' && !doubleQuoted && !backtickQuoted) {
                if (singleQuoted && nextChar == '\'') {
                    current.append(nextChar);
                    index++;
                } else {
                    singleQuoted = !singleQuoted;
                }
            } else if (currentChar == '"' && !singleQuoted && !backtickQuoted) {
                if (doubleQuoted && nextChar == '"') {
                    current.append(nextChar);
                    index++;
                } else {
                    doubleQuoted = !doubleQuoted;
                }
            } else if (currentChar == '`' && !singleQuoted && !doubleQuoted) {
                if (backtickQuoted && nextChar == '`') {
                    current.append(nextChar);
                    index++;
                } else {
                    backtickQuoted = !backtickQuoted;
                }
            }
        }

        if (singleQuoted || doubleQuoted || backtickQuoted || blockComment || dollarQuote != null) {
            throw new IllegalArgumentException("SQL script contains an unterminated quoted value or comment");
        }
        addStatement(statements, current);
        return List.copyOf(statements);
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
}
