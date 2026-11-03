import { Routes } from '@angular/router';

import { authGuard, roleGuard } from './core/auth.guard';
import { ROLE_PROFESSOR, ROLE_STUDENT } from './core/models';
import { MobilityChoices } from './mobility-choices/mobility-choices';
import { Partners } from './partners/partners';
import { PersonalData } from './personal-data/personal-data';
import { Shell } from './shell/shell';
import { SignIn } from './sign-in/sign-in';
import { SignUp } from './sign-up/sign-up';
import { Users } from './users/users';

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
      {
        path: 'mobility-choices',
        component: MobilityChoices,
        title: $localize`:@@route.mobilityChoices.title:Demandes de mobilité - EMA`,
      },
      // screens of one role: the others are sent back to the partners
      {
        path: 'personal-data',
        component: PersonalData,
        canActivate: [roleGuard(ROLE_STUDENT)],
        title: $localize`:@@route.personalData.title:Mes données personnelles - EMA`,
      },
      {
        path: 'users',
        component: Users,
        canActivate: [roleGuard(ROLE_PROFESSOR)],
        title: $localize`:@@route.users.title:Utilisateurs - EMA`,
      },
    ],
  },
  { path: '**', redirectTo: 'partners' },
];
