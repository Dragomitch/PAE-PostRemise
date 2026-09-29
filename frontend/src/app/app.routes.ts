import { Routes } from '@angular/router';
import { SignIn } from './sign-in/sign-in';
import { SignUp } from './sign-up/sign-up';

export const routes: Routes = [
  { path: '', redirectTo: 'signin', pathMatch: 'full' },
  { path: 'signin', component: SignIn, title: $localize`:@@route.signin.title:Connexion - EMA` },
  { path: 'signup', component: SignUp, title: $localize`:@@route.signup.title:Inscription - EMA` },
  { path: '**', redirectTo: 'signin' },
];
