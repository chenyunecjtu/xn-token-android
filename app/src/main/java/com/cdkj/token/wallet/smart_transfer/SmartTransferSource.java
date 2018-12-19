package com.cdkj.token.wallet.smart_transfer;

import android.content.Context;
import android.text.TextUtils;

import com.cdkj.baselibrary.api.BaseResponseModel;
import com.cdkj.baselibrary.appmanager.AppConfig;
import com.cdkj.baselibrary.appmanager.SPUtilHelper;
import com.cdkj.baselibrary.base.mvp.BaseMVPModel;
import com.cdkj.baselibrary.model.CodeModel;
import com.cdkj.baselibrary.nets.BaseResponseModelCallBack;
import com.cdkj.baselibrary.nets.RetrofitUtils;
import com.cdkj.baselibrary.utils.BigDecimalUtils;
import com.cdkj.baselibrary.utils.LogUtil;
import com.cdkj.baselibrary.utils.StringUtils;
import com.cdkj.token.api.MyApi;
import com.cdkj.token.model.BalanceListModel;
import com.cdkj.token.model.BtcFeesModel;
import com.cdkj.token.model.CoinModel;
import com.cdkj.token.model.CoinTypeAndAddress;
import com.cdkj.token.model.GasPrice;
import com.cdkj.token.model.TxHashModel;
import com.cdkj.token.model.UTXOListModel;
import com.cdkj.token.model.UTXOModel;
import com.cdkj.token.model.db.LocalCoinDbModel;
import com.cdkj.token.model.db.WalletDBModel;
import com.cdkj.token.utils.AmountUtil;
import com.cdkj.token.utils.LocalCoinDBUtils;
import com.cdkj.token.utils.wallet.WalletHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;

/**
 * Created by cdkj on 2018/9/10.
 */

public class SmartTransferSource extends BaseMVPModel {


    interface SmartTransferModelCallBack {

        void showDialog();

        void dismissDialog();

        void coinData(CoinModel coinModel);

        void balanceData(BigDecimal balance);

        void feesData(BigDecimal fee);

        void btcfeesData(BtcFeesModel btcFee);

        void btcUTXOData(List<UTXOModel> utxo);

        void usdtUTXOData(List<UTXOModel> utxo);

        void transferSuccess();

        void transferFail();

    }

    public SmartTransferModelCallBack smartTransferModelCallBack;

    public SmartTransferSource(SmartTransferModelCallBack smartTransferModelCallBack) {
        this.smartTransferModelCallBack = smartTransferModelCallBack;
    }


    /**
     * 获取币种
     */
    public void getAllCoins(Context context) {

        if (TextUtils.isEmpty(SPUtilHelper.getUserId())) {
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("currency", "");
        map.put("userId", SPUtilHelper.getUserId());
        map.put("token", SPUtilHelper.getUserToken());

        Call call = RetrofitUtils.createApi(MyApi.class).getAccount("802503", StringUtils.getRequestJsonString(map));

        addCall(call);

        smartTransferModelCallBack.showDialog();

        call.enqueue(new BaseResponseModelCallBack<CoinModel>(context) {
            @Override
            protected void onSuccess(CoinModel data, String SucMessage) {
                smartTransferModelCallBack.coinData(data);
            }

            @Override
            protected void onReqFailure(String errorCode, String errorMessage) {
                super.onReqFailure(errorCode, errorMessage);
                smartTransferModelCallBack.coinData(null);
            }

            @Override
            protected void onFinish() {
                smartTransferModelCallBack.dismissDialog();
            }
        });
    }

    /**
     * 获取手续费
     *
     * @param context
     */
    public void getFeeByCoin(Context context, String coinSymbol) {
        if (TextUtils.isEmpty(coinSymbol)) {
            return;
        }

        Map<String, String> map = new HashMap<>();

        map.put("symbol", coinSymbol);
        map.put("systemCode", AppConfig.SYSTEMCODE);
        map.put("companyCode", AppConfig.COMPANYCODE);

        Call call = RetrofitUtils.createApi(MyApi.class).getCoinFees("802266", StringUtils.getRequestJsonString(map));

        addCall(call);

        call.enqueue(new BaseResponseModelCallBack<LocalCoinDbModel>(context) {

            @Override
            protected void onSuccess(LocalCoinDbModel data, String SucMessage) {
                smartTransferModelCallBack.feesData(data.getWithdrawFee());
            }

            @Override
            protected void onReqFailure(String errorCode, String errorMessage) {
                super.onReqFailure(errorCode, errorMessage);
                smartTransferModelCallBack.feesData(BigDecimal.ZERO);
            }

            @Override
            protected void onFinish() {
            }
        });
    }


    /**
     * 获取币种余额
     */
    public void getPrivateCoinBalanceBySymbol(String coinSymbol, String coinAddress) {

        List<CoinTypeAndAddress> coinList = new ArrayList<>();

        CoinTypeAndAddress coinTypeAndAddress = new CoinTypeAndAddress();

        coinTypeAndAddress.setSymbol(coinSymbol);
        coinTypeAndAddress.setAddress(coinAddress);
        coinList.add(coinTypeAndAddress);

        Map<String, Object> map = new HashMap<>();
        map.put("accountList", coinList);

        Call<BaseResponseModel<BalanceListModel>> call = RetrofitUtils.createApi(MyApi.class).getBalanceList("802270", StringUtils.getRequestJsonString(map));

        addCall(call);

        smartTransferModelCallBack.showDialog();

        call.enqueue(new BaseResponseModelCallBack<BalanceListModel>(null) {
            @Override
            protected void onSuccess(BalanceListModel data, String SucMessage) {

                if (data == null || data.getAccountList() == null
                        || data.getAccountList().size() == 0 || data.getAccountList().get(0) == null) {
                    return;
                }

                smartTransferModelCallBack.balanceData(new BigDecimal(data.getAccountList().get(0).getBalance()));
            }

            @Override
            protected void onFinish() {
                smartTransferModelCallBack.dismissDialog();
            }
        });
    }


    /**
     * 根据币种获取私有钱包手续费
     *
     * @param coinSymbol
     */
    public void getPrivateFeeCoin(String coinSymbol) {

        if (LocalCoinDBUtils.isBTC(coinSymbol) || LocalCoinDBUtils.isUSDT(coinSymbol)) {
            getFeeForBtc();
            return;
        }

        if (canGetETHGasPrice(coinSymbol)) {
            getEthGasPriceValue();
        } else {
            getWanGasPriceValue();
        }

    }

    /**
     * 能否获取ETH网络Gas
     *
     * @return
     */
    private boolean canGetETHGasPrice(String coinSymbol) {

        if (TextUtils.equals(coinSymbol, WalletHelper.COIN_ETH)) {
            return true;
        }

        if (LocalCoinDBUtils.isEthTokenCoinByName(coinSymbol)) {
            return true;
        }

        return false;
    }


    /**
     * 划转交易
     */
    public void transfer(Context context, String password, String amount, String googleCode,
                         CoinModel.AccountListBean accountListBean) throws Exception {


        String toAddress = LocalCoinDBUtils.getAddressByCoin(accountListBean.getCurrency(), WalletHelper.WALLET_USER);

        Map<String, String> map = new HashMap<>();
        map.put("googleCaptcha", googleCode);
        map.put("token", SPUtilHelper.getUserToken());
        map.put("applyUser", SPUtilHelper.getUserId());
        map.put("systemCode", AppConfig.SYSTEMCODE);
        map.put("accountNumber", accountListBean.getAccountNumber());
        map.put("amount", AmountUtil.transformForRequest(amount, accountListBean.getCurrency()));
        map.put("payCardNo", toAddress);
        map.put("payCardInfo", accountListBean.getCurrency());
        map.put("applyNote", accountListBean.getCurrency());
        map.put("tradePwd", password);

        smartTransferModelCallBack.showDialog();

        Call call = RetrofitUtils.getBaseAPiService().codeRequest("802750", StringUtils.getRequestJsonString(map));

        addCall(call);

        call.enqueue(new BaseResponseModelCallBack<CodeModel>(context) {

            @Override
            protected void onSuccess(CodeModel data, String SucMessage) {
                if (!TextUtils.isEmpty(data.getCode())) {
                    smartTransferModelCallBack.transferSuccess();
                } else {
                    smartTransferModelCallBack.transferFail();
                }
            }

            @Override
            protected void onReqFailure(String errorCode, String errorMessage) {
                super.onReqFailure(errorCode, errorMessage);
                smartTransferModelCallBack.transferFail();
            }

            @Override
            protected void onFinish() {
                smartTransferModelCallBack.dismissDialog();
            }
        });
    }


    /**
     * 私钥钱包
     */
    public void transferPrivate(String coinSymbol, String toAddress, String amount, BigInteger transferGasPrice) {


        smartTransferModelCallBack.showDialog();
        mSubscription.add(Observable.just("")
                .subscribeOn(Schedulers.newThread())
                .map(s -> transferByCoin(coinSymbol, toAddress, amount, transferGasPrice))
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> smartTransferModelCallBack.dismissDialog())
                .subscribe(s -> {

                    if (TextUtils.isEmpty(s)) {
                        smartTransferModelCallBack.transferFail();
                        return;
                    }

                    LogUtil.E("交易hash" + s);
                    smartTransferModelCallBack.transferSuccess();

                }, throwable -> {
                    smartTransferModelCallBack.transferFail();
                    LogUtil.BIGLOG("has————" + throwable);
                }));
    }

    /**
     * 根据币种类型进行转账操作
     *
     * @return
     * @throws Exception
     */
    private String transferByCoin(String coinSymbol, String toAddress, String amount, BigInteger transferGasPrice) throws Exception {

        WalletDBModel w = WalletHelper.getUserWalletInfoByUserId(WalletHelper.WALLET_USER);

        if (TextUtils.equals(coinSymbol, WalletHelper.COIN_WAN)) {
            return WalletHelper.transferForWan(w, toAddress, amount, WalletHelper.getDeflutGasLimit(), transferGasPrice);
        }

        if (TextUtils.equals(coinSymbol, WalletHelper.COIN_ETH)) {
            return WalletHelper.transferForEth(w, toAddress, amount, WalletHelper.getDeflutGasLimit(), transferGasPrice);
        }

        //币种类型
        String coinType = LocalCoinDBUtils.getLocalCoinType(coinSymbol);
        //合约地址
        String contractAddress = LocalCoinDBUtils.getLocalCoinContractAddress(coinSymbol);

        if (LocalCoinDBUtils.isEthTokenCoin(coinType)) {
            return WalletHelper.transferForEthTokenCoin(w, toAddress,WalletHelper.getUnitAmountValue(amount, coinSymbol), contractAddress, transferGasPrice);
        }

        if (LocalCoinDBUtils.isWanTokenCoin(coinType)) {
            return WalletHelper.transferForWanTokenCoin(w, toAddress, WalletHelper.getUnitAmountValue(amount, coinSymbol), contractAddress, transferGasPrice);
        }

        return "";
    }


    /**
     * 获取燃料费用
     */
    public void getEthGasPriceValue() {
        Call<BaseResponseModel<GasPrice>> call = RetrofitUtils.createApi(MyApi.class).getGasPrice("802117", "{}");  //ETH gas

        addCall(call);

        call.enqueue(new BaseResponseModelCallBack<GasPrice>(null) {
            @Override
            protected void onSuccess(GasPrice gasPrice, String SucMessage) {
                smartTransferModelCallBack.feesData(BigDecimalUtils.tr(gasPrice.getGasPrice()));
            }

            @Override
            protected void onReqFailure(String errorCode, String errorMessage) {
                super.onReqFailure(errorCode, errorMessage);
                smartTransferModelCallBack.feesData(BigDecimal.ZERO);
            }

            @Override
            protected void onFinish() {
            }
        });
    }

    /**
     * 获取燃料费用
     */
    public void getWanGasPriceValue() {
        Call<BaseResponseModel<GasPrice>> call = RetrofitUtils.createApi(MyApi.class).getGasPrice("802358", "{}");

        addCall(call);

        call.enqueue(new BaseResponseModelCallBack<GasPrice>(null) {
            @Override
            protected void onSuccess(GasPrice gasPrice, String SucMessage) {
                smartTransferModelCallBack.feesData(BigDecimalUtils.tr(gasPrice.getGasPrice()));
            }

            @Override
            protected void onReqFailure(String errorCode, String errorMessage) {
                super.onReqFailure(errorCode, errorMessage);
                smartTransferModelCallBack.feesData(BigDecimal.ZERO);
            }

            @Override
            protected void onFinish() {
            }
        });
    }


    /**
     * 获取手续费
     */
    public void getFeeForBtc() {

        Call<BaseResponseModel<BtcFeesModel>> call = RetrofitUtils.createApi(MyApi.class).getBtcFees("802223", StringUtils.getRequestJsonString(new HashMap<>()));

        call.enqueue(new BaseResponseModelCallBack<BtcFeesModel>(null) {
            @Override
            protected void onSuccess(BtcFeesModel data, String SucMessage) {
                smartTransferModelCallBack.btcfeesData(data);
            }

            @Override
            protected void onReqFailure(String errorCode, String errorMessage) {
                super.onReqFailure(errorCode, errorMessage);
                smartTransferModelCallBack.btcfeesData(new BtcFeesModel());
            }

            @Override
            protected void onFinish() {

            }
        });

    }

    /**
     * 获取utxo列表然后进行签名
     */
    public void getBTCUTXO(String btcAddress) {

        Map<String, String> map = new HashMap<>();

        map.put("address", btcAddress);

        Call<BaseResponseModel<UTXOListModel>> call = RetrofitUtils.createApi(MyApi.class).getUtxoList("802220", StringUtils.getRequestJsonString(map));

        smartTransferModelCallBack.showDialog();

        call.enqueue(new BaseResponseModelCallBack<UTXOListModel>(null) {
            @Override
            protected void onSuccess(UTXOListModel data, String SucMessage) {
                smartTransferModelCallBack.btcUTXOData(data.getUtxoList());
            }

            @Override
            protected void onReqFailure(String errorCode, String errorMessage) {

            }

            @Override
            protected void onFinish() {
                smartTransferModelCallBack.dismissDialog();
            }
        });

    }

    /**
     * btc交易签名广播
     *
     * @param txSign
     */
    public void btcTransactionBroadcast(String txSign) {

        Map<String, String> map = new HashMap<>();
        map.put("signTx", txSign);

        smartTransferModelCallBack.showDialog();

        Call<BaseResponseModel<TxHashModel>> call = RetrofitUtils.createApi(MyApi.class).btcTransactionBroadcast("802222", StringUtils.getRequestJsonString(map));

        call.enqueue(new BaseResponseModelCallBack<TxHashModel>(null) {
            @Override
            protected void onSuccess(TxHashModel data, String SucMessage) {
                smartTransferModelCallBack.transferSuccess();
            }

            @Override
            protected void onFinish() {
                smartTransferModelCallBack.dismissDialog();
            }
        });

    }


    /**
     * 获取utxo列表然后进行签名
     */
    public void getUSDTUTXO(String btcAddress) {

        Map<String, String> map = new HashMap<>();

        map.put("address", btcAddress);

        Call<BaseResponseModel<UTXOListModel>> call = RetrofitUtils.createApi(MyApi.class).getUtxoList("802220", StringUtils.getRequestJsonString(map));

        smartTransferModelCallBack.showDialog();

        call.enqueue(new BaseResponseModelCallBack<UTXOListModel>(null) {
            @Override
            protected void onSuccess(UTXOListModel data, String SucMessage) {
                smartTransferModelCallBack.usdtUTXOData(data.getUtxoList());
            }

            @Override
            protected void onReqFailure(String errorCode, String errorMessage) {

            }

            @Override
            protected void onFinish() {
                smartTransferModelCallBack.dismissDialog();
            }
        });

    }

}
