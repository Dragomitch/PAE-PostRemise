import {Component} from '@angular/core';
import {OptionService} from "./services/option.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'erasmus-mobility';

  constructor(private optionService: OptionService) {

  }


}
