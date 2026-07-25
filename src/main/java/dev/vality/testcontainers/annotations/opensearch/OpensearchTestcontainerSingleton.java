package dev.vality.testcontainers.annotations.opensearch;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(OpensearchTestcontainerExtension.class)
@DirtiesContext
public @interface OpensearchTestcontainerSingleton {

    /**
     * Аналогичный параметр как у аннотации {@link SpringBootTest#properties()}
     * <p>
     * пример — properties = {"opensearch.make.happy=true",...}
     */
    String[] properties() default {};

    /**
     * Удалять тестовые индексы перед каждым тестом.
     */
    boolean cleanupIndexes() default true;

    /**
     * Удалять только индексы с указанными префиксами. Пустой список означает все пользовательские индексы.
     */
    String[] indexPrefixes() default {};

    /**
     * Имена индексов, которые не нужно удалять. Системные индексы с точкой в начале исключаются всегда.
     */
    String[] excludeIndexes() default {};

}
