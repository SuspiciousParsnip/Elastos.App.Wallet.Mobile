import { Component, OnInit } from '@angular/core';
import {BaseComponent} from './../../../app/BaseComponent';
import {Config} from '../../../providers/Config';
import { Util } from '../../../providers/Util';

@Component({
  selector: 'app-recordinfo',
  templateUrl: './recordinfo.component.html',
  // styleUrls: ['./recordinfo.component.scss']
})
export class RecordinfoComponent extends BaseComponent implements OnInit {
  masterWalletId:string = "1";
  transactionRecord: any = {};

  start = 0;

  blockchain_url = Config.BLOCKCHAIN_URL;

  ngOnInit() {
    this.masterWalletId = Config.getCurMasterWalletId();
    this.setTitleByAssets('text-record');
    let txId = this.getNavParams().get("txId");
    let chainId = this.getNavParams().get("chainId");
    this.walletManager.getAllTransaction(this.masterWalletId,chainId, this.start, txId, (data) => {
      if(data["success"]){
        console.log("====getAllTransaction====="+JSON.stringify(data));
        let allTransaction = JSON.parse(data['success']);
        let transactions = allTransaction['Transactions'];
        let transaction = transactions[0];
        let timestamp = transaction['Timestamp']*1000;
        let datetime = Util.dateFormat(new Date(timestamp), 'yyyy-MM-dd HH:mm:ss');
        let summary = transaction['Summary'];
        let incomingAmount = summary["Incoming"]['Amount'];
        let outcomingAmount = summary["Outcoming"]['Amount'];
        let incomingAddress = summary["Incoming"]['ToAddress'];
        let outcomingAddress = summary["Outcoming"]['ToAddress'];
        let balanceResult = incomingAmount - outcomingAmount;
        let status = '';
        switch(summary["Status"])
        {
          case 'Confirmed':
            status = 'Confirmed'
            break;
          case 'Pending':
            status = 'Pending'
            break;
          case 'Unconfirmed':
            status = 'Unconfirmed'
            break;
        }
        this.transactionRecord = {
          name: chainId,
          status: status,
          balance: balanceResult/Config.SELA,
          incomingAmount: incomingAmount/Config.SELA,
          outcomingAmount: outcomingAmount/Config.SELA,
          incomingAddress: incomingAddress,
          outcomingAddress: outcomingAddress,
          txId: txId,
          transactionTime: datetime,
          timestamp: timestamp,
          payfees: summary['Fee']/Config.SELA,
          confirmCount: summary["ConfirmStatus"],
          remark: summary["Remark"]
        }
      }else{
          alert("======getAllTransaction====error"+JSON.stringify(data));
      }

    });
  }

  onNext(address){
    this.native.copyClipboard(address);
    this.toast('copy-ok');
  }

}
