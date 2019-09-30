//
//  {lastName: "Nom", firstName: "Prenom", username: "User2", password: "pass1", reenterPassword: "pass1", …}
// email: "email2@email.com"
// firstName: "Prenom"
// lastName: "Nom"
// option:
// code: "BIN"
// __proto__: Object
// password: "pass1"
// reenterPassword: "pass1"
// username: "User2"

import {Option} from "./option.model";

export class User {
  public email: string;
  public firstname: string;
  public lastname: string;
  public option: Option;
  public version: number;
  public role: string;

  constructor(email: string,
              firstname: string,
              lastname: string,
              option: Option,
              role: string,
              version: number){
    this.email= email;
    this.firstname= firstname;
    this.lastname= lastname;
    this.option= option;
    this.role= role;
    this.version= version;
  }
}
