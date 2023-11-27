package dev.vality.testcontainers.annotations.opensearch;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(OpensearchTestcontainerExtension.class)
public @interface OpensearchTestcontainer {

    /**
     * Аналогичный параметр как у аннотации {@link SpringBootTest#properties()}
     * <p>
     * пример — properties = {"opensearch.make.happy=true",...}
     */
    String[] properties() default {};

}
