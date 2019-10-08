import {Option} from "./option.model";

export class User {
  public email: string;
  public firstname: string;
  public lastname: string;
  public option: Option;
  public version: number;
  public role: string;
  public static ROLE_STUDENT = "Student";
  public static ROLE_PROFESSOR = "Professor";

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
