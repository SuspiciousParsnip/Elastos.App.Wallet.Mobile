import { Component,OnInit } from '@angular/core';
import {BaseComponent} from "./../../../app/BaseComponent";
import {IdHomeComponent} from "../../../pages/id/home/home";
import {PathlistPage} from '../../../pages/id/pathlist/pathlist';
@Component({
  selector: 'id-kyc-result',
  templateUrl: './result.html',
})
export class IdResultComponent extends BaseComponent implements OnInit{
  type: string;
  parms:any;
  ngOnInit() {
    this.setTitleByAssets('text-commit-result');
    this.parms = this.getNavParams().data;
    let status = this.parms["status"];
    if(this.isNull(status)){
      this.type = '0';
    }else{
      this.type = status;
    }

    this.setLeftIcon("",()=>{this.Go(IdHomeComponent)});
  }

  check(){
     this.Go(PathlistPage,this.parms);
  }

}
