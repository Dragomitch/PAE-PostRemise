import {Component, Injectable, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {OptionService} from "../services/option.service";
import {Option} from "../models/option.model";
import {Observable, of, pipe} from "rxjs";
import {tap} from "rxjs/operators";
import {SessionService} from "../services/session.service";

@Injectable()
@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent implements OnInit {
  signupForm: FormGroup;
  options: Option[];
  options$ : Observable<Option[]>;
  selectedOption: Option;//TODO Necessary ?
  selectedOptionIndex: number = 0;

  constructor(private optionService: OptionService,
              private sessionService: SessionService) {

    optionService.optionsChanged$.subscribe((options) => {
      console.debug("Lets react to the change of the option list");
    });

    optionService.selectedOption$.subscribe( (index: number) => {
      this.selectedOptionIndex = index;
      console.debug("Reacted to selectedOptionIndex changes");
    });

    this.options$ = optionService.options$;
  }

  ngOnInit() {
    this.options = this.optionService.getOptions();
    this.initForm();
  }

  onOptionSelected(value: string) {
          this.options.forEach( (option, index)=> {
            if(option.name == value) {// TODO Check how to find index of array member faster
              this.selectedOptionIndex = index;
            }
          });
    console.log(this.selectedOptionIndex);
  }

  onSubmit() {
    console.log(this.signupForm);
    if(this.signupForm.valid){

    }
  }

  initForm() {
    let lastName = '';
    let firstName = '';
    let username = '';
    let password = '';
    let reenterPassword = '';
    let email = '';
    let selectedOption = '';

    //TODO Create a password (strength) validator
    //TODO Create return form validator that displays errors
    this.signupForm = new FormGroup({
      'firstName': new FormControl(firstName, Validators.required),
      'lastName': new FormControl(lastName, Validators.required),
      'username': new FormControl(username, Validators.required),
      'password': new FormControl(password, [Validators.required, Validators.minLength(8)]),
      'reenterPassword': new FormControl(reenterPassword,
        [Validators.required, Validators.minLength(8)]),
      'email': new FormControl(email, Validators.required),
      'selectedOption': new FormControl(selectedOption, Validators.required)
    });
  }

}
