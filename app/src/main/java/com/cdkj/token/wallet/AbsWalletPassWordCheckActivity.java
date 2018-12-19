package com.cdkj.token.wallet;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.View;

import com.cdkj.baselibrary.base.AbsLoadActivity;
import com.cdkj.token.R;
import com.cdkj.token.databinding.ActivityCreatePassWordBinding;
import com.cdkj.token.utils.wallet.WalletHelper;
import com.cdkj.token.views.password.PassWordLayout;

/**
 * 钱包密码验证
 * Created by cdkj on 2018/6/7.
 */

public abstract class AbsWalletPassWordCheckActivity extends AbsLoadActivity {

    protected ActivityCreatePassWordBinding mBinding;


    public static void open(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, AbsWalletPassWordCheckActivity.class);
        context.startActivity(intent);
    }


    @Override
    public View addMainView() {
        mBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.activity_create_pass_word, null, false);
        return mBinding.getRoot();
    }

    @Override
    public void afterCreate(Bundle savedInstanceState) {
        initPassWordVIewListener();
    }

    protected void initPassWordVIewListener() {

        mBinding.passWordLayout.passWordLayout.setPwdChangeListener(new PassWordLayout.pwdChangeListener() {
            @Override
            public void onChange(String pwd) {
            }

            @Override
            public void onNull() {

            }

            @Override
            public void onFinished(String pwd) {
                checkOldPassword(pwd);
            }
        });
    }

    /**
     * 检测旧密码
     *
     * @param pwd
     */
    private void checkOldPassword(String pwd) {
        checkPassWord(WalletHelper.checkPasswordByUserId(pwd, WalletHelper.WALLET_USER));
    }


    //密码是否输入成功
    public abstract void checkPassWord(boolean isSuccess);


}
