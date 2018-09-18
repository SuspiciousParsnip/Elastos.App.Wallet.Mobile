import {Component} from '@angular/core';
import { NavController,NavParams} from 'ionic-angular';
import {Native} from "../../../providers/Native";
import {TabsComponent} from "../../tabs/tabs.component";
import {Util} from "../../../providers/Util";


@Component({
  selector: 'app-write',
  templateUrl: './write.component.html',
})
export class WriteComponent {


  mnemonicList: Array<any> = []
  selectList: Array<any> = [];
  mnemonicStr: string;
  selectComplete = false;
  constructor(public navCtrl: NavController,public navParams: NavParams,public native: Native){
    this.init();
}
  init() {
    this.mnemonicStr = this.native.clone(this.navParams.get("mnemonicStr"));
    this.mnemonicList = this.native.clone(this.navParams.get("mnemonicList")).sort(function(){ return 0.5 - Math.random() });
  }

  onNext() {
    let mn = "";
    for(let i =0;i<this.selectList.length;i++){
      mn += this.selectList[i].text;
    }

    if(!Util.isNull(mn) && mn == this.mnemonicStr.replace(/\s+/g,"")){
      this.native.toast_trans('text-mnemonic-ok');
      this.native.setRootRouter(TabsComponent);
    }else{
      this.native.toast_trans('text-mnemonic-prompt3');
    }
  }

  public addButton(index: number, item: any): void {
    var newWord = {
      text: item.text,
      prevIndex: index
    };
    this.selectList.push(newWord);
    this.mnemonicList[index].selected = true;
    this.shouldContinue();
  }



  public removeButton(index: number, item: any): void {
    this.selectList.splice(index, 1);
    this.mnemonicList[item.prevIndex].selected = false;
    this.shouldContinue();
  }

  private shouldContinue(): void {
    this.selectComplete = this.selectList.length === this.mnemonicList.length ? true : false;
  }
}
