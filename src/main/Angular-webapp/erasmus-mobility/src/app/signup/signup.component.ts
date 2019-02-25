import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {OptionService} from "../services/option.service";
import {Option} from "../models/option.model";
import {HttpRequestsService} from "../services/http-requests.service";
import {map} from "rxjs/operators";
import {pipe} from "rxjs";
import {flatMap} from "tslint/lib/utils";

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent implements OnInit {
  signupForm: FormGroup;
  options: Option[];
  selectedOption: Option;
  optionIndex: number = -1;

  constructor(private optionService: OptionService) {
    // this.optionService.testCombinaison().pipe(
    //   map(() => {return this.initForm();}));
    this.optionService.optionsChanged$.subscribe(
      (options: Option[]) => {
        this.options = options;
      }
    );

    this.optionService.selectedOption$.subscribe(
      (index: number) => {
        this.optionIndex = index;
        this.selectedOption = this.options[index];
      }
    );
  }

  async ngOnInit() {
    console.log("coucou Form");
    this.initForm();

    // pipe(map( () => {this.optionService.optionsChanged$.subscribe( (options : Option[]) => {
    //  this.options = options;
    // })}),
    // map(() => this.initForm()));
    flatMap( () => {
      this.optionService.optionsChanged$.subscribe(
        (options : Option[]) => {

        }
      )
    });
    pipe(flatMap( this.optionService.optionsChanged$.subscribe(
      (options: Option[]) => {
        this.options = options;
        return this.optionService.optionsChanged$;
      })),
      map(() => this.initForm()));

    // this.optionService.getOptions().subscribe(
    //   (options) => {
    //     console.log("signup component = ", options);
    //     this.options = options;
    //     this.selectedOption = options.slice(0,1)[0];
    //   },
    // () => {},
    // () => {
    //   console.log("signup Component options=", this.options);
    // }
    // );
    // this.optionService.getOptions().subscribe((options) => {
    //     console.log(options);
    //     this.options = options;
    //   },
    //   (err) => {
    //     console.log("error: ", err)
    //   }, () => {
    //     console.log("signupOptions = ", this.options);
    //   });


    // this.optionService.testCombinaison().pipe((lol) => {this.initForm()})subscribe( options => {
    //   map(this.options = options, );
    // });
    // console.log(this.optionService.testCombinaison());

  }

  onSubmit() {
    console.log(this.signupForm);
  }

  initForm() {
    let lastName = 'dsfs';
    let firstName = ' ';
    let username = '';
    let password = '';
    let reenterPassword = '';
    let email = '';
    let optionList = this.options;
    // console.log(this.options);
    // console.log("Option length = ",this.options.length);
    let selectedOption = this.selectedOption;

    //TODO Create a password (strenght) validator
    this.signupForm = new FormGroup({
      'firstName': new FormControl(firstName, Validators.required),
      'lastName': new FormControl(lastName, Validators.required),
      'username': new FormControl(username, Validators.required),
      'password': new FormControl(password, [Validators.required, Validators.minLength(8)]),
      'reenterPassword': new FormControl(reenterPassword,
        [Validators.required, Validators.minLength(8)]),
      'email': new FormControl(email, Validators.required),
      'selectedOption': new FormControl(this.selectedOption, Validators.required),
    });
  }

}
