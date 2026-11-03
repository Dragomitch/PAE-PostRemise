import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiProblem, toApiProblem } from '../core/api-problem';
import { roleLabel } from '../core/roles';
import { SessionService } from '../core/session.service';

/**
 * Layout of the authenticated screens: header with the application name, the navigation, the
 * signed-in user (name and role) and the sign-out action, and the outlet of the current screen.
 * Mounted by the guarded parent route in `app.routes.ts`.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shell {
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  protected readonly isProfessor = this.session.isProfessor;
  protected readonly displayName = computed(() => {
    const user = this.session.user();
    const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(' ');
    return fullName || user?.username || '';
  });
  protected readonly roleLabel = computed(() => roleLabel(this.session.user()?.role));
  protected readonly signingOut = signal(false);
  protected readonly problem = signal<ApiProblem | null>(null);

  protected signOut(): void {
    if (this.signingOut()) {
      return;
    }
    this.signingOut.set(true);
    this.problem.set(null);
    this.session
      .signOut()
      .pipe(finalize(() => this.signingOut.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/signin']),
        error: (error: unknown) => this.problem.set(toApiProblem(error)),
      });
  }
}
