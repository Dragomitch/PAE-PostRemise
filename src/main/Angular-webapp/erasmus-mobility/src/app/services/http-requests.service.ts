import {Injectable} from '@angular/core';
import {environment} from "../../environments/environment.dev";
import {Option} from "../models/option.model";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {OptionService} from "./option.service";

@Injectable()

export class HttpRequestsService {

  constructor(private httpClient: HttpClient,
              private optionService: OptionService) {
  }

  public getOptions(){
    this.httpClient.get<Option[]>(environment.url + 'options').subscribe(
      (options) => {
        this.optionService.setOptions(options);
      }
    );
  }

}
