package io.github.logassert.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link io.github.logassert.core.LogCaptor} field for automatic injection by {@link
 * LogCaptorExtension}.
 *
 * <p>The extension will set the field value before each test method runs. The field must be of type
 * {@link io.github.logassert.core.LogCaptor} (or a compatible subtype).
 *
 * <pre>{@code
 * @ExtendWith(LogCaptorExtension.class)
 * class MyTest {
 *     @InjectLogCaptor LogCaptor captor;
 *
 *     @Test void example() {
 *         myService.doSomething();
 *         assertThat(captor.getLogs()).isNotEmpty();
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectLogCaptor {}
