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
    /*const example = of(2000, 1000).pipe(
      concatMap(val =>
        of(`Delayed by: ${val}ms`).pipe(delay(val))
      )
    );

    const subscribe = example.subscribe(val =>
      console.log(`With concatMap: ${val}`));*/


    return this.httpRequestService.signin(username, password).pipe(
      concatMap( res =>
        this.redirectAfterSignin(res)
      ));

        //of(console.log(payload))
      /*.subscribe( (userData) => {
      SessionService.userData = new User(userData['email'],
                                userData['firstName'],
                                userData['lastName'],
                                userData['option'],
                                userData['role'],
                                userData['version']);
      SessionService.session = new Session (userData['jwt_token'], 0);
      return true;
      });*/



  }




  public signout(){
    localStorage.removeItem('access_token');
    //TODO also remove the expiration time when this is also send
  }

  public redirectAfterSignin(userData): Observable<any>{
    console.log("Object.values = ");
    console.log(userData);
    localStorage.setItem('session', userData['access_token']);

    this.userData = new User(userData['email'],
                                      userData['firstName'],
                                      userData['lastName'],
                                      userData['option'],
                                      userData['role'],
                                      userData['version']);
    if(this.userData.role == "Professor")
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
