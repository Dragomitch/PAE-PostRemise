package com.dragomitch.ipl.pae.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.mock.web.MockHttpServletRequest;

class AcceptLanguageLocaleResolverTest {

  private final AcceptLanguageLocaleResolver resolver = new AcceptLanguageLocaleResolver(
      WebConfig.SUPPORTED_LOCALES, WebConfig.DEFAULT_LOCALE);

  private Locale resolve(String acceptLanguage) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (acceptLanguage != null) {
      request.addHeader("Accept-Language", acceptLanguage);
    }
    return resolver.resolveLocale(request);
  }

  @ParameterizedTest(name = "\"{0}\" -> {1}")
  @CsvSource(delimiter = '|', value = {
      "en|en", "en-US|en", "en-US,en;q=0.9,fr;q=0.8|en", "fr|fr", "fr-BE|fr",
      "fr-CA,en;q=0.5|fr", "de|fr", "de-DE,en;q=0.5|en", "*|fr", "nl;q=0.9,en;q=0.1|en",
      "en;q=0.1,fr;q=0.9|fr", "not a language tag;;;|fr"})
  void negotiatesAmongTheSupportedLanguages(String header, String language) {
    assertThat(resolve(header)).isEqualTo(Locale.forLanguageTag(language));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void withoutHeaderTheDefaultLanguageIsFrench(String header) {
    assertThat(resolve(header)).isEqualTo(Locale.FRENCH);
  }
}
