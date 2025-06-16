import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';

@Component({
  selector: 'app-sign-up',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './sign-up.html',
  styleUrl: './sign-up.css'
})
export class SignUp {
  form: any = {};

  constructor(private api: ApiService) {}

  submit() {
    this.api.signup(this.form).subscribe({
      next: () => {
        console.log('signed up');
      }
    });
  }
}
