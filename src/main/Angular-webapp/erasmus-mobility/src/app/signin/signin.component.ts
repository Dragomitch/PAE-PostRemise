import { Component, OnInit } from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";

@Component({
  selector: 'app-signin',
  templateUrl: './signin.component.html',
  styleUrls: ['./signin.component.scss']
})
export class SigninComponent implements OnInit {
  signinForm: FormGroup;
  message: string;
  username: string;
  password: string;
  notification: string;



  constructor() { }

  ngOnInit() {
    this.initForm();
  }

  onSubmit(){
    console.log(this.signinForm);
    //TODO HTTP Request to the backend with the login informations
  }

  initForm() {
    let username = '';
    let password = '';

    this.signinForm = new FormGroup({
      'username': new FormControl(username, Validators.required),
      'password': new FormControl(password, [Validators.required, Validators.minLength(8)])
    });
  }

  //TODO Handle Notification Messages after a sign-up and after a failed sign-in
  //TODO Create a Validator for a correct secured password
}
