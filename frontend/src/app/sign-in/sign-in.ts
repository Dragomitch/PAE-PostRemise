import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { ApiService } from '../api.service';

@Component({
  selector: 'app-sign-in',
  standalone: true,
  imports: [FormsModule, NgIf],
  templateUrl: './sign-in.html',
  styleUrl: './sign-in.css'
})
export class SignIn {
  username = '';
  password = '';
  error = false;

  constructor(private api: ApiService) {}

  submit() {
    this.api.login({ username: this.username, password: this.password }).subscribe({
      next: () => {
        console.log('logged in');
      },
      error: () => {
        this.error = true;
      }
    });
  }
}
