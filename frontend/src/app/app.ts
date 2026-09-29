import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { LanguageSwitcher } from './language-switcher/language-switcher';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LanguageSwitcher],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
