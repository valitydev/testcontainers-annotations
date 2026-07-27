package dev.vality.testcontainers.annotations.util;

import lombok.Data;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RandomBeansTest {

    @Test
    void shouldKeepSeededGenerationDeterministic() {
        var first = RandomBeans.random(123L, RandomBean.class);
        var second = RandomBeans.random(123L, RandomBean.class);

        assertEquals(first, second);
    }

    @Test
    void shouldUseCurrentTimeForGenerationWithoutExplicitSeed() {
        var first = RandomBeans.random(RandomBean.class);
        var second = RandomBeans.random(RandomBean.class);

        assertNotEquals(first.getValue(), second.getValue());
        assertNotEquals(first.getCreatedAt(), second.getCreatedAt());
    }

    @Data
    public static class RandomBean {

        private String value;
        private Instant createdAt;
    }
}
