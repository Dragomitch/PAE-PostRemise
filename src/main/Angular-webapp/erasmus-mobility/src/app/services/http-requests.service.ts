import {Injectable} from '@angular/core';
import {environment} from "../../environments/environment.dev";
import {Option} from "../models/option.model";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {Observable} from "rxjs";
import {tap} from "rxjs/operators";

@Injectable()
export class  HttpRequestsService {
  ourHeaders : HttpHeaders = new
    HttpHeaders().set("Content-Type", "application/x-www-form-urlencoded");//set("session", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyUm9sZSI6IlN0dWRlbnQiLCJ1c2VySWQiOjMsInRpbWVzdGFtcCI6eyJuYW5vIjo1NTIwMDAwMDAsImVwb2NoU2Vjb25kIjoxNTY5NTk5NDA0fX0.4dYcJZPu56CsrIlKM2Ud260QNH2zb085V0AEKc78ZtA"); //set("Content-Type", "application/x-www-form-urlencoded");

  constructor(private httpClient: HttpClient) {
  }

  public getOptions(): Observable<Option[]> {
    return this.httpClient.get<Option[]>(environment.api_url + 'options');
  }

  public signin(username: string, password: string): Observable<any> {
    return this.httpClient.post(environment.api_url + 'session', {username: username, passsword: password },
      {headers: this.ourHeaders}) ;
    //.pipe(tap( res => localStorage.setItem('access_token', res['access_token'])))
    //TODO also store the expiration time when this is also send
  }


  public getMobilityChoices(){
    return this.httpClient.get(environment.api_url + 'mobilityChoices', {headers: this.ourHeaders})//TODO We have to pass the token from the session, Lets check howtoDo
  }

  public signup(form: any){

  }



}
