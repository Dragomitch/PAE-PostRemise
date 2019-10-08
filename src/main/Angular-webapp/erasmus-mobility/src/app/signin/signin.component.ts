import {Component, Injectable, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {OptionService} from "../services/option.service";
import {Option} from "../models/option.model";
import {SessionService} from "../services/session.service";
import {logger} from "codelyzer/util/logger";
import {pipe} from "rxjs";

@Component({
  selector: 'app-signin',
  templateUrl: './signin.component.html',
  styleUrls: ['./signin.component.scss']
})
@Injectable()
export class SigninComponent implements OnInit {
  signinForm: FormGroup;
  message: string;
  username: string;
  password: string;
  notification: string;
  options = Option;

  constructor(private optionService: OptionService,
              private sessionService: SessionService) {
  }

  ngOnInit() {
    this.initForm();
  }

  onSubmit() {
    let form: Object = this.signinForm.getRawValue();
    this.sessionService.login(form['username'], form['password']).subscribe(
      res => {
        console.log("res = ");
        console.log(res);
      }
    );
/*
    if(SessionService.userData.role === "Student"){
      console.log(this.sessionService.temp_getMobilities());
    }*/
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
