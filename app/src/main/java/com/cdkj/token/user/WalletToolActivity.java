package com.cdkj.token.user;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.View;

import com.cdkj.baselibrary.appmanager.SPUtilHelper;
import com.cdkj.baselibrary.base.AbsLoadActivity;
import com.cdkj.token.R;
import com.cdkj.token.databinding.ActivityUserWalletBinding;
import com.cdkj.token.utils.wallet.WalletHelper;
import com.cdkj.token.wallet.WalletPasswordModifyActivity;
import com.cdkj.token.wallet.backup_guide.BackupWalletStartActivity;
import com.cdkj.token.wallet.backup_guide.WalletBackupPasswordCheckActivity;
import com.cdkj.token.wallet.delete_guide.WallteDeleteStartActivity;
import com.cdkj.token.wallet.export.WalletExportPasswordCheckActivity;

/**
 * 钱包工具
 * Created by cdkj on 2018/5/26.
 */

public class WalletToolActivity extends AbsLoadActivity {

    private ActivityUserWalletBinding mBinding;

    public static void open(Context context) {
        if (context == null) {
            return;
        }
        context.startActivity(new Intent(context, WalletToolActivity.class));
    }

    @Override
    public View addMainView() {
        mBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.activity_user_wallet, null, false);
        return mBinding.getRoot();
    }

    @Override
    public void afterCreate(Bundle savedInstanceState) {

        mBaseBinding.titleView.setMidTitle(getStrRes(R.string.wallet_tool));

        setStatusBarBlue();
        setTitleBgBlue();
        initClickListener();

        mBinding.tvWalletName.setText(WalletHelper.getWalletNameByUserId(SPUtilHelper.getUserId()));

    }


    private void initClickListener() {
        mBinding.llBackUp.setOnClickListener(view -> BackupWalletStartActivity.open(this));
        mBinding.llModify.setOnClickListener(view -> WalletPasswordModifyActivity.open(this));
        mBinding.btnDelete.setOnClickListener(view -> WallteDeleteStartActivity.open(this));
        mBinding.llOut.setOnClickListener(view -> WalletExportPasswordCheckActivity.open(this));
    }
}
