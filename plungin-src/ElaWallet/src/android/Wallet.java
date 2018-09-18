package ElaWallet;

import android.util.JsonReader;

import com.elastos.spvcore.IMasterWallet;
import com.elastos.spvcore.ISubWallet;
import com.elastos.spvcore.IMainchainSubWallet;
import com.elastos.spvcore.IIdChainSubWallet;
import com.elastos.spvcore.ISidechainSubWallet;
import com.elastos.spvcore.ISubWalletCallback;
import com.elastos.spvcore.MasterWalletManager;
import com.elastos.spvcore.IdManagerFactory;
import com.elastos.spvcore.IDidManager;
import com.elastos.spvcore.IDid;
import com.elastos.spvcore.IIdManagerCallback;
import com.elastos.spvcore.WalletException;
import com.elastos.wallet.util.LogUtil;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import android.util.Log;

import io.ionic.starter.MyUtil;

/**
 * wallet webview jni
 */
public class Wallet extends CordovaPlugin {
	private static final String TAG = "Wallet.java";

	private MasterWalletManager mMasterWalletManager = null;
	private IDidManager mDidManager = null;
	private String mRootPath = null;

	private int errCodeInvalidArg                 = 10001;
	private int errCodeInvalidMasterWallet        = 10002;
	private int errCodeInvalidSubWallet           = 10003;
	private int errCodeCreateMasterWallet         = 10004;
	private int errCodeCreateSubWallet            = 10005;
	private int errCodeRecoverSubWallet           = 10006;
	private int errCodeInvalidMasterWalletManager = 10007;
	private int errCodeImportFromKeyStore         = 10008;
	private int errCodeImportFromMnemonic         = 10009;
	private int errCodeSubWalletInstance          = 10010;
	private int errCodeInvalidDIDManager          = 10011;
	private int errCodeInvalidDID                 = 10012;
	private int errCodeActionNotFound             = 10013;

	private int errCodeWalletException     = 20000;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);

		mRootPath = MyUtil.getRootPath();

		Log.i(TAG, "Initialize mRootPath = " + mRootPath);

		mMasterWalletManager = new MasterWalletManager(mRootPath);
		MyUtil.SetCurrentMasterWalletManager(mMasterWalletManager);
		ArrayList<IMasterWallet> masterWalletList = mMasterWalletManager.GetAllMasterWallets();
		for (int i = 0; i < masterWalletList.size(); i++) {
			initDidManager(masterWalletList.get(i));
		}

		if (masterWalletList.size() == 0) {
			Log.w(TAG, "Without master wallet: you should create one at least");
		}
	}

	private void initDidManager(IMasterWallet masterWallet) {
		try {
			if (mDidManager == null && masterWallet != null) {
				JSONObject basicInfo = new JSONObject(masterWallet.GetBasicInfo());
				String accountType = basicInfo.getJSONObject("Account").getString("Type");
				if (accountType == "Standard") {
					Log.i(TAG, "Master wallet '" + masterWallet.GetId() + "' create DID manager with root path '" + mRootPath + "'");
					mDidManager = IdManagerFactory.CreateIdManager(masterWallet, mRootPath);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "DID manager exception initialize");
		}
	}

	private JSONObject mkJson(String key, Object value) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(key, value);

		return jsonObject;
	}

	private void exceptionProcess(WalletException e, CallbackContext cc, Object msg) throws JSONException {
		e.printStackTrace();

		JSONObject errJson = new JSONObject();

		errJson.put("code", errCodeWalletException);
		errJson.put("message", msg);
		errJson.put("exception", e.GetErrorInfo());

		Log.e(TAG, errJson.toString());

		cc.error(mkJson("error", errJson));
	}

	private void errorProcess(CallbackContext cc, int code, Object msg) throws JSONException {
		JSONObject errJson = new JSONObject();

		errJson.put("code", code);
		errJson.put("message", msg);
		Log.e(TAG, errJson.toString());

		cc.error(mkJson("error", errJson));
	}

	private void successProcess(CallbackContext cc, Object msg) throws JSONException {
		Log.i(TAG, "" + msg);
		cc.success(mkJson("success", msg));
	}

	private IMasterWallet getMasterWallet(String masterWalletId) {
		if (mMasterWalletManager == null) {
			Log.e(TAG, "Master wallet manager has not initialize");
			return null;
		}

		ArrayList<IMasterWallet> masterWalletList = mMasterWalletManager.GetAllMasterWallets();
		for (int i = 0; i < masterWalletList.size(); i++) {
			if (masterWalletId == masterWalletList.get(i).GetId()) {
				return masterWalletList.get(i);
			}
		}

		Log.e(TAG, "Master wallet '" + masterWalletId + "' not found");
		return null;
	}

	private ISubWallet getSubWallet(String masterWalletId, String chainId) {
		IMasterWallet masterWallet = getMasterWallet(masterWalletId);
		if (masterWallet == null) {
			return null;
		}

		ArrayList<ISubWallet> subWalletList = masterWallet.GetAllSubWallets();
		for (int i = 0; i < subWalletList.size(); i++) {
			if (chainId == subWalletList.get(i).GetChainId()) {
				return subWalletList.get(i);
			}
		}

		Log.e(TAG, "sub wallet '" + chainId + "' not found");
		return null;
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext cc) {
		Log.i(TAG, "execute: action=" + action);
		try {
			switch (action) {
				case "coolMethod":
					String message = args.getString(0);
					this.coolMethod(message, cc);
					break;
				case "print":
					this.print(args.getString(0), cc);
					break;
				case "createSubWallet":
					this.createSubWallet(args, cc);
					break;
				case "getPublicKey":
					this.getPublicKey(args, cc);
					break;
				case "createMasterWallet":
					this.createMasterWallet(args, cc);
					break;
				case "createMultiSignMasterWallet":
					this.createMultiSignMasterWallet(args, cc);
					break;
				case "recoverSubWallet":
					this.recoverSubWallet(args, cc);
					break;
				case "importWalletWithKeystore":
					this.importWalletWithKeystore(args, cc);
					break;
				case "importWalletWithMnemonic":
					this.importWalletWithMnemonic(args, cc);
					break;
				case "exportWalletWithKeystore":
					this.exportWalletWithKeystore(args, cc);
					break;
				case "exportWalletWithMnemonic":
					this.exportWalletWithMnemonic(args, cc);
					break;
				case "getBalanceInfo":
					this.getBalanceInfo(args, cc);
					break;
				case "getBalance":
					this.getBalance(args, cc);
					break;
				case "createAddress":
					this.createAddress(args, cc);
					break;
				case "getAllAddress":
					this.getAllAddress(args, cc);
					break;
				case "getBalanceWithAddress":
					this.getBalanceWithAddress(args, cc);
					break;
				case "createTransaction":
					this.createTransaction(args, cc);
					break;
				case "createMultiSignTransaction":
					this.createMultiSignTransaction(args, cc);
					break;
				case "appendSignToTransaction":
					this.appendSignToTransaction(args, cc);
					break;
				case "getAllTransaction":
					this.getAllTransaction(args, cc);
					break;
				case "sign":
					this.sign(args, cc);
					break;
				case "checkSign":
					this.checkSign(args, cc);
					break;
				case "registerWalletListener":
					this.registerWalletListener(args, cc);
					break;
				case "getAllMasterWallets":
					this.getAllMasterWallets(cc);
					break;
				case "masterWalletGetBasicInfo":
					this.masterWalletGetBasicInfo(args, cc);
					break;
				case "getAllSubWallets":
					this.getAllSubWallets(args, cc);
					break;
				case "saveConfigs":
					this.saveConfigs(cc);
					break;
				case "isAddressValid":
					this.isAddressValid(args, cc);
					break;
				case "generateMnemonic":
					this.generateMnemonic(args, cc);
					break;
				case "destroyWallet":
					this.destroyWallet(args, cc);
					break;
				case "getSupportedChains":
					this.getSupportedChains(args, cc);
					break;
				case "changePassword":
					this.changePassword(args, cc);
					break;
				case "sendRawTransaction":
					this.sendRawTransaction(args, cc);
					break;
				case "calculateTransactionFee":
					this.calculateTransactionFee(args, cc);
					break;
				case "createIdTransaction":
					this.createIdTransaction(args, cc);
					break;
				case "createDepositTransaction":
					this.createDepositTransaction(args, cc);
					break;

					//did
				case "createDID":
					this.createDID(args, cc);
					break;
				case "didGenerateProgram":
					this.didGenerateProgram(args, cc);
					break;
				case "getDIDList":
					this.getDIDList(cc);
					break;
				case "destoryDID":
					this.destoryDID(args, cc);
					break;
				case "didSetValue":
					this.didSetValue(args, cc);
					break;
				case "didGetValue":
					this.didGetValue(args, cc);
					break;
				case "didGetHistoryValue":
					this.didGetHistoryValue(args, cc);
					break;
				case "didGetAllKeys":
					this.didGetAllKeys(args, cc);
					break;
				case "didSign":
					this.didSign(args, cc);
					break;
				case "didCheckSign":
					this.didCheckSign(args, cc);
					break;
				case "didGetPublicKey":
					this.didGetPublicKey(args, cc);
					break;
				case "registerIdListener":
					this.registerIdListener(args, cc);
					break;
				case "createWithdrawTransaction":
					this.createWithdrawTransaction(args, cc);
					break;
				case "getGenesisAddress":
					this.getGenesisAddress(args, cc);
					break;
				default:
					errorProcess(cc, errCodeActionNotFound, "action '" + action + "' not found, please check!");
					return false;
			}
		} catch (JSONException e) {
			Log.e(TAG, "execute exception");
			e.printStackTrace();
			cc.error("{\"Exception\": \"json parse execute error\"}");
			return false;
		}

		return true;
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String payPassword
	// args[3]: boolean singleAddress
	// args[4]: long feePerKb
	public void createSubWallet(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId = args.getString(idx++);
		String payPassword = args.getString(idx++);
		boolean singleAddress = args.getBoolean(idx++);
		long feePerKb = args.getLong(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = getMasterWallet(masterWalletId);
			if (masterWallet == null) {
				errorProcess(cc, errCodeInvalidMasterWallet, "Get master wallet '" + masterWalletId + "' fail");
				return;
			}

			ISubWallet subWallet = masterWallet.CreateSubWallet(chainId, payPassword, singleAddress, feePerKb);
			if (subWallet == null) {
				errorProcess(cc, errCodeCreateSubWallet, "Master wallet '" + masterWalletId + "' create subwallet '" + chainId + "' fail");
				return;
			}
			successProcess(cc, "Master wallet '" + masterWalletId + "' create subwallet '" + chainId + "' successfully");
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' create subwallet '" + chainId + "'");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String payPassword
	// args[3]: boolean singleAddress
	// args[4]: int limitGap
	// args[5]: long feePerKb
	public void recoverSubWallet(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId = args.getString(idx++);
		String payPassword = args.getString(idx++);
		boolean singleAddress = args.getBoolean(idx++);
		int limitGap = args.getInt(idx++);
		long feePerKb = args.getLong(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = getMasterWallet(masterWalletId);
			if (masterWallet == null) {
				errorProcess(cc, errCodeInvalidMasterWallet, "Get master wallet '" + masterWalletId + "' fail");
				return;
			}

			ISubWallet subWallet = masterWallet.RecoverSubWallet(chainId, payPassword, singleAddress, limitGap, feePerKb);
			if (subWallet == null) {
				errorProcess(cc, errCodeRecoverSubWallet, "Master wallet '" + masterWalletId + "' recover subwallet '" + chainId + "' fail");
				return;
			}
			successProcess(cc, "Master wallet '" + masterWalletId + "' recover subwallet '" + chainId + "' successfully");
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' recover subwallet '" + chainId + "'");
		}
	}

	public void getAllMasterWallets(CallbackContext cc) throws JSONException {
		try {
			ArrayList<IMasterWallet> masterWalletList = mMasterWalletManager.GetAllMasterWallets();
			JSONArray masterWalletListJson = new JSONArray();

			if (masterWalletList.size() == 0) {
				errorProcess(cc, errCodeInvalidMasterWallet, "Don't have any master wallet");
				return;
			}

			for (int i = 0; i < masterWalletList.size(); i++) {
				masterWalletListJson.put(masterWalletList.get(i).GetId());
			}
			successProcess(cc, masterWalletListJson.toString());
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Get all master wallets exception");
		}
	}

	// args[0]: String masterWalletId
	public void masterWalletGetBasicInfo(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = getMasterWallet(masterWalletId);
			if (masterWallet == null) {
				errorProcess(cc, errCodeInvalidMasterWallet, "Get master wallet '" + masterWalletId + "' fail");
				return;
			}

			successProcess(cc, masterWallet.GetBasicInfo());
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' get basic info");
		}
	}

	// args[0]: String masterWalletId
	public void getAllSubWallets(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = getMasterWallet(masterWalletId);
			if (masterWallet == null) {
				errorProcess(cc, errCodeInvalidMasterWallet, "get master wallet '" + masterWalletId + "' fail");
				return;
			}

			ArrayList<ISubWallet> subWalletList = masterWallet.GetAllSubWallets();
			if (subWalletList.size() == 0) {
				errorProcess(cc, errCodeInvalidSubWallet, "master wallet '" + masterWalletId + "' don't have any subwallet");
				return;
			}

			JSONArray subWalletJsonArray = new JSONArray();
			for (int i = 0; i < subWalletList.size(); i++) {
				subWalletJsonArray.put(subWalletList.get(i).GetChainId());
			}
			successProcess(cc, subWalletJsonArray.toString());
		} catch (WalletException e) {
			exceptionProcess(e, cc, "master wallet '" + masterWalletId + "' get all subwallets");
		}
	}

	public void saveConfigs(CallbackContext cc) throws JSONException {
		if (mMasterWalletManager == null) {
			errorProcess(cc, errCodeInvalidMasterWalletManager, "Master wallet manager has not initialize");
			return;
		}

		try {
			mMasterWalletManager.SaveConfigs();
			successProcess(cc, "Configuration files save successfully");
		} catch(WalletException e) {
			exceptionProcess(e, cc, "Master wallet manager save configuration files");
		}
	}

	// args[0]: String language
	public void generateMnemonic(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String language = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mMasterWalletManager == null) {
			errorProcess(cc, errCodeInvalidMasterWalletManager, "Master wallet manager has not initialize");
			return;
		}

		try {
			String mnemonic = mMasterWalletManager.GenerateMnemonic(language);
			Log.i(TAG, "Generate mnemonic in '" + language + "'");
			cc.success(mkJson("success", mnemonic));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Generate mnemonic in '" + language + "'");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String address
	public void isAddressValid(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String addr = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = getMasterWallet(masterWalletId);
			if (masterWallet == null) {
				errorProcess(cc, errCodeInvalidMasterWallet, "get master wallet '" + masterWalletId + "' fail");
				return;
			}

			boolean valid = masterWallet.IsAddressValid(addr);
			successProcess(cc, valid);
		} catch (WalletException e) {
			exceptionProcess(e, cc, "is address valid of master wallet '" + masterWalletId + "'");
		}
	}

	// args[0]: String masterWalletId
	public void getPublicKey(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = getMasterWallet(masterWalletId);
			if (masterWallet == null) {
				errorProcess(cc, errCodeInvalidMasterWallet, "get master wallet '" + masterWalletId + "' fail");
				return;
			}

			String publicKey = masterWallet.GetPublicKey();
			successProcess(cc, publicKey);
		} catch (WalletException e) {
			exceptionProcess(e, cc, "master wallet '" + masterWalletId + "' get public key");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String mnemonic
	// args[2]: String phrasePassword
	// args[3]: String payPassword
	// args[4]: String language
	public void createMasterWallet(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String mnemonic       = args.getString(idx++);
		String phrasePassword = args.getString(idx++);
		String payPassword    = args.getString(idx++);
		String language       = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = mMasterWalletManager.CreateMasterWallet(
					masterWalletId, mnemonic, phrasePassword, payPassword, language);

			if (masterWallet == null) {
				errorProcess(cc, errCodeCreateMasterWallet, "create master wallet '" + masterWalletId + "' failed");
				return;
			}
			initDidManager(masterWallet);
			successProcess(cc, "create master wallet '" + masterWalletId + "' = " + masterWallet + " OK");
		} catch (WalletException e) {
			exceptionProcess(e, cc, "create master wallet '" + masterWalletId + "'");
		}
	}

	// args.length() == 4
	//		args[0]: String masterWalletId
	//		args[1]: String payPassword
	//		args[2]: String coSigners
	//		args[3]: int requiredSignCount
	// args.length() == 5
	//		args[0]: String masterWalletId
	//		args[1]: String privKey
	//		args[2]: String payPassword
	//		args[3]: String coSigners
	//		args[4]: int requiredSignCount
	public void createMultiSignMasterWallet(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;
		String privKey = null;

		if (args.length() != 4 && args.length() != 5) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		String masterWalletId = args.getString(idx++);
		if (args.length() == 5)
			privKey           = args.getString(idx++);
		String payPassword    = args.getString(idx++);
		String coSigners      = args.getString(idx++);
		int requiredSignCount = args.getInt(idx++);

		try {
			IMasterWallet masterWallet = null;
			if (args.length() == 4) {
				masterWallet = mMasterWalletManager.CreateMultiSignMasterWallet(
						masterWalletId, payPassword, coSigners, requiredSignCount);
			} else if (args.length() == 5) {
				masterWallet = mMasterWalletManager.CreateMultiSignMasterWallet(
						masterWalletId, privKey, payPassword, coSigners, requiredSignCount);
			} else {
				errorProcess(cc, errCodeInvalidArg, "Invalid args length");
				return;
			}

			if (masterWallet == null) {
				errorProcess(cc, errCodeCreateMasterWallet, "Create multi sign master wallet '" + masterWalletId + "' failed");
				return;
			}

			initDidManager(masterWallet);
			successProcess(cc, "Create multi sign master wallet '" + masterWalletId + "' = " + masterWallet + " OK");
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Create multi sign master wallet '" + masterWalletId + "'");
		}
	}

	// args[0]: String masterWalletId
	public void destroyWallet(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			mMasterWalletManager.DestroyWallet(masterWalletId);
			successProcess(cc, "Destroy master wallet '" + masterWalletId + "' OK");
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Destroy master wallet '" + masterWalletId + "'");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String keystoreContent
	// args[2]: String backupPassword
	// args[3]: String payPassword
	// args[4]: String phrasePassword
	public void importWalletWithKeystore(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId  = args.getString(idx++);
		String keystoreContent = args.getString(idx++);
		String backupPassword  = args.getString(idx++);
		String payPassword     = args.getString(idx++);
		String phrasePassword  = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = mMasterWalletManager.ImportWalletWithKeystore(
					masterWalletId, keystoreContent, backupPassword, payPassword, phrasePassword);
			if (masterWallet == null) {
				errorProcess(cc, errCodeImportFromKeyStore, "Import master wallet '" + masterWalletId + "' with keystore failed");
				return;
			}

			initDidManager(masterWallet);

			successProcess(cc, "Import master wallet '" + masterWalletId + "' with keystore OK");
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Import master wallet '" + masterWalletId + "' with keystore");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String mnemonic
	// args[2]: String phrasePassword
	// args[3]: String payPassword
	// args[4]: String language
	public void importWalletWithMnemonic(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String mnemonic       = args.getString(idx++);
		String phrasePassword = args.getString(idx++);
		String payPassword    = args.getString(idx++);
		String language       = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = mMasterWalletManager.ImportWalletWithMnemonic(
					masterWalletId, mnemonic, phrasePassword, payPassword, language);
			if (masterWallet == null) {
				errorProcess(cc, errCodeImportFromMnemonic, "import master wallet '" + masterWalletId + "' with mnemonic failed");
				return;
			}

			initDidManager(masterWallet);
			successProcess(cc, "import master wallet '" + masterWalletId + "' with mnemonic OK");
		} catch (WalletException e) {
			exceptionProcess(e, cc, "import master wallet '" + masterWalletId + "' with mnemonic");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String backupPassword
	// args[2]: String payPassword
	public void exportWalletWithKeystore(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String backupPassword = args.getString(idx++);
		String payPassword    = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = getMasterWallet(masterWalletId);
			if (masterWallet == null) {
				errorProcess(cc, errCodeInvalidMasterWallet, "get master wallet '" + masterWalletId + "' fail");
				return;
			}

			String keystore = mMasterWalletManager.ExportWalletWithKeystore(masterWallet, backupPassword, payPassword);
			successProcess(cc, keystore);
		} catch (WalletException e) {
			exceptionProcess(e, cc, "export master wallet '" + masterWalletId + "' with keystore");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String backupPassword
	public void exportWalletWithMnemonic(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String backupPassword = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = getMasterWallet(masterWalletId);
			if (masterWallet == null) {
				errorProcess(cc, errCodeInvalidMasterWallet, "get master wallet '" + masterWalletId + "' fail");
				return;
			}

			String mnemonic = mMasterWalletManager.ExportWalletWithMnemonic(masterWallet, backupPassword);
			cc.success(mkJson("Success", mnemonic));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "export master wallet '" + masterWalletId + "' with mnemonic");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String fromAddress
	// args[3]: String toAddress
	// args[4]: long amount
	// args[5]: String memo
	// args[6]: String remark
	public void createTransaction(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		String fromAddress    = args.getString(idx++);
		String toAddress      = args.getString(idx++);
		long   amount         = args.getLong(idx++);
		String memo           = args.getString(idx++);
		String remark         = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return ;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			String tx = subWallet.CreateTransaction(fromAddress, toAddress, amount, memo, remark);
			successProcess(cc, tx);
		} catch (WalletException e) {
			exceptionProcess(e, cc, "create tx of master wallet '" + masterWalletId + "' subwallet '" + chainId + "'");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String fromAddress
	// args[3]: String toAddress
	// args[4]: long amount
	// args[5]: String memo
	public void createMultiSignTransaction(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		String fromAddress    = args.getString(idx++);
		String toAddress      = args.getString(idx++);
		long   amount         = args.getLong(idx++);
		String memo           = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return ;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			String tx = subWallet.CreateMultiSignTransaction(fromAddress, toAddress, amount, memo);
			successProcess(cc, tx);
		} catch (WalletException e) {
			exceptionProcess(e, cc, "create multi sign tx of master wallet '" + masterWalletId + "' subwallet '" + chainId + "'");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String rawTransaction
	// args[3]: String payPassword
	public void appendSignToTransaction(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		String rawTransaction = args.getString(idx++);
		String payPassword    = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			String tx = subWallet.AppendSignToTransaction(rawTransaction, payPassword);
			successProcess(cc, tx);
		} catch (WalletException e) {
			exceptionProcess(e, cc, "append sign to tx");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String rawTransaction
	// args[3]: long fee
	public void publishMultiSignTransaction(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		String rawTransaction = args.getString(idx++);
		long   fee            = args.getLong(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			String result = subWallet.PublishMultiSignTransaction(rawTransaction, fee);
			successProcess(cc, result);
		} catch (WalletException e) {
			exceptionProcess(e, cc, "publish multi sign tx");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: int start
	// args[3]: int count
	// args[4]: String addressOrTxId
	public void getAllTransaction(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		int    start          = args.getInt(idx++);
		int    count          = args.getInt(idx++);
		String addressOrTxId  = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			String result = subWallet.GetAllTransaction(start, count, addressOrTxId);
			successProcess(cc, result);
		} catch (WalletException e) {
			exceptionProcess(e, cc, "get all tx");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	public void registerWalletListener(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			subWallet.AddCallback(new ISubWalletCallback() {
				@Override
				public void OnTransactionStatusChanged(String txId, String status, String desc, int confirms) {
					JSONObject jsonObject = new JSONObject();
					Log.i(TAG, "OnTransactionStatusChanged");
					try {
						jsonObject.put("txId", txId);
						jsonObject.put("status", status);
						jsonObject.put("desc", desc);
						jsonObject.put("confirms", confirms);
					} catch (JSONException e) {
						e.printStackTrace();
					}

					PluginResult pluginResult = new PluginResult(PluginResult.Status.OK,jsonObject);
					pluginResult.setKeepCallback(true);
					cc.sendPluginResult(pluginResult);
				}

				@Override
				public void OnBlockSyncStarted() {
					JSONObject jsonObject = new JSONObject();
					Log.i(TAG, "OnBlockSyncStarted");
					try {
						jsonObject.put("OnBlockSyncStarted", "OnBlockSyncStarted");
					}
					catch (JSONException e) {
						e.printStackTrace();
					}

					PluginResult pluginResult = new PluginResult(PluginResult.Status.OK,jsonObject);
					pluginResult.setKeepCallback(true);
					cc.sendPluginResult(pluginResult);
				}

				@Override
				public void OnBlockHeightIncreased(int currentBlockHeight, double progress) {
					JSONObject jsonObject = new JSONObject();
					Log.i(TAG, "OnBlockHeightIncreased");
					try {
						jsonObject.put("currentBlockHeight", currentBlockHeight);
						jsonObject.put("progress", progress);
					}
					catch (JSONException e) {
						e.printStackTrace();
					}

					PluginResult pluginResult = new PluginResult(PluginResult.Status.OK,jsonObject);
					pluginResult.setKeepCallback(true);
					cc.sendPluginResult(pluginResult);
				}

				@Override
				public void OnBlockSyncStopped() {
					JSONObject jsonObject = new JSONObject();
					Log.i(TAG, "OnBlockSyncStopped");
					try {
						jsonObject.put("OnBlockSyncStopped", "OnBlockSyncStopped");
					}
					catch (JSONException e) {
						e.printStackTrace();
					}

					PluginResult pluginResult = new PluginResult(PluginResult.Status.OK,jsonObject);
					pluginResult.setKeepCallback(true);
					cc.sendPluginResult(pluginResult);
				}

				@Override
				public void OnDestroyWallet() {
					JSONObject jsonObject = new JSONObject();
					Log.i(TAG, "OnDestroyWallet");
					try {
						jsonObject.put("OnDestroyWallet", "OnDestroyWallet");
					} catch (JSONException e) {
						e.printStackTrace();
					}

					PluginResult pluginResult = new PluginResult(PluginResult.Status.OK,jsonObject);
					pluginResult.setKeepCallback(true);
					cc.sendPluginResult(pluginResult);
				}
			});
		} catch (WalletException e) {
			exceptionProcess(e, cc, "add callback for subwallet '" + chainId + "' of master wallet '" + masterWalletId + "'");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	public void getBalanceInfo(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}
			successProcess(cc, subWallet.GetBalanceInfo());
		} catch (WalletException e) {
			exceptionProcess(e, cc, "master wallet '" + masterWalletId + "' subwallet '" + chainId + "' get balance info");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	public void getBalance(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			successProcess(cc, subWallet.GetBalance());
		} catch (WalletException e) {
			exceptionProcess(e, cc, "master wallet '" + masterWalletId + "' subwallet '" + chainId + "' get balance");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	public void createAddress(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			successProcess(cc, subWallet.CreateAddress());
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' subwallet ' " + chainId + "' create address");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: int start
	// args[3]: int count
	public void getAllAddress(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		int    start          = args.getInt(idx++);
		int    count          = args.getInt(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "Get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			successProcess(cc, subWallet.GetAllAddress(start, count));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "master wallet '" + masterWalletId + "' subwallet '" + chainId + "' get all addresses");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String address
	public void getBalanceWithAddress(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		String address        = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "Get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			successProcess(cc, subWallet.GetBalanceWithAddress(address));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' subwallet '" + chainId + "' get balance with address");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String message
	// args[3]: String payPassword
	public void sign(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		String message        = args.getString(idx++);
		String payPassword    = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "Get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			successProcess(cc, subWallet.Sign(message, payPassword));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' subWallet '" + chainId + "' sign");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String publicKey
	// args[3]: String message
	// args[4]: String signature
	public void checkSign(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		String publicKey      = args.getString(idx++);
		String message        = args.getString(idx++);
		String signature      = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "Get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			successProcess(cc, subWallet.CheckSign(publicKey, message, signature));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' subwallet '" + chainId + "' check sign");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String transactionJson
	// args[3]: long fee
	// args[4]: String payPassword
	public void sendRawTransaction(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		String txJson         = args.getString(idx++);
		long   fee            = args.getLong(idx++);
		String payPassword    = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "Get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			successProcess(cc, subWallet.SendRawTransaction(txJson, fee, payPassword));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' subwallet '" + chainId + "' send raw tx");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String rawTransaction
	// args[3]: long feePerKb
	public void calculateTransactionFee(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		String rawTransaction = args.getString(idx++);
		long   feePerKb       = args.getLong(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "Get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			successProcess(cc, subWallet.CalculateTransactionFee(rawTransaction, feePerKb));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' subWallet '" + chainId + "' calculate tx fee");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String fromAddress
	// args[3]: String payloadJson
	// args[4]: String programJson
	// args[5]: String memo
	// args[6]: String remark
	public void createIdTransaction(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		String fromAddress    = args.getString(idx++);
		String payloadJson    = args.getString(idx++);
		String programJson    = args.getString(idx++);
		String memo           = args.getString(idx++);
		String remark         = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "Get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			if (! (subWallet instanceof IIdChainSubWallet)) {
				errorProcess(cc, errCodeSubWalletInstance, "subwallet '" + chainId + "' is not instance of IIdChainSubWallet");
				return;
			}

			IIdChainSubWallet idchainSubWallet = (IIdChainSubWallet)subWallet;

			successProcess(cc, idchainSubWallet.CreateIdTransaction(fromAddress, payloadJson, programJson, memo, remark));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "master wallet '" + masterWalletId + "' subwallet '" + chainId + "' create id tx");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String fromAddress
	// args[3]: String toAddress
	// args[4]: long amount
	// args[5]: String sideAccountJson
	// args[6]: String sideAmountJson
	// args[7]: String sideIndicesJson
	// args[8]: String memo
	// args[9]: String remark
	public void createDepositTransaction(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);
		String fromAddress    = args.getString(idx++);
		String toAddress      = args.getString(idx++);
		long   amount         = args.getLong(idx++);
		String sideAccountJson = args.getString(idx++);
		String sideAmountJson  = args.getString(idx++);
		String sideIndicesJson = args.getString(idx++);
		String memo            = args.getString(idx++);
		String remark          = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "Get subwalelt '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			if (! (subWallet instanceof IMainchainSubWallet)) {
				errorProcess(cc, errCodeSubWalletInstance, "Subwallet '" + chainId + "' is not instance of IMainchainSubWallet");
				return;
			}

			IMainchainSubWallet mainchainSubWallet = (IMainchainSubWallet)subWallet;

			String txJson = mainchainSubWallet.CreateDepositTransaction(fromAddress, toAddress, amount,
					sideAccountJson, sideAmountJson, sideIndicesJson, memo, remark);
			successProcess(cc, txJson);
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' subwallet '" + chainId + "' create deposit tx");
		}
	}

	// args[0]: masterWalletId
	public void getSupportedChains(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = getMasterWallet(masterWalletId);
			if (masterWallet == null) {
				errorProcess(cc, errCodeInvalidMasterWallet, "Get master wallet '" + masterWalletId + "' fail");
				return;
			}

			String[] supportedChains = masterWallet.GetSupportedChains();
			JSONArray supportedChainsJson = new JSONArray();
			for (int i = 0; i < supportedChains.length; i++) {
				supportedChainsJson.put(supportedChains[i]);
			}

			successProcess(cc, supportedChainsJson);
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' get support chain");
		}
	}

	// args[0]: masterWalletId
	// args[1]: oldPassword
	// args[2]: newPassword
	public void changePassword(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String oldPassword    = args.getString(idx++);
		String newPassword    = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			IMasterWallet masterWallet = getMasterWallet(masterWalletId);
			if (masterWallet == null) {
				errorProcess(cc, errCodeInvalidMasterWallet, "Get master wallet '" + masterWalletId + "' fail");
				return;
			}

			masterWallet.ChangePassword(oldPassword, newPassword);
			successProcess(cc, "Change password OK");
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' change password");
		}
	}

	private JSONObject parseOneParam(String key, Object value) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(key, value);
		return jsonObject;
	}

	private void coolMethod(String message, CallbackContext cc) {
		if (message != null && message.length() > 0) {
			cc.success(message);
		} else {
			cc.error("Expected one non-empty string argument.");
		}
	}

	public void print(String text, CallbackContext cc) throws JSONException {
		if (text == null) {
			cc.error("Text not can be null");
		} else {
			LogUtil.i(TAG, text);
			cc.success(parseOneParam("text", text));
		}
	}


	//IDIDManager
	// args[0]: String password
	public void createDID(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String password = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {
			IDid did = mDidManager.CreateDID(args.getString(0));
			successProcess(cc, did.GetDIDName());
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Create DID");
		}
	}

	// return value
	// { "Error": String message }
	// { "Success": Object result }
	// { "Exception": String message }
	public void getDIDList(CallbackContext cc) throws JSONException {
		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {
			successProcess(cc, mDidManager.GetDIDList());
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Get DID list");
		}
	}

	// args[0]: String didName
	public void destoryDID(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String didName = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {
			mDidManager.DestoryDID(didName);
			successProcess(cc, "Destroy DID '" + didName + "' successfully");
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Destroy DID '" + didName + "'");
		}
	}

	//IDID
	// args[0]: String didName
	// args[1]: String keyPath
	// args[2]: String valueJson
	public void didSetValue(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String didName   = args.getString(idx++);
		String keyPath   = args.getString(idx++);
		String valueJson = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {

			IDid did = mDidManager.GetDID(didName);
			if (did == null) {
				errorProcess(cc, errCodeInvalidDID, "DID manager get DID '" + didName + "' fail");
				return;
			}

			did.SetValue(keyPath, valueJson);
			successProcess(cc, "DID set value successfully");
		} catch (WalletException e) {
			exceptionProcess(e, cc, "DID set value");
		}
	}

	// args[0]: String didName
	// args[1]: String keyPath
	public void didGetValue(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String didName   = args.getString(idx++);
		String keyPath   = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {
			IDid did = mDidManager.GetDID(didName);
			if (did == null) {
				errorProcess(cc, errCodeInvalidDID, "DID manager get DID '" + didName + "' fail");
				return;
			}

			successProcess(cc, did.GetValue(keyPath));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "DID get value of '" + keyPath + "'");
		}
	}

	// args[0]: String didName
	// args[1]: String keyPath
	public void didGetHistoryValue(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String didName   = args.getString(idx++);
		String keyPath   = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {
			IDid did = mDidManager.GetDID(didName);
			if (did == null) {
				errorProcess(cc, errCodeInvalidDID, "DID manager get DID '" + didName + "' fail");
				return;
			}

			successProcess(cc, did.GetHistoryValue(keyPath));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "DID get history value by '" + keyPath + "'");
		}
	}

	// args[0]: String didName
	// args[1]: int start
	// args[2]: int count
	public void didGetAllKeys(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String didName = args.getString(idx++);
		int    start   = args.getInt(idx++);
		int    count   = args.getInt(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {
			IDid did = mDidManager.GetDID(didName);
			if (did == null) {
				errorProcess(cc, errCodeInvalidDID, "DID manager get DID '" + didName + "' fail");
				return;
			}

			successProcess(cc, did.GetAllKeys(start, count));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "DID get " + count + " keys from " + start);
		}
	}

	// args[0]: String didName
	// args[1]: String message
	// args[2]: String password
	public void didSign(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String didName  = args.getString(idx++);
		String message  = args.getString(idx++);
		String password = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {
			IDid did = mDidManager.GetDID(didName);
			if (did == null) {
				errorProcess(cc, errCodeInvalidDID, "DID manager get DID '" + didName + "' fail");
				return;
			}

			successProcess(cc, did.Sign(message, password));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "DID sign");
		}
	}

	// args[0]: String didName
	// args[1]: String message
	// args[2]: String password
	public void didGenerateProgram(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String didName  = args.getString(idx++);
		String message  = args.getString(idx++);
		String password = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {
			IDid did = mDidManager.GetDID(didName);
			if (did == null) {
				errorProcess(cc, errCodeInvalidDID, "DID manager get DID '" + didName + "' fail");
				return;
			}

			successProcess(cc, did.GenerateProgram(message, password));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "DID generate program");
		}
	}

	// args[0]: String didName
	// args[1]: String message
	// args[2]: String signature
	public void didCheckSign(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String didName   = args.getString(idx++);
		String message   = args.getString(idx++);
		String signature = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {
			IDid did = mDidManager.GetDID(didName);
			if (did == null) {
				errorProcess(cc, errCodeInvalidDID, "DID manager get DID '" + didName + "' fail");
				return;
			}

			successProcess(cc, did.CheckSign(message, signature));
		} catch (WalletException e) {
			exceptionProcess(e, cc, "DID check sign");
		}
	}

	// args[0]: String didName
	public void didGetPublicKey(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String didName   = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {
			IDid did = mDidManager.GetDID(didName);
			if (did == null) {
				errorProcess(cc, errCodeInvalidDID, "DID manager get DID '" + didName + "' fail");
				return;
			}

			successProcess(cc, did.GetPublicKey());
		} catch (WalletException e) {
			exceptionProcess(e, cc, "DID get public key");
		}
	}

	// args[0]: String didName
	public void registerIdListener(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String didName   = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		if (mDidManager == null) {
			errorProcess(cc, errCodeInvalidDIDManager, "DID manager has not initialize");
			return;
		}

		try {
			mDidManager.RegisterCallback(didName, new IIdManagerCallback() {
				@Override
				public void OnIdStatusChanged(String id, String path, /*const nlohmann::json*/ String value) {
					try {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("id", id);
						jsonObject.put("path", path);
						jsonObject.put("value", value);

						PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
						pluginResult.setKeepCallback(true);
						cc.sendPluginResult(pluginResult);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			});
		} catch (WalletException e) {
			exceptionProcess(e, cc, "DID register listener");
		}
	}

	// SidechainSubWallet

	// args[0]: String masterWalletId
	// args[1]: String chainId
	// args[2]: String fromAddress
	// args[3]: String toAddress
	// args[4]: long amount
	// args[5]: String mainchainAccountsJson
	// args[6]: String mainchainAmountsJson
	// args[7]: String mainchainIndexsJson
	// args[8]: String memo
	// args[9]: String remark
	public void createWithdrawTransaction(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId        = args.getString(idx++);
		String chainId               = args.getString(idx++);
		String fromAddress           = args.getString(idx++);
		String toAddress             = args.getString(idx++);
		long   amount                = args.getLong(idx++);
		String mainchainAccountsJson = args.getString(idx++);
		String mainchainAmountsJson  = args.getString(idx++);
		String mainchainIndexsJson   = args.getString(idx++);
		String memo                  = args.getString(idx++);
		String remark                = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "Get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			if (! (subWallet instanceof ISidechainSubWallet)) {
				errorProcess(cc, errCodeSubWalletInstance, "Subwallet '" + chainId + "' is not instance of ISidechainSubWallet");
				return;
			}

			ISidechainSubWallet sidechainSubWallet = (ISidechainSubWallet)subWallet;
			String tx = sidechainSubWallet.CreateWithdrawTransaction(fromAddress, toAddress, amount,
					mainchainAccountsJson, mainchainAmountsJson, mainchainIndexsJson, memo, remark);

			successProcess(cc, tx);
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' subwallet '" + chainId + "' create withdraw tx");
		}
	}

	// args[0]: String masterWalletId
	// args[1]: String chainId
	public void getGenesisAddress(JSONArray args, CallbackContext cc) throws JSONException {
		int idx = 0;

		String masterWalletId = args.getString(idx++);
		String chainId        = args.getString(idx++);

		if (args.length() != idx) {
			errorProcess(cc, errCodeInvalidArg, idx + " parameters are expected");
			return;
		}

		try {
			ISubWallet subWallet = getSubWallet(masterWalletId, chainId);
			if (subWallet == null) {
				errorProcess(cc, errCodeInvalidSubWallet, "Get subwallet '" + chainId + "' of master wallet '" + masterWalletId + "' fail");
				return;
			}

			if (! (subWallet instanceof ISidechainSubWallet)) {
				errorProcess(cc, errCodeSubWalletInstance, "Subwallet '" + chainId + "' is not instance of ISidechainSubWallet");
				return;
			}

			ISidechainSubWallet sidechainSubWallet = (ISidechainSubWallet)subWallet;

			successProcess(cc, sidechainSubWallet.GetGenesisAddress());
		} catch (WalletException e) {
			exceptionProcess(e, cc, "Master wallet '" + masterWalletId + "' subwallet '" + chainId + "' get genesis address");
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}

