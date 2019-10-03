import {Injectable} from "@angular/core";
import {HttpRequestsService} from "./http-requests.service";
import {User} from "../models/user.model";
import {Session} from "../models/session.model";
import {tap} from "rxjs/operators";


@Injectable()
export class SessionService{
  userData: User;
  static session: Session;

  constructor(private httpRequestService: HttpRequestsService){

  }

  public login (username: string, password: string) {
    this.httpRequestService.signin(username, password)
      .pipe(tap( res => localStorage.setItem('access_token', res['access_token'])))
      .subscribe( (userData) => {
      this.userData = new User(userData['email'],
                                userData['firstName'],
                                userData['lastName'],
                                userData['option'],
                                userData['role'],
                                userData['version']);
      SessionService.session = new Session (userData['jwt_token'], 0);
    });
    }


  public signout(){
    localStorage.removeItem('access_token');
    //TODO also remove the expiration time when this is also send
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
