import { ChangeDetectionStrategy, Component, LOCALE_ID, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs';

import { SUPPORTED_LOCALES, toSupportedLanguage } from '../core/locales';

/**
 * Links to the same route in the other locales. Each locale is a separate build served under
 * `/<locale>/` (see angular.json `i18n.*.subPath` and nginx.conf), so switching is a plain
 * full-page navigation, not a router navigation.
 */
@Component({
  selector: 'app-language-switcher',
  templateUrl: './language-switcher.html',
  styleUrl: './language-switcher.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguageSwitcher {
  private readonly router = inject(Router);
  protected readonly current = toSupportedLanguage(inject(LOCALE_ID));

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  protected readonly links = computed(() =>
    SUPPORTED_LOCALES.filter((locale) => locale.code !== this.current).map((locale) => ({
      ...locale,
      href: `/${locale.code}${this.url()}`,
    })),
  );
}
