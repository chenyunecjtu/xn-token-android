package com.cdkj.token.wallet;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cdkj.baselibrary.api.BaseResponseModel;
import com.cdkj.baselibrary.appmanager.AppConfig;
import com.cdkj.baselibrary.appmanager.CdRouteHelper;
import com.cdkj.baselibrary.appmanager.SPUtilHelper;
import com.cdkj.baselibrary.base.BaseLazyFragment;
import com.cdkj.baselibrary.interfaces.BaseRefreshCallBack;
import com.cdkj.baselibrary.model.AllFinishEvent;
import com.cdkj.baselibrary.nets.BaseResponseModelCallBack;
import com.cdkj.baselibrary.nets.RetrofitUtils;
import com.cdkj.baselibrary.utils.BigDecimalUtils;
import com.cdkj.baselibrary.utils.RefreshHelper;
import com.cdkj.baselibrary.utils.StringUtils;
import com.cdkj.token.R;
import com.cdkj.token.adapter.WalletBalanceAdapter;
import com.cdkj.token.api.MyApi;
import com.cdkj.token.databinding.FragmentWallet2Binding;
import com.cdkj.token.find.FutureImageShowActivity;
import com.cdkj.token.find.MsgListActivity;
import com.cdkj.token.find.NoneActivity;
import com.cdkj.token.interfaces.LocalCoinCacheInterface;
import com.cdkj.token.interfaces.LocalCoinCachePresenter;
import com.cdkj.token.model.AddCoinChangeEvent;
import com.cdkj.token.model.BalanceListModel;
import com.cdkj.token.model.CoinModel;
import com.cdkj.token.model.CoinTypeAndAddress;
import com.cdkj.token.model.MsgListModel;
import com.cdkj.token.model.WalletBalanceModel;
import com.cdkj.token.model.db.LocalCoinDbModel;
import com.cdkj.token.model.db.WalletDBModel;
import com.cdkj.token.utils.LocalCoinDBUtils;
import com.cdkj.token.utils.wallet.WalletHelper;
import com.cdkj.token.views.CardChangeLayout;
import com.cdkj.token.views.dialogs.InfoSureDialog;
import com.cdkj.token.wallet.account_wallet.BillListActivity;
import com.cdkj.token.wallet.create_guide.CreateWalletStartActivity;
import com.cdkj.token.wallet.import_guide.ImportWalletStartActivity;
import com.cdkj.token.wallet.private_wallet.WalletCoinDetailsActivity;
import com.cdkj.token.wallet.smart_transfer.SmartTransferActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.Disposable;
import retrofit2.Call;

import static com.cdkj.token.utils.LocalCoinDBUtils.getCoinIconByCoinSymbol;
import static com.cdkj.token.views.CardChangeLayout.BOTTOMVIEW;
import static com.cdkj.token.views.CardChangeLayout.TOPVIEW;

/**
 * 钱包资产
 * Created by cdkj on 2018/6/28.
 */
//TODO 代码分离优化  请求嵌套优化
public class WalletFragment extends BaseLazyFragment {

    public static final String HIND_SIGN = "****"; // 隐藏金额

    private FragmentWallet2Binding mBinding;

    private RefreshHelper mRefreshHelper;

    private List<CoinTypeAndAddress> mChooseCoinList; // 用户自选的币种

    private boolean isPrivateWallet = false; // 当前是否是私密钱包 默认我的钱包

    private LocalCoinCachePresenter mlLocalCoinCachePresenter;

    private View mImportGuideView; // 导入钱包引导
    private CoinModel mWalletData;
    private BalanceListModel mPrivateWalletData;


    public static WalletFragment getInstance() {
        WalletFragment fragment = new WalletFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_wallet_2, null, false);

        initViewState();

        initRefresh();

        initCardChangeListener();

        initClickListener();

        getMsgRequest();

        initLocalCoinPresenter();

        initAssetsByEyeState(SPUtilHelper.isAssetsShow());

        mlLocalCoinCachePresenter.getCoinList(mActivity);  //开始时请求币种缓存

        return mBinding.getRoot();
    }

    /**
     * 本地币种缓存Presenter回调
     * 获取缓存后再获取对应钱包数据
     */
    void initLocalCoinPresenter() {
        mlLocalCoinCachePresenter = new LocalCoinCachePresenter(new LocalCoinCacheInterface() {
            @Override
            public void cacheEnd(List<LocalCoinDbModel> data) {
                if (isPrivateWallet) {
                    mChooseCoinList = null;
                    getPriWalletAssetsData(true, false);
                } else {
                    getWalletAssetsData(true, false);
                }
            }

        });
    }

    /**
     * 初始化View 状态 本地货币类型
     */
    void initViewState() {
        mBinding.tvAllAssets.setText(getString(R.string.wallet_assets, SPUtilHelper.getLocalMarketSymbol()));
        mBinding.cardChangeLayout.tvMyWallet.setText(getString(R.string.my_wallet, SPUtilHelper.getLocalMarketSymbol()));
        mBinding.cardChangeLayout.tvMyPrivateWallet.setText(getString(R.string.my_private_wallet, SPUtilHelper.getLocalMarketSymbol()));

        mBinding.cardChangeLayout.tvWalletSymbol.setText(AppConfig.getSymbolByType(SPUtilHelper.getLocalMarketSymbol()));
        mBinding.cardChangeLayout.tvPrivateWalletSymbol.setText(AppConfig.getSymbolByType(SPUtilHelper.getLocalMarketSymbol()));
    }


    /**
     * 点击事件
     */
    private void initClickListener() {

        //资产显示隐藏
        mBinding.fralayoutAssetsShow.setOnClickListener(view -> {

            boolean isShow = !SPUtilHelper.isAssetsShow();
            toggleAssetsByEyeState(isShow);
            SPUtilHelper.saveIsAssetsShow(isShow);

        });

        //公告信息
        mBinding.tvBulletin.setOnClickListener(view -> {
            MsgListActivity.open(mActivity);
        });


        //闪兑
        mBinding.linLayoutTransferChange.setOnClickListener(view -> {
            FutureImageShowActivity.open(mActivity, NoneActivity.FLASH);
        });

        //公告关闭
        mBinding.imgBulletinClose.setOnClickListener(view -> {
            mBinding.linLayoutBulletin.setVisibility(View.GONE);
        });

        //添加自选
        mBinding.imgAddCoin.setOnClickListener(view -> {
            if (isPrivateWallet){
                AddPriChoiceCoinActivity.open(mActivity);
            } else {
                AddChoiceCoinActivity.open(mActivity);
            }

        });

        //我的钱包说明
        mBinding.cardChangeLayout.imgMyWalletInfo.setOnClickListener(view -> {
            new InfoSureDialog(mActivity).setInfoTitle(getString(R.string.my_account_wallet)).setInfoContent(getString(R.string.my_wallet_introduction)).show();
        });

        //私钥钱包说明
        mBinding.cardChangeLayout.imgMyPrivateWalletInfo.setOnClickListener(view -> {
            new InfoSureDialog(mActivity).setInfoTitle(getString(R.string.my_private_wallet_title)).setInfoContent(getString(R.string.my_private_wallet_introduction)).show();
        });

        //一键划转
        mBinding.linLayoutSmartTransfer.setOnClickListener(view -> {
            boolean isHasInfo = WalletHelper.isUserAddedWallet(SPUtilHelper.getUserId());
            if (!isHasInfo) {
                CreateWalletStartActivity.open(mActivity);
                return;
            }
            SmartTransferActivity.open(mActivity, isPrivateWallet);
        });

    }

    /**
     * 是否显示资产
     *
     * @param isShow
     */
    private void toggleAssetsByEyeState(boolean isShow) {
        if (isShow) {
            mBinding.tvAssetsShow.setImageResource(R.drawable.eye_open);
            setWalletAssetsText(mWalletData);
            shePrivateWalletAssectText(mPrivateWalletData);
            countAllWalletAmount();
        } else {
            mBinding.tvAllWalletAmount.setText(AppConfig.getSymbolByType(SPUtilHelper.getLocalMarketSymbol()) + HIND_SIGN);
            mBinding.cardChangeLayout.tvWalletAmount.setText(HIND_SIGN);
            mBinding.cardChangeLayout.tvPriWalletAmount.setText(HIND_SIGN);
            mBinding.tvAssetsShow.setImageResource(R.drawable.eye_close);
        }
    }

    /**
     * 是否显示资产
     *
     * @param isShow
     */
    private void initAssetsByEyeState(boolean isShow) {
        if (isShow) {
            mBinding.tvAssetsShow.setImageResource(R.drawable.eye_open);
            mBinding.cardChangeLayout.tvWalletAmount.setText("0.00");
            mBinding.cardChangeLayout.tvPriWalletAmount.setText("0.00");
            mBinding.tvAllWalletAmount.setText("0.00");
        } else {
            mBinding.tvAllWalletAmount.setText(AppConfig.getSymbolByType(SPUtilHelper.getLocalMarketSymbol()) + HIND_SIGN);
            mBinding.cardChangeLayout.tvWalletAmount.setText(HIND_SIGN);
            mBinding.cardChangeLayout.tvPriWalletAmount.setText(HIND_SIGN);
            mBinding.tvAssetsShow.setImageResource(R.drawable.eye_close);
        }
    }

    /**
     * 顶部卡片布局改变监听
     */
    void initCardChangeListener() {
        mBinding.cardChangeLayout.cardChangeLayout.setChangeCallBack(new CardChangeLayout.ChangeCallBack() {
            @Override
            public boolean onChangeStart(int index) {
                if (!SPUtilHelper.isLoginNoStart()) {   //如果用户没登录则跳转的登录界面
                    EventBus.getDefault().post(new AllFinishEvent());
                    CdRouteHelper.openLogin(true);
                    if (mActivity != null) {
                        mActivity.finish();
                    }
                    return false;
                }

                if (mBinding.refreshLayout.isRefreshing() || mBinding.refreshLayout.isLoading()) {
                    return false;
                }

                return true;
            }

            @Override
            public void onChangeEnd(int index) {
                changeLayoutByIndex(index);
            }
        });
    }

    /**
     * 改变布局
     *
     * @param index 在Layout中的索引
     */
    private void changeLayoutByIndex(int index) {

        switch (index) {
            case BOTTOMVIEW:                                         //私密钱包
                isPrivateWallet = true;
                boolean isHasWallet = showImportGuideViewAndGetState();
                if (!isHasWallet) {
                    return;
                }

                mRefreshHelper.setPageIndex(1);
                mRefreshHelper.setData(transformToPrivateAdapterData(mPrivateWalletData), getString(R.string.no_assets), R.mipmap.order_none);
                break;

            case TOPVIEW:                                         //个人钱包
                isPrivateWallet = false;
                hindImportGuideView();
                mRefreshHelper.setData(transformToAdapterData(mWalletData), getString(R.string.no_assets), R.mipmap.order_none);
                break;
        }

        showWalletStateView();
    }

    /**
     * 根据是否有钱包来判段是否显示导入引导View   //用户没有导入钱包 则显示引导界面,同时隐藏列表界面和功能界面
     */
    private boolean showImportGuideViewAndGetState() {

        boolean isHasWallet = WalletHelper.isUserAddedWallet(SPUtilHelper.getUserId());

        if (isHasWallet) {
            hindImportGuideView();
            return isHasWallet;
        }

        inFlateImportViewAndSetListener();

        showImportGuideView();

        return isHasWallet;
    }

    /**
     * 导入ImportView 并设置点击事件
     */
    private void inFlateImportViewAndSetListener() {
        if (!mBinding.importLayout.isInflated()) {
            mImportGuideView = mBinding.importLayout.getViewStub().inflate();

            mImportGuideView.findViewById(R.id.btn_create).setOnClickListener(view -> {
                CreateWalletStartActivity.open(mActivity);
            });

            mImportGuideView.findViewById(R.id.tv_import).setOnClickListener(view -> {
                ImportWalletStartActivity.open(mActivity);
            });
        }
    }

    /**
     * 显示引导View
     */
    private void showImportGuideView() {
        if (mImportGuideView != null) {
            mImportGuideView.setVisibility(View.VISIBLE);
        }
        mBinding.linLayoutTransfer.setVisibility(View.GONE);
        mBinding.linLayoutCoinlistTitle.setVisibility(View.GONE);
        mBinding.linLayoutRecycler.setVisibility(View.GONE);
    }

    /**
     * 隐藏引导View
     */
    private void hindImportGuideView() {
        if (mImportGuideView != null && mImportGuideView.getVisibility() == View.VISIBLE) {
            mImportGuideView.setVisibility(View.GONE);
            mBinding.linLayoutTransfer.setVisibility(View.VISIBLE);
            mBinding.linLayoutCoinlistTitle.setVisibility(View.VISIBLE);
            mBinding.linLayoutRecycler.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 根据用户选择的钱包显示相应界面
     */
    private void showWalletStateView() {

        if (isPrivateWallet) {
            mBinding.imgAddCoin.setVisibility(View.VISIBLE);
            mBinding.imgChange.setImageResource(R.drawable.change_red);
            mBinding.imgTransfer.setImageResource(R.drawable.transfer_red);
        } else {
            mBinding.imgAddCoin.setVisibility(View.VISIBLE);
            mBinding.imgChange.setImageResource(R.drawable.change_blue);
            mBinding.imgTransfer.setImageResource(R.drawable.transfer_blue);
        }

    }

    private void initRefresh() {
        mRefreshHelper = new RefreshHelper(mActivity, new BaseRefreshCallBack(mActivity) {
            @Override
            public View getRefreshLayout() {
                mBinding.refreshLayout.setEnableLoadmore(false);
                return mBinding.refreshLayout;
            }

            @Override
            public RecyclerView getRecyclerView() {
                return mBinding.rvWallet;
            }

            @Override
            public RecyclerView.Adapter getAdapter(List listData) {

                WalletBalanceAdapter walletBalanceAdapter = new WalletBalanceAdapter(listData);

                walletBalanceAdapter.setOnItemClickListener((adapter, view, position) -> {

                    if (mBinding.cardChangeLayout.cardChangeLayout.isChanging()) { //动画中禁止点击
                        return;
                    }

                    if (isPrivateWallet) {

                        WalletBalanceModel localCoinModel = walletBalanceAdapter.getItem(position);

                        String address = SPUtilHelper.getPastBtcInfo().split("\\+")[0];

                        WalletCoinDetailsActivity.open(mActivity, localCoinModel,TextUtils.equals(address, localCoinModel.getAddress()));
                    } else {
                        BillListActivity.open(mActivity, walletBalanceAdapter.getItem(position));
                    }

                });

                return walletBalanceAdapter;
            }

            @Override
            public void onRefresh(int pageindex, int limit) {
                mlLocalCoinCachePresenter.getCoinList(mActivity);
            }

            @Override
            public void getListDataRequest(int pageindex, int limit, boolean isShowDialog) {

            }
        });

        mBinding.rvWallet.setLayoutManager(getRecyclerViewLayoutManager());

        mBinding.rvWallet.setNestedScrollingEnabled(false);


        mRefreshHelper.init(10);
    }


    @Override
    protected void lazyLoad() {
        if (mBinding == null) {
            return;
        }
        getMsgRequest();
    }

    @Override
    protected void onInvisible() {

    }

    /**
     * 获取钱包资产数据
     *
     * @param isRequstPrivateWallet 是否同时请求私钥钱包数据
     */
    private void getWalletAssetsData(boolean isRequstPrivateWallet, boolean isShowDialog) {

        if (TextUtils.isEmpty(SPUtilHelper.getUserToken()))
            return;

        Map<String, Object> map = new HashMap<>();
        map.put("currency", "");
        map.put("userId", SPUtilHelper.getUserId());
        map.put("token", SPUtilHelper.getUserToken());

        Call call = RetrofitUtils.createApi(MyApi.class).getAccount("802504", StringUtils.getRequestJsonString(map));

        addCall(call);

        if (isShowDialog) {
            showLoadingDialog();
        }

        call.enqueue(new BaseResponseModelCallBack<CoinModel>(mActivity) {
            @Override
            protected void onSuccess(CoinModel data, String SucMessage) {

                mWalletData = data;
                toggleAssetsByEyeState(SPUtilHelper.isAssetsShow());
                mRefreshHelper.setPageIndex(1);
                mRefreshHelper.setData(transformToAdapterData(data), getString(R.string.no_assets), R.mipmap.order_none);
                if (isRequstPrivateWallet && WalletHelper.isUserAddedWallet(SPUtilHelper.getUserId())) {  //没有添加钱包不用请求私钥钱包数据
                    getPriWalletAssetsData(false, false);
                }
            }

            @Override
            protected void onReqFailure(String errorCode, String errorMessage) {
                super.onReqFailure(errorCode, errorMessage);
                mRefreshHelper.loadError(errorMessage, 0);
                disMissLoading();
            }

            @Override
            protected void onFinish() {
                disMissLoading();
            }
        });
    }

    private void setWalletAssetsText(CoinModel data) {
        if (data == null) {
            mBinding.cardChangeLayout.tvWalletAmount.setText("0.00");
            return;
        }

        mBinding.cardChangeLayout.tvWalletAmount.setText(data.getAmountStringByLocalMarket());

    }


    /**
     * 获取私有钱包数据
     *
     * @param isSetRecyclerData 是否设置recyclerData
     */

    private void getPriWalletAssetsData(boolean isSetRecyclerData, boolean isShowDialog) {

        if (mChooseCoinList == null) {
            getLocalCoinAndRequestWalletDdata(isSetRecyclerData, isShowDialog);
            return;
        }

        if (!WalletHelper.isUserAddedWallet(SPUtilHelper.getUserId())) {
            disMissLoading();
            mRefreshHelper.refreshLayoutStop();
            return;
        }

        Boolean isHasBtc = false;
        for (CoinTypeAndAddress coin : mChooseCoinList){
            if (TextUtils.equals(coin.getSymbol(),WalletHelper.COIN_BTC)){// 跟随新BTC地址自选，如果新BTC地址没有自选，老的也不出现
                isHasBtc = true;
            }
        }
        if (isHasBtc){// 每次刷新时获取老的BTC地址和私钥
            WalletHelper.getPastBtcAddress();
            String adress = SPUtilHelper.getPastBtcInfo();
            CoinTypeAndAddress pastBtcAddress = new CoinTypeAndAddress();
            pastBtcAddress.setSymbol(WalletHelper.COIN_BTC);
            pastBtcAddress.setAddress(adress.split("\\+")[0]);
            mChooseCoinList.add(pastBtcAddress);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("accountList", mChooseCoinList);

        Call<BaseResponseModel<BalanceListModel>> call = RetrofitUtils.createApi(MyApi.class).getBalanceList("802270", StringUtils.getRequestJsonString(map));

        addCall(call);

        if (isShowDialog) {
            showLoadingDialog();
        }

        call.enqueue(new BaseResponseModelCallBack<BalanceListModel>(mActivity) {
            @Override
            protected void onSuccess(BalanceListModel data, String SucMessage) {

                mPrivateWalletData = data;

                toggleAssetsByEyeState(SPUtilHelper.isAssetsShow());

                if (isSetRecyclerData) {
                    List<WalletBalanceModel> walletBalanceModels = transformToPrivateAdapterData(mPrivateWalletData);
                    mRefreshHelper.setPageIndex(1);
                    mRefreshHelper.setData(walletBalanceModels, getString(R.string.no_assets), R.mipmap.order_none);
                }
            }

            @Override
            protected void onReqFailure(String errorCode, String errorMessage) {
                super.onReqFailure(errorCode, errorMessage);
                if (isSetRecyclerData) {
                    mRefreshHelper.loadError(errorMessage, 0);
                }
            }

            @Override
            protected void onFinish() {
                disMissLoading();
            }
        });
    }

    private void shePrivateWalletAssectText(BalanceListModel data) {
        if (data == null) {
            mBinding.cardChangeLayout.tvPriWalletAmount.setText("0.00");
            return;
        }

        mBinding.cardChangeLayout.tvPriWalletAmount.setText(data.getAmountStringByLocalMarket());

    }

    /**
     * 异步获取本地货币并请求钱包数据
     *
     * @param isSetRecyclerData 用户调用 getPriWalletAssetsData方法
     * @param isShowDialog
     */
    public void getLocalCoinAndRequestWalletDdata(boolean isSetRecyclerData, boolean isShowDialog) {
        Disposable disposable = WalletHelper.getLocalCoinListAsync(localCoinDbModels -> {
            if (localCoinDbModels != null) {
                mChooseCoinList = getChooseCoinList(localCoinDbModels);
                getPriWalletAssetsData(isSetRecyclerData, isShowDialog);
            }
        });
        mSubscription.add(disposable); //用于结束异步
    }

    /**
     * 获取用户选择的币种
     */
    private List<CoinTypeAndAddress> getChooseCoinList(List<LocalCoinDbModel> localCoinDbModels) {

        List<CoinTypeAndAddress> chooseCoinList = new ArrayList<>();

        String chooseSymbol = WalletHelper.getUserChooseCoinSymbolString(SPUtilHelper.getUserId()); //获取用户选择币种

        WalletDBModel walletDBModel = WalletHelper.getUserWalletInfoByUsreId(SPUtilHelper.getUserId());//获取钱包信息

        boolean isFirstChoose = WalletHelper.userIsCoinChoosed(SPUtilHelper.getUserId());


        for (LocalCoinDbModel localCoinDbModel : localCoinDbModels) {           //获取本地缓存的币种

            if (localCoinDbModel == null) {
                continue;
            }
            //如果用户没有添加过自选 则不进行自选币种判断
            if (isFirstChoose && TextUtils.indexOf(chooseSymbol, localCoinDbModel.getSymbol()) == -1) {
                continue;
            }

            CoinTypeAndAddress coinTypeAndAddress = new CoinTypeAndAddress();    //0 公链币（ETH BTC WAN） 1 ethtoken（ETH） 2 wantoken（WAN）        通过币种和type 添加地址

            if (LocalCoinDBUtils.isCommonChainCoinByType(localCoinDbModel.getType())) {

                if (TextUtils.equals(WalletHelper.COIN_BTC, localCoinDbModel.getSymbol())) {
                    coinTypeAndAddress.setAddress(walletDBModel.getBtcAddress());
                } else if (TextUtils.equals(WalletHelper.COIN_ETH, localCoinDbModel.getSymbol())) {
                    coinTypeAndAddress.setAddress(walletDBModel.getEthAddress());
                } else if (TextUtils.equals(WalletHelper.COIN_WAN, localCoinDbModel.getSymbol())) {
                    coinTypeAndAddress.setAddress(walletDBModel.getWanAddress());
                } else if (TextUtils.equals(WalletHelper.COIN_USDT, localCoinDbModel.getSymbol())) {
                    coinTypeAndAddress.setAddress(walletDBModel.getBtcAddress());
                }

            } else if (LocalCoinDBUtils.isEthTokenCoin(localCoinDbModel.getType())) {

                coinTypeAndAddress.setAddress(walletDBModel.getEthAddress());

            } else if (LocalCoinDBUtils.isWanTokenCoin(localCoinDbModel.getType())) {

                coinTypeAndAddress.setAddress(walletDBModel.getWanAddress());

            }
            coinTypeAndAddress.setSymbol(localCoinDbModel.getSymbol());

            if (!TextUtils.isEmpty(coinTypeAndAddress.getAddress())) {
                chooseCoinList.add(coinTypeAndAddress);
            }

        }

        return chooseCoinList;
    }

    /**
     * 计算所有钱包资产(个人 + 私有)
     */
    private void countAllWalletAmount() {

        BigDecimal wallAmount = BigDecimal.ZERO;

        BigDecimal priWallAmount = BigDecimal.ZERO;

        if (mWalletData != null) {
            wallAmount = new BigDecimal(mWalletData.getAmountStringByLocalMarket());
        }
        if (mPrivateWalletData != null) {
            priWallAmount = new BigDecimal(mPrivateWalletData.getAmountStringByLocalMarket());
        }

        mBinding.tvAllWalletAmount.setText(AppConfig.getSymbolByType(SPUtilHelper.getLocalMarketSymbol()) + BigDecimalUtils.add(wallAmount, priWallAmount).toPlainString());

    }

    /**
     * 转换为adapter数据 （我的钱包） getUnit
     *
     * @param data
     * @return
     */
    private List<WalletBalanceModel> transformToAdapterData(CoinModel data) {
        List<WalletBalanceModel> walletBalanceModels = new ArrayList<>();

        if (data == null) {
            return walletBalanceModels;
        }

        for (CoinModel.AccountListBean accountListBean : data.getAccountList()) {

            WalletBalanceModel walletBalanceModel = new WalletBalanceModel();

            walletBalanceModel.setCoinSymbol(accountListBean.getCurrency());

            walletBalanceModel.setAmount(accountListBean.getAmount());

            walletBalanceModel.setFrozenAmount(accountListBean.getFrozenAmount());

            if (accountListBean.getAmount() != null && accountListBean.getFrozenAmount() != null) {

                BigDecimal amount = accountListBean.getAmount();

                BigDecimal frozenAmount = accountListBean.getFrozenAmount();
                //可用=总资产-冻结
                walletBalanceModel.setAvailableAmount(amount.subtract(frozenAmount));
            }

            walletBalanceModel.setCoinImgUrl(getCoinIconByCoinSymbol(accountListBean.getCurrency()));

            walletBalanceModel.setLocalMarketPrice(accountListBean.getMarketStringByLocalSymbol());

            walletBalanceModel.setLocalAmount(accountListBean.getAmountStringByLocalMarket());

            walletBalanceModel.setAddress(accountListBean.getCoinAddress());

            walletBalanceModel.setAccountNumber(accountListBean.getAccountNumber());

            walletBalanceModel.setCoinBalance(accountListBean.getCoinBalance());

            walletBalanceModel.setCoinType(accountListBean.getType());

            walletBalanceModels.add(walletBalanceModel);
        }

        return walletBalanceModels;
    }

    /**
     * 转换为adapter数据 （秘钥钱包）
     *
     * @param data
     * @return
     */
    @NonNull
    private List<WalletBalanceModel> transformToPrivateAdapterData(BalanceListModel data) {
            List<WalletBalanceModel> walletBalanceModels = new ArrayList<>();
        if (data == null) {
            return walletBalanceModels;
        }

        for (BalanceListModel.AccountListBean accountListBean : data.getAccountList()) {

            if (TextUtils.equals(accountListBean.getAddress(), SPUtilHelper.getPastBtcInfo().split("\\+")[0])){

                if (new BigDecimal(accountListBean.getBalance()).compareTo(BigDecimal.ZERO) == 0){
                    // 如果老版本老BTC地址余额为零则不展示
                    continue;
                }
            }

            WalletBalanceModel walletBalanceModel = new WalletBalanceModel();

            walletBalanceModel.setCoinSymbol(accountListBean.getSymbol());

            walletBalanceModel.setAvailableAmount(new BigDecimal(accountListBean.getBalance()));

            walletBalanceModel.setCoinImgUrl(getCoinIconByCoinSymbol(accountListBean.getSymbol()));

            walletBalanceModel.setLocalMarketPrice(accountListBean.getMarketStringByLocalSymbol());

            walletBalanceModel.setLocalAmount(accountListBean.getAmountStringByLocalMarket());

            walletBalanceModel.setAddress(accountListBean.getAddress());

            if (accountListBean.getBalance() != null) {
                walletBalanceModel.setCoinBalance(accountListBean.getBalance().toString());
            }

            walletBalanceModels.add(walletBalanceModel);
        }
        return walletBalanceModels;
    }

    /**
     * 获取消息列表
     */
    public void getMsgRequest() {

        Map<String, String> map = new HashMap<>();
        map.put("channelType", "4");
        map.put("start", "1");
        map.put("limit", "1");
        map.put("status", "1");
        map.put("fromSystemCode", AppConfig.SYSTEMCODE);
        map.put("toSystemCode", AppConfig.SYSTEMCODE);

        Call call = RetrofitUtils.createApi(MyApi.class).getMsgList("804040", StringUtils.getRequestJsonString(map));

        addCall(call);


        call.enqueue(new BaseResponseModelCallBack<MsgListModel>(mActivity) {
            @Override
            protected void onSuccess(MsgListModel data, String SucMessage) {
                if (data.getList() == null || data.getList().size() < 1) {
                    mBinding.linLayoutBulletin.setVisibility(View.GONE);
                    return;
                }
                mBinding.linLayoutBulletin.setVisibility(View.VISIBLE);
                mBinding.tvBulletin.setText(data.getList().get(0).getSmsTitle());
            }


            @Override
            protected void onFinish() {
            }
        });
    }


    /**
     * 获取 LinearLayoutManager
     *
     * @return LinearLayoutManager
     */
    private LinearLayoutManager getRecyclerViewLayoutManager() {
        return new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };

    }

    @Override
    public void onDestroy() {
        if (mlLocalCoinCachePresenter != null) {
            mlLocalCoinCachePresenter.clear();
        }
        super.onDestroy();
    }

    /**
     * 自选币种改变
     *
     * @param ad
     */
    @Subscribe
    public void addCoinChangeEventPri(AddCoinChangeEvent ad) {
        if (ad.getTag().equals(AddCoinChangeEvent.PRI)){
            mChooseCoinList = null;
            getPriWalletAssetsData(true, true);
        } else if (ad.getTag().equals(AddCoinChangeEvent.NOT_PRI)){
            getWalletAssetsData(false, true);
        }


    }


}
