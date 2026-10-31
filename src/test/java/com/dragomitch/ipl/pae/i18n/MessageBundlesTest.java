package com.dragomitch.ipl.pae.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;

import jakarta.validation.Constraint;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

/**
 * Resilience of the translations: a message missing in one language, an error code without its
 * title or detail, or a constraint whose message key is not translated fails the build.
 *
 * <p>Bundles: {@code i18n/messages.properties} (French, the fallback) and
 * {@code i18n/messages_en.properties}.
 */
class MessageBundlesTest {

  /** Languages other than the fallback bundle. */
  private static final List<Locale> LANGUAGES = List.of(Locale.FRENCH, Locale.ENGLISH);

  /** The exceptions of Spring MVC that ApiExceptionHandler renders with their own messages. */
  static final List<String> SPRING_EXCEPTIONS = List.of(
      "org.springframework.web.HttpRequestMethodNotSupportedException",
      "org.springframework.web.HttpMediaTypeNotSupportedException",
      "org.springframework.web.HttpMediaTypeNotAcceptableException",
      "org.springframework.web.bind.MissingServletRequestParameterException",
      "org.springframework.web.bind.MissingRequestHeaderException",
      "org.springframework.web.bind.MissingRequestCookieException",
      "org.springframework.web.bind.UnsatisfiedServletRequestParameterException",
      "org.springframework.web.bind.ServletRequestBindingException",
      "org.springframework.web.multipart.support.MissingServletRequestPartException",
      "org.springframework.web.servlet.resource.NoResourceFoundException",
      "org.springframework.web.servlet.NoHandlerFoundException",
      "org.springframework.beans.TypeMismatchException",
      "org.springframework.web.method.annotation.MethodArgumentTypeMismatchException",
      "org.springframework.http.converter.HttpMessageNotReadableException",
      "org.springframework.web.multipart.MaxUploadSizeExceededException");

  private static final Pattern MESSAGE_KEY = Pattern.compile("^\\{([^{}]+)\\}$");
  private static final Pattern ARGUMENT = Pattern.compile("\\{\\d+}");

  private static Properties french;
  private static Properties english;

  @BeforeAll
  static void loadBundles() throws IOException {
    french = load("i18n/messages.properties");
    english = load("i18n/messages_en.properties");
  }

  private static Properties load(String path) throws IOException {
    Properties properties = new Properties();
    try (Reader reader = new InputStreamReader(new ClassPathResource(path).getInputStream(),
        StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private static Stream<Properties> bundles() {
    return Stream.of(french, english);
  }

  @Test
  void bothLanguagesHaveTheSameKeys() {
    Set<String> onlyFrench = new TreeSet<>(french.stringPropertyNames());
    onlyFrench.removeAll(english.stringPropertyNames());
    Set<String> onlyEnglish = new TreeSet<>(english.stringPropertyNames());
    onlyEnglish.removeAll(french.stringPropertyNames());

    assertThat(onlyFrench).as("keys missing in messages_en.properties").isEmpty();
    assertThat(onlyEnglish).as("keys missing in messages.properties").isEmpty();
    assertThat(french.stringPropertyNames()).isNotEmpty();
  }

  @Test
  void noMessageIsBlank() {
    bundles().forEach(bundle -> bundle.stringPropertyNames().forEach(key ->
        assertThat(bundle.getProperty(key)).as(key).isNotBlank()));
  }

  @ParameterizedTest
  @EnumSource(ErrorCode.class)
  void everyErrorCodeHasATitleAndADetailInEveryLanguage(ErrorCode code) {
    bundles().forEach(bundle -> {
      assertThat(bundle.getProperty(code.titleMessageCode())).as(code.titleMessageCode())
          .isNotBlank();
      assertThat(bundle.getProperty(code.detailMessageCode())).as(code.detailMessageCode())
          .isNotBlank();
    });
  }

  @Test
  void everyMessageKeyOfTheBundlesIsUsed() {
    // problem.* keys must belong to an existing error code (no leftover after a removal)
    Set<String> expected = new TreeSet<>();
    for (ErrorCode code : ErrorCode.values()) {
      expected.add(code.titleMessageCode());
      expected.add(code.detailMessageCode());
    }
    Set<String> problemKeys = new TreeSet<>();
    for (String key : french.stringPropertyNames()) {
      if (key.startsWith("problem.")) {
        problemKeys.add(key);
      }
    }
    assertThat(problemKeys).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("springExceptions")
  void theSpringMvcErrorsHaveATitleAndADetailInEveryLanguage(String exceptionClass) {
    bundles().forEach(bundle -> {
      assertThat(bundle.getProperty("problemDetail.title." + exceptionClass))
          .as("title of " + exceptionClass).isNotBlank();
      assertThat(bundle.getProperty("problemDetail." + exceptionClass))
          .as("detail of " + exceptionClass).isNotBlank();
    });
  }

  static Stream<String> springExceptions() {
    return SPRING_EXCEPTIONS.stream();
  }

  @Test
  void theHandledSpringExceptionsExist() {
    for (String exceptionClass : SPRING_EXCEPTIONS) {
      assertThatCode(() -> Class.forName(exceptionClass)).as(exceptionClass)
          .doesNotThrowAnyException();
    }
  }

  /**
   * The problem messages may take arguments ({0}, {1}): they are then MessageFormat patterns, in
   * which a plain apostrophe starts a quoted section and hides the arguments. Texts must use the
   * typographic apostrophe.
   */
  @Test
  void theProblemMessagesAreValidMessageFormatsWithoutPlainApostrophes() {
    bundles().forEach(bundle -> bundle.stringPropertyNames().stream()
        .filter(key -> key.startsWith("problem"))
        .forEach(key -> {
          String message = bundle.getProperty(key);
          assertThat(message).as(key).doesNotContain("'");
          assertThatCode(() -> new MessageFormat(message, Locale.ROOT)).as(key)
              .doesNotThrowAnyException();
        }));
  }

  @Test
  void aProblemDetailHasTheSameArgumentsInEveryLanguage() {
    for (String key : french.stringPropertyNames()) {
      assertThat(arguments(english.getProperty(key))).as(key)
          .isEqualTo(arguments(french.getProperty(key)));
    }
  }

  private static Set<String> arguments(String message) {
    Set<String> arguments = new TreeSet<>();
    Matcher matcher = ARGUMENT.matcher(message);
    while (matcher.find()) {
      arguments.add(matcher.group());
    }
    return arguments;
  }

  /**
   * Every message template of a constraint of the application ({@code {key}}, explicit or the
   * default one of the annotation) is translated, so that the Spring validator never falls back
   * on Hibernate Validator's own bundle or shows a raw key.
   */
  @Test
  void everyConstraintMessageOfTheApplicationIsTranslated() {
    Map<String, List<String>> usedKeys = constraintMessageKeys();

    assertThat(usedKeys).as("constraint messages found").isNotEmpty();
    assertThat(usedKeys).containsKeys("jakarta.validation.constraints.NotBlank.message",
        "jakarta.validation.constraints.Size.message", "pae.validation.Iban.message",
        "pae.validation.FilterValueRequired.message");
    usedKeys.forEach((key, users) -> bundles().forEach(bundle ->
        assertThat(bundle.getProperty(key)).as(key + " used by " + users).isNotBlank()));
  }

  /** The {@code {key}} message templates of the constraints of the application's classes. */
  private static Map<String, List<String>> constraintMessageKeys() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false) {
          @Override
          protected boolean isCandidateComponent(
              org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definition) {
            return true;
          }
        };
    scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
    Map<String, List<String>> keys = new TreeMap<>();
    for (BeanDefinition definition : scanner.findCandidateComponents("com.dragomitch.ipl.pae")) {
      Class<?> type;
      try {
        type = Class.forName(definition.getBeanClassName());
      } catch (ClassNotFoundException | LinkageError ex) {
        continue;
      }
      if (type.getName().contains("Test")) {
        continue;
      }
      List<AnnotatedElement> elements = new ArrayList<>();
      elements.add(type);
      for (Method method : type.getDeclaredMethods()) {
        elements.add(method);
        for (Parameter parameter : method.getParameters()) {
          elements.add(parameter);
        }
      }
      if (type.isRecord()) {
        for (var component : type.getRecordComponents()) {
          elements.add(component);
        }
      }
      for (AnnotatedElement element : elements) {
        for (Annotation annotation : element.getAnnotations()) {
          collect(annotation, type.getSimpleName(), keys);
        }
      }
    }
    return keys;
  }

  private static void collect(Annotation annotation, String user,
      Map<String, List<String>> keys) {
    Class<? extends Annotation> annotationType = annotation.annotationType();
    if (annotationType.isAnnotationPresent(Constraint.class)) {
      try {
        String message = (String) annotationType.getMethod("message").invoke(annotation);
        Matcher matcher = MESSAGE_KEY.matcher(message);
        if (matcher.matches()) {
          keys.computeIfAbsent(matcher.group(1), k -> new ArrayList<>()).add(user);
        }
      } catch (ReflectiveOperationException ex) {
        throw new IllegalStateException(ex);
      }
      return;
    }
    // repeated constraints (@Size.List...)
    try {
      Method value = annotationType.getMethod("value");
      if (value.getReturnType().isArray()
          && value.getReturnType().getComponentType().isAnnotation()) {
        for (Annotation nested : (Annotation[]) value.invoke(annotation)) {
          collect(nested, user, keys);
        }
      }
    } catch (ReflectiveOperationException ex) {
      // not a container annotation
    }
  }

  @Test
  void theSupportedLanguagesAreFrenchAndEnglish() {
    assertThat(LANGUAGES).containsExactly(com.dragomitch.ipl.pae.config.WebConfig.DEFAULT_LOCALE,
        Locale.ENGLISH);
    assertThat(com.dragomitch.ipl.pae.config.WebConfig.SUPPORTED_LOCALES)
        .containsExactlyElementsOf(LANGUAGES);
  }
}
