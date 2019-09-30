import {Injectable} from '@angular/core';
import {Option} from "../models/option.model";
import {Observable, of, pipe, Subject} from "rxjs";
import {HttpRequestsService} from "./http-requests.service";
import {map, switchMap} from "rxjs/operators";

@Injectable()
export class OptionService {
  private options: Option[] = [
    new Option('BIN', 'Bachelier en informatique de gestion', 0),
    new Option('BBM', 'Bachelier en biologie médicale', 0),
    new Option('BCH', 'Bachelier en chimie', 0),
    new Option('BDI', 'Bachelier en diététique', 0),
    new Option('BIM', 'Bachelier en imagerie médicale', 0)];
  //TODO Find how to retrieve an initial state from the server
  public readonly options$: Observable<Option[]>;
  private selectedOption: number;
  optionsChanged$: Subject<Option[]> = new Subject<Option[]>();
  selectedOption$: Subject<number> = new Subject<number>();
  selectedOptionIndex: number = Option.EMPTY_STATE_INDEX;

  constructor(private httpRequestsService: HttpRequestsService) {
    this.selectedOption$.subscribe((index: number) => {
      this.selectedOptionIndex = index;
      this.options = new Array(0);
    });

    this.optionsChanged$.subscribe((options: Option[]) => {
      this.options = options;
    });

    this.options$ = of(this.options);
  }

  setOptions(options: Option[]) {// Is this necessary ? We will never use it in fact ?
    this.options = options;
    this.optionsChanged$.next(this.options);
  }

  getOptions(): Option[] {
    return this.options;
  }

  public getOptions$(): Observable<Option[]>{
    return this.options$;
  }

  /*getOptions(): void {
    let options;
    if (this.options == null || this.options.length === 0) {
      pipe(map(() => { return this.getHttpOptions();
      }), map(() => options = this.options.slice()),
        map((options) => {return options;}));
    }
  }*/

  /*getHttpOptions(): Observable<Option[]> {
    this.httpRequestsService.getOptions().subscribe((options: Option[]) => {
      this.options = options;
      this.selectedOption = Option.EMPTY_STATE_INDEX;
      this.selectedOption$.next(Option.EMPTY_STATE_INDEX);
      console.log("Fini Inside");
      return options;
    });

    /*return this.httpRequestsService.getOptions().pipe(map(options => {
      this.options = options;
      this.selectedOption = Option.EMPTY_STATE_INDEX;
      this.selectedOption$.next(Option.EMPTY_STATE_INDEX);
      return
    }));
    pipe(switchMap(this.httpRequestsService.getOptions()));
  };*/


  getSelectedOption(): Subject<number> {
    if (this.options.length === 0)
      this.getOptions;
    return this.selectedOption$;
  }

  getOption(index: number): Option {
    if (this.options == null || this.options.length === 0) {
      this.getOptions();
    }
    return this.options[index];

  }
}
