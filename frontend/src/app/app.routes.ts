import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';
import { Partners } from './partners/partners';
import { Shell } from './shell/shell';
import { SignIn } from './sign-in/sign-in';
import { SignUp } from './sign-up/sign-up';

export const routes: Routes = [
  { path: 'signin', component: SignIn, title: $localize`:@@route.signin.title:Connexion - EMA` },
  { path: 'signup', component: SignUp, title: $localize`:@@route.signup.title:Inscription - EMA` },
  // Authenticated screens share the shell (header, navigation, sign-out). The guard sends
  // anonymous visitors to /signin, so "/" and unknown paths land on the partners once signed in.
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'partners', pathMatch: 'full' },
      { path: 'partners', component: Partners, title: $localize`:@@route.partners.title:Partenaires - EMA` },
    ],
  },
  { path: '**', redirectTo: 'partners' },
];
