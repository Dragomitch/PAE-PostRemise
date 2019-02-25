import {Injectable} from '@angular/core';
import {Option} from "../models/option.model";
import {HttpRequestsService} from "./http-requests.service";
import {identity, observable, Observable, Observer, of, pipe, Subject} from "rxjs";
import {flatMap, map} from "rxjs/operators";

@Injectable()
export class OptionService {
  private options: Option[] = [];
  optionsChanged$ : Subject<Option[]>= new Subject<Option[]>();
  selectedOption$ : Subject<number>= new Subject<number>();
  selectedOptionIndex : number = -1;

  constructor() {

    this.selectedOption$.subscribe( (index : number) => {
      this.selectedOptionIndex = index;
      this.selectedOption$.next(index);
    });

    this.optionsChanged$.subscribe( (options: Option[]) => {
      this.options = options;
      this.selectedOptionIndex = 0;
    });
    //TODO Find a way to create an observable from an asynchronous call
    //Test02
    //pipe()
    //   this.httpRequests.getOptions().subscribe(
    //     (options) => {
    //       this.options_data = options.slice();
    //       console.log("options data after assignement in option_service :", this.options_data);
    //     });
    //   console.log("Second check for options_data", this.options_data);
    // }
    //Test 01//
    // getOptions(): Observable<Option[]> {
    //   if(this.options$ === undefined || this.options$ === null) {
    //     // this.httpRequests.getOptions().pipe() .subscribe(
    //     //   (options) => {
    //     //     this.options_data = options.slice();
    //     //     console.log("options data after assignement in option_service :", this.options_data);
    //     //   });
    //     return this.options$ = of(this.options_data);
    //   map( (op) => { return this.httpRequests.getOptions()})

  }

  // testCombinaison(): Observable<any> {
  //   if (this.options_data === undefined || this.options_data === null) {
  //     console.log("optionData is null");
  //     this.httpRequests.getOptions().pipe(
  //       map((options) => {
  //         this.options_data = options;
  //       }));
  //   } else {
  //     console.log("optionData not null");
  //     return of(this.options_data);
  //   }
  //
  // }

  // this.options$ = new Observable(
  //   (observer) => {
  //     if (this.options$ === undefined || this.options$ === null) {
  //       await this.httpRequests.getOptions().subscribe(
  //         (options) => {
  //           this.options_data = options.slice();
  //           console.log("2)options data after inside assignement in option_service :", this.options_data);
  //         });
  //       observer.next(this.options_data);
  //     }
  //     return this.options$;
  //   });

  setOptions(options: Option[]) {
    this.options = options;
    this.optionsChanged$.next(this.options);
  }

  getOptions() : Option[] {
    return this.options.slice();
  }


}
