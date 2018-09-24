import { Component } from '@angular/core';
import { NavController, NavParams } from 'ionic-angular';
import {WalletManager} from '../../providers/WalletManager';
import {TabsComponent} from "../tabs/tabs.component";
import {Native} from "../../providers/Native";
import {Config} from "../../providers/Config";
import {LocalStorage} from "../../providers/Localstorage";
@Component({
  selector: 'page-addpublickey',
  templateUrl: 'addpublickey.html',
})
export class AddpublickeyPage {
  masterWalletId:string = "1";
  public  publicKey1:string="xpub6DXoyYMMVE2snF2A51DfVrKikRqMbMmw6JQbS5wSHVVPj7SrBR3QHXeqjGU5rb1TA3hNE7SoJhdRGpRLJg2ntRiKJiRs37jnD2kPxScTzZB";
  public  publicKey2:string="xpub6DBsmXRSKmrkVEZ5kd25mq4mwGxSWr66QY9MajeSH5nSj2hHWVDYKgT1MMHehfMhVqsqwtmqs13qgzJC7SwWUKGmwJDESXM62QUaNCTJ4vP";
  private msobj:any;
  public  publicKeyArr:any=[];
  constructor(public navCtrl: NavController, public navParams: NavParams,public walletManager:WalletManager,public native :Native,public localStorage:LocalStorage) {
    console.log("=========AddpublickeyPage"+JSON.stringify(this.navParams.data));
    this.msobj = this.navParams.data;
    let totalCopayers = this.msobj["totalCopayers"];
    for(let index=0 ;index<totalCopayers;index++){
          let  item = {};
          if(index === 0){
            item = {index:index,publicKey:this.publicKey1};
          }else if(index === 1){
            item = {index:index,publicKey:this.publicKey2};
          }
          this.publicKeyArr.push(item);
    }
    this.masterWalletId = Config.uuid(6,16);
  }

  ionViewDidLoad() {
    console.log('ionViewDidLoad AddpublickeyPage');
  }

  nextPage(){
    console.log("========"+JSON.stringify(this.publicKeyArr));
    this.createWallet();
  }

  createWallet(){
    let copayers = this.getTotalCopayers();
    console.log("====mastId===="+this.masterWalletId+"====copayers===="+copayers+"==requiredCopayers=="+this.msobj["requiredCopayers"]);
    this.walletManager.createMultiSignMasterWallet(this.masterWalletId,copayers,this.msobj["requiredCopayers"],(data)=>{
              if(data['success']){
                console.log("=====createMultiSignMasterWallet======"+JSON.stringify(data));
              }else{
                alert("=====createMultiSignMasterWallet===error=="+JSON.stringify(data));
              }
    });
   }

  getTotalCopayers(){
    let arr = [];
    for(let index = 0;index<this.publicKeyArr.length;index++){
          let item = this.publicKeyArr[index];
          arr.push(item["publicKey"]);
    }
    return JSON.stringify(arr);
  }

  createSubWallet(chainId){
    // Sub Wallet
    this.walletManager.createSubWallet(this.masterWalletId,chainId,"s12345678",true, 0, (data)=>{
          if(data["success"]){
               console.log("====createSubWallet===="+JSON.stringify(data));
               this.saveWalletList();
          }else{
                alert("createSubWallet=error:"+JSON.stringify(data));
          }
    });
  }

  saveWalletList(){
    Config.getMasterWalletIdList().push(this.masterWalletId);
    this.localStorage.setWalletList(Config.getMasterWalletIdList()).then((data)=>{
            this.localStorage.saveCurMasterId({masterId:this.masterWalletId}).then((data)=>{
              Config.setCurMasterWalletId(this.masterWalletId);
              this.native.setRootRouter(TabsComponent);
            });
    })
  }


}
