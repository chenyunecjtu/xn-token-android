package com.cdkj.token.wallet.private_wallet;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.cdkj.baselibrary.api.BaseResponseModel;
import com.cdkj.baselibrary.appmanager.CdRouteHelper;
import com.cdkj.baselibrary.appmanager.SPUtilHelper;
import com.cdkj.baselibrary.base.AbsLoadActivity;
import com.cdkj.baselibrary.dialog.TextPwdInputDialog;
import com.cdkj.baselibrary.dialog.UITipDialog;
import com.cdkj.baselibrary.nets.BaseResponseModelCallBack;
import com.cdkj.baselibrary.nets.RetrofitUtils;
import com.cdkj.baselibrary.utils.PermissionHelper;
import com.cdkj.baselibrary.utils.StringUtils;
import com.cdkj.token.R;
import com.cdkj.token.api.MyApi;
import com.cdkj.token.databinding.ActivityTransferBinding;
import com.cdkj.token.model.BtcFeesModel;
import com.cdkj.token.model.TransferSuccessEvent;
import com.cdkj.token.model.TxHashModel;
import com.cdkj.token.model.UTXOListModel;
import com.cdkj.token.model.UTXOModel;
import com.cdkj.token.model.WalletBalanceModel;
import com.cdkj.token.model.db.WalletDBModel;
import com.cdkj.token.utils.AmountUtil;
import com.cdkj.token.utils.EditTextJudgeNumberWatcher;
import com.cdkj.token.utils.wallet.WalletHelper;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import org.greenrobot.eventbus.EventBus;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

import static com.cdkj.token.utils.AmountUtil.ALLSCALE;
import static com.cdkj.token.utils.wallet.WalletHelper.getBtcFee;

/**
 * 钱包转账（BTC）
 * Created by cdkj on 2018/6/8.
 */

public class WalletBTCTransferActivity extends AbsLoadActivity {

    private ActivityTransferBinding mBinding;

    private final int CODEPERSE = 101;

    private Boolean isPastBtc;
    private WalletBalanceModel accountListBean;

    private PermissionHelper mPermissionHelper;

    private TextPwdInputDialog passWordInputDialog;

    private BigDecimal maxFees;//最大矿工费
    private BigDecimal mfees;//选择的矿工费
    private BigDecimal minfees;//最小矿工费

    private List<UTXOModel> unSpentBTCList;

    //需要的权限
    private String[] needLocationPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private BigDecimal transactionAmount = BigDecimal.ZERO;

    public static void open(Context context, WalletBalanceModel accountListBean, Boolean isPastBtc) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, WalletBTCTransferActivity.class);
        intent.putExtra(CdRouteHelper.DATASIGN, accountListBean);
        intent.putExtra(CdRouteHelper.DATASIGN2, isPastBtc);
        context.startActivity(intent);
    }

    private int SeekBarIndex = 50;

    @Override
    public View addMainView() {
        mBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.activity_transfer, null, false);
        return mBinding.getRoot();
    }

    @Override
    public void afterCreate(Bundle savedInstanceState) {

        setStatusBarBlue();
        setTitleBgBlue();

        isPastBtc = getIntent().getBooleanExtra(CdRouteHelper.DATASIGN2, false);
        accountListBean = getIntent().getParcelableExtra(CdRouteHelper.DATASIGN);

        mPermissionHelper = new PermissionHelper(this);
        if (accountListBean != null && !TextUtils.isEmpty(accountListBean.getCoinBalance())) {
            mBinding.tvCurrency.setText(AmountUtil.transformFormatToString(new BigDecimal(accountListBean.getCoinBalance()), accountListBean.getCoinSymbol(), ALLSCALE) + " " + accountListBean.getCoinSymbol());
            mBaseBinding.titleView.setMidTitle(accountListBean.getCoinSymbol());
        }


        mBaseBinding.titleView.setMidTitle(R.string.transfer);
        mBinding.edtAmount.addTextChangedListener(new EditTextJudgeNumberWatcher(mBinding.edtAmount, 15, 8));

        initListener();

        mBinding.btnNext.setOnClickListener(view -> {
            if (transferInputCheck()) return;
            showPasswordInputDialog();

        });

        getUtxoListRequest();

    }

    /**
     * 获取utxo列表然后进行签名
     */
    private void getUtxoListRequest() {

        Map<String, String> map = new HashMap<>();

        if (isPastBtc){
            String btcInfo = SPUtilHelper.getPastBtcInfo();

            if (TextUtils.isEmpty(btcInfo))
                return;

            map.put("address", btcInfo.split("\\+")[0]);
        }else {
            WalletDBModel userWalletIn = WalletHelper.getUserWalletInfoByUserId(WalletHelper.WALLET_USER);

            if (userWalletIn == null)
                return;

            map.put("address", userWalletIn.getBtcAddress());
        }



        showLoadingDialog();

        Call<BaseResponseModel<UTXOListModel>> call = RetrofitUtils.createApi(MyApi.class).getUtxoList("802220", StringUtils.getRequestJsonString(map));

        call.enqueue(new BaseResponseModelCallBack<UTXOListModel>(this) {
            @Override
            protected void onSuccess(UTXOListModel data, String SucMessage) {

                unSpentBTCList = data.getUtxoList();

            }

            @Override
            protected void onReqFailure(String errorCode, String errorMessage) {
                disMissLoadingDialog();
            }

            @Override
            protected void onFinish() {
                getFeesRequest();
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
        showLoadingDialog();
        Call<BaseResponseModel<TxHashModel>> call = RetrofitUtils.createApi(MyApi.class).btcTransactionBroadcast("802222", StringUtils.getRequestJsonString(map));

        call.enqueue(new BaseResponseModelCallBack<TxHashModel>(this) {
            @Override
            protected void onSuccess(TxHashModel data, String SucMessage) {

                EventBus.getDefault().post(new TransferSuccessEvent()); //通知上级界面进行数据刷新

                UITipDialog.showSuccess(WalletBTCTransferActivity.this, getString(R.string.transaction_success), new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        finish();
                    }
                });
            }

            @Override
            protected void onFinish() {
                disMissLoadingDialog();
            }
        });

    }

    /**
     * 获取手续费
     */
    public void getFeesRequest() {

        showLoadingDialog();

        Call<BaseResponseModel<BtcFeesModel>> call = RetrofitUtils.createApi(MyApi.class).getBtcFees("802223", StringUtils.getRequestJsonString(new HashMap<>()));

        call.enqueue(new BaseResponseModelCallBack<BtcFeesModel>(this) {
            @Override
            protected void onSuccess(BtcFeesModel data, String SucMessage) {
                maxFees = data.getFastestFeeMax();
                minfees = data.getFastestFeeMin();
                setFeesBySeekBarChange(SeekBarIndex);
            }

            @Override
            protected void onFinish() {
                disMissLoadingDialog();
            }
        });


    }


    /**
     * 转账输入状态检测
     *
     * @return
     */
    boolean transferInputCheck() {
        if (TextUtils.isEmpty(mBinding.editToAddress.getText().toString().trim())) {
            UITipDialog.showInfo(this, getString(R.string.please_to_address));
            return true;
        }

        if (!WalletHelper.verifyBTCAddress(mBinding.editToAddress.getText().toString().trim())) {
            UITipDialog.showInfo(this, getStrRes(R.string.error_wallet_address));
            return true;
        }

        if (isSameAddress()) {

            return true;
        }


        if (TextUtils.isEmpty(mBinding.edtAmount.getText().toString().trim())) {
            UITipDialog.showInfo(this, getString(R.string.please_input_transaction_number));
            return true;
        }

        try {

            if (accountListBean == null || TextUtils.isEmpty(accountListBean.getCoinBalance())) {
                UITipDialog.showInfo(this, getStrRes(R.string.no_balance));
                return true;
            }

            if (transactionAmount.compareTo(BigDecimal.ZERO) == 0 || transactionAmount.compareTo(BigDecimal.ZERO) == -1) {
                UITipDialog.showInfo(this, getString(R.string.please_correct_transaction_number));
                return true;
            }
            if (mfees == null) return true;

            if (getBtcFee(unSpentBTCList, transactionAmount.longValue(), mfees.intValue()) == -1) {
                UITipDialog.showInfo(this, getString(R.string.no_balance));
                return true;
            }

//            if (mfees == null) return true;
//
//            BigDecimal allBigInteger = mfees.add(transactionAmount);//手续费+转账数量
//
//            int checkInt = allBigInteger.compareTo(new BigDecimal(accountListBean.getCoinBalance())); //比较
//
//            if (checkInt == 1 || checkInt == 0) {
//                UITipDialog.showInfo(this, getString(R.string.no_balance));
//                return true;
//            }
        } catch (Exception e) {
            e.printStackTrace();
            UITipDialog.showInfo(this, getString(R.string.please_correct_transaction_number));
            return true;
        }
        return false;
    }

    /**
     * to 地址 和from地址是否相同 相同不允许转账
     *
     * @return
     */
    private boolean isSameAddress() {
        if (accountListBean == null) return false;



        String toAddress = mBinding.editToAddress.getText().toString();

        if (isPastBtc){
            String btcInfo = SPUtilHelper.getPastBtcInfo();
            return TextUtils.equals(toAddress, btcInfo.split("\\+")[0]);

        } else {
            WalletDBModel walletDBModel = WalletHelper.getUserWalletInfoByUserId(WalletHelper.WALLET_USER);
            //币种类型
            return TextUtils.equals(toAddress, walletDBModel.getBtcAddress());
        }

    }


    /**
     * 相机权限请求
     */
    private void permissionRequest() {
        mPermissionHelper.requestPermissions(new PermissionHelper.PermissionListener() {
            @Override
            public void doAfterGrand(String... permission) {
                Intent intent = new Intent(WalletBTCTransferActivity.this, CaptureActivity.class);
                startActivityForResult(intent, CODEPERSE);
            }

            @Override
            public void doAfterDenied(String... permission) {
                showToast(getStrRes(R.string.no_camera_permission));
            }
        }, needLocationPermissions);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mPermissionHelper != null) {
            mPermissionHelper.handleRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    /**
     * 显示密码输入框
     */
    private void showPasswordInputDialog() {

        passWordInputDialog = new TextPwdInputDialog(this).builder().setTitle(getStrRes(R.string.please_input_transaction_pwd))
                .setPositiveBtn(getStrRes(R.string.confirm), (view, inputMsg) -> {

                    String tradePwd = passWordInputDialog.getContentView().getText().toString().trim();

                    if (TextUtils.isEmpty(tradePwd)) {
                        UITipDialog.showInfoNoIcon(WalletBTCTransferActivity.this, getString(R.string.please_input_transaction_pwd));
                        return;
                    }

                    if (!WalletHelper.checkPasswordByUserId(tradePwd, WalletHelper.WALLET_USER)) {
                        UITipDialog.showInfoNoIcon(this, getString(R.string.transaction_password_error));
                        return;
                    }

                    try {

                        String address;
                        String privateKey;

                        if (isPastBtc){
                            String btcInfo = SPUtilHelper.getPastBtcInfo();
                            address = btcInfo.split("\\+")[0];
                            privateKey = btcInfo.split("\\+")[1];
                        }else {
                            WalletDBModel walletDBModel = WalletHelper.getUserWalletInfoByUserId(WalletHelper.WALLET_USER);
                            address = walletDBModel.getBtcAddress();
                            privateKey = walletDBModel.getBtcPrivateKey();
                        }



                        //获取btc交易签名
                        String sign = WalletHelper.signBTCTransactionData(unSpentBTCList,  //utxo列表
                                address,  //btc地址
                                mBinding.editToAddress.getText().toString().trim(),//btc转出地址
                                privateKey,//btc 私钥
                                transactionAmount.longValue()   //需要交易的金额
                                , getBtcFee(unSpentBTCList, transactionAmount.longValue(), mfees.intValue())); //矿工费

                        btcTransactionBroadcast(sign);

                    } catch (Exception e) {
                        e.printStackTrace();
                        disMissLoadingDialog();
                        UITipDialog.showFail(WalletBTCTransferActivity.this, getString(R.string.transfer_fail));
                    }

                })
                .setNegativeBtn(getStrRes(R.string.cancel), null)
                .setContentMsg("");
        passWordInputDialog.getContentView().setText("");
        passWordInputDialog.getContentView().setHint(getStrRes(R.string.please_input_transaction_pwd));
        passWordInputDialog.show();
    }


    /**
     * 设置矿工费显示
     */
    private void setShowFeesPrice(BigDecimal fees) {
        if (accountListBean == null || fees == null)
            return;

        // Btc矿工费
        long feeBtc = WalletHelper.getEstimateBtcFee(unSpentBTCList, transactionAmount.longValue(), fees.intValue());

        DecimalFormat df = new DecimalFormat("#######0.#");
        mBinding.tvGas.setText(df.format(fees) + " " + "sat/b ≈ " + AmountUtil.toMinWithUnit(new BigDecimal(feeBtc), accountListBean.getCoinSymbol(), AmountUtil.ALLSCALE));
    }

    private void initListener() {
        //扫码
        mBinding.fraLayoutQRcode.setOnClickListener(view -> {
            permissionRequest();
        });

        //矿工费滑动设置显示
        mBinding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                SeekBarIndex = i;
                setFeesBySeekBarChange(SeekBarIndex);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mBinding.edtAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (TextUtils.isEmpty(editable)){
                    //转账数量
                    transactionAmount = BigDecimal.ZERO;
                }else {
                    transactionAmount = AmountUtil.bigDecimalFormat(new BigDecimal(mBinding.edtAmount.getText().toString().trim()), WalletHelper.COIN_BTC);
                }

                setFeesBySeekBarChange(SeekBarIndex);
            }
        });

    }

    /**
     * 根据seekBar滑动计算矿工费
     *
     * @param i
     */
    public void setFeesBySeekBarChange(int i) {
        if (minfees == null || maxFees == null) return;

        float progress = i / 100f;

        BigDecimal progressBigDecimal = new BigDecimal(progress);

        BigDecimal lilmit = maxFees.subtract(minfees).multiply(progressBigDecimal);

        mfees = lilmit.add(minfees);

        setShowFeesPrice(mfees);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        /**
         * 处理二维码扫描结果
         */
        if (requestCode == CODEPERSE) {
            //处理扫描结果（在界面上显示）
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = bundle.getString(CodeUtils.RESULT_STRING);
                    if (WalletHelper.verifyBTCAddress(result)) {
                        mBinding.editToAddress.setText(result);
                    } else {
                        Toast.makeText(WalletBTCTransferActivity.this, R.string.error_wallet_address, Toast.LENGTH_LONG).show();
                    }
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(WalletBTCTransferActivity.this, R.string.resolve_wallet_address_fail, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
