import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {AppComponent} from "./app.component";
import {SignupComponent} from "./signup/signup.component";
import {SigninComponent} from "./signin/signin.component";

const routes: Routes = [
  {path: '', component: AppComponent, pathMatch: 'full'},
  //{path: 'recipes', component: RecipesComponent, children: [
  {path: 'signin', component: SigninComponent},
  {path: 'signup', component: SignupComponent}

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
