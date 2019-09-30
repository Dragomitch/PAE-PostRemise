export class Session{
  public selfForgedToken : string;
  public expireTime: number;

  constructor(selfForgedToken: string,
              expireTime: number){
    this.selfForgedToken = selfForgedToken;
    this.expireTime = expireTime;
  }
}
