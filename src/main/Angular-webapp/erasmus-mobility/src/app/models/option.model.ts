export class Option {
  public static EMPTY_STATE_INDEX : number = -1;
  public code: string;
  public name: string;
  public version: number;

  constructor(code: string,
              name: string,
              version: number) {
    this.code = code;
    this.name = name;
    this.version = version;

  }

}
