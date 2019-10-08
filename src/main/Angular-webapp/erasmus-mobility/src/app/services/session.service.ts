import {Injectable} from "@angular/core";
import {HttpRequestsService} from "./http-requests.service";
import {User} from "../models/user.model";
import {Session} from "../models/session.model";
import {concatMap} from "rxjs/operators";
import {Observable} from "rxjs";


@Injectable()
export class SessionService{
  static session: Session;
  userData: User;


  constructor(private httpRequestService: HttpRequestsService){

  }

  public login (username: string, password: string) {
    return this.httpRequestService.signin(username, password).pipe(
      concatMap( res =>
        this.redirectAfterSignin(res)
      ));
  }

  public createUser()




  public signout(){
    localStorage.removeItem('access_token');
  }

  public redirectAfterSignin(userData): Observable<any>{
    localStorage.setItem('session', userData['access_token']);

    this.userData = new User(userData['email'],
                                      userData['firstName'],
                                      userData['lastName'],
                                      userData['option'],
                                      userData['role'],
                                      userData['version']);
    if(this.userData.role == User.ROLE_PROFESSOR)
    {
      return this.httpRequestService.getMobilities();
    }else{
      return this.httpRequestService.getMobilityChoices();
    }
  }

  //TODO Enable those functions when expirationTime of the tokens is also retrieved
  // public isLoggedIn() {
  //   return moment().isBefore(this.getExpiration());
  // }
  //
  // isLoggedOut() {
  //   return !this.isLoggedIn();
  // }
  //
  // getExpiration() {
  //   const expiration = localStorage.getItem("expires_at");
  //   const expiresAt = JSON.parse(expiration);
  //   return moment(expiresAt);
  // }
}
