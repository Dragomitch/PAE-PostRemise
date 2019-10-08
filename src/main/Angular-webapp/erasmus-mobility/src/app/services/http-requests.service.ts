import {Injectable} from '@angular/core';
import {environment} from "../../environments/environment.dev";
import {Option} from "../models/option.model";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable} from "rxjs";
import {tap} from "rxjs/operators";

@Injectable()
export class  HttpRequestsService {
  ourHeaders : HttpHeaders = new
    HttpHeaders().set("Content-Type", "application/x-www-form-urlencoded");

  constructor(private httpClient: HttpClient) {
  }

  public getOptions(): Observable<Option[]> {
    return this.httpClient.get<Option[]>(environment.api_url + 'options');
  }

  public signin(username: string, password: string): Observable<any> {
    return this.httpClient.post(environment.api_url + 'session', {username: username, passsword: password },
      {headers: this.ourHeaders});
    //TODO also store the expiration time when this is also send
  }


  public getMobilityChoices(){
    return this.httpClient.get(environment.api_url + 'mobilityChoices', {headers: this.ourHeaders});
  }

  public getMobilities(){
    return this.httpClient.get(environment.api_url + 'mobilities', {headers: this.ourHeaders});
  }

  public signup(form: any){

  }



}
