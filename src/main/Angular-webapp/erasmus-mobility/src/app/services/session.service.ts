import {Injectable} from "@angular/core";
import {HttpRequestsService} from "./http-requests.service";
import {User} from "../models/user.model";


@Injectable()
export class SessionService{
  //private session: Session;
  userData: User;

  constructor(private httpRequestService: HttpRequestsService){

  }

  public getSession (username: string, password: string) {
    this.httpRequestService.signin(username, password).subscribe( (userData) => {
      this.userData = new User(userData['email'],
                                userData['firstName'],
                                userData['lastName'],
                                userData['option'],
                                userData['role'],
                                userData['version']);
    });

    this.httpRequestService.getMobilityChoices().subscribe((mobilities) => {
      console.log(mobilities);
    });
  }
}
