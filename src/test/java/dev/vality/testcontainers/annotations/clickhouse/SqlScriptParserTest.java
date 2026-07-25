package dev.vality.testcontainers.annotations.clickhouse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlScriptParserTest {

    @Test
    void shouldIgnoreSemicolonsInsideQuotedValuesAndComments() {
        var statements = SqlScriptParser.splitStatements("""
                CREATE TABLE events (value String);
                INSERT INTO events VALUES ('a;b'); -- comment ;
                /* block ; comment */ SELECT $$dollar;quoted$$;
                """);

        assertThat(statements)
                .hasSize(3)
                .element(1)
                .isEqualTo("INSERT INTO events VALUES ('a;b')");
        assertThat(statements.get(2))
                .contains("$$dollar;quoted$$");
    }
}
