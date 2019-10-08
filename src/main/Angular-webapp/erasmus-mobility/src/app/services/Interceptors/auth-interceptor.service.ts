import { Injectable } from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from "@angular/common/http";
import {Observable} from "rxjs";
import {environment} from "../../../environments/environment.dev";

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  excludedRoutes: string[] = [environment.api_url+'session'];

  intercept(req: HttpRequest<any>,
            next: HttpHandler): Observable<HttpEvent<any>> {

    const accessToken = localStorage.getItem("session");
    console.log("access token ="+accessToken);
    if (accessToken && ! this.isExcludedRoute(req.url)) {
      console.log("Request intercepted");
      console.log(req.url);
      const cloned = req.clone({
        headers: req.headers.set("Session", accessToken)
      });

      return next.handle(cloned);
    }
    else {
      console.log("Not intercepted route:"+req.url);
      return next.handle(req);
    }
  }

  public isExcludedRoute(pathToCheck: string): boolean{
    let res : number = this.excludedRoutes.indexOf(pathToCheck);
    console.log("route Excluded");
    return res != -1;
  }
}
