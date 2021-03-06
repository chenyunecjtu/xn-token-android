package com.cdkj.baselibrary.interfaces;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.cdkj.baselibrary.R;
import com.cdkj.baselibrary.appmanager.AppConfig;
import com.cdkj.baselibrary.appmanager.SPUtilHelper;
import com.cdkj.baselibrary.model.IsSuccessModes;
import com.cdkj.baselibrary.model.SendVerificationCode;
import com.cdkj.baselibrary.nets.BaseResponseModelCallBack;
import com.cdkj.baselibrary.nets.RetrofitUtils;
import com.cdkj.baselibrary.utils.StringUtils;
import com.cdkj.baselibrary.utils.ToastUtil;
import com.li.verification.VerificationAliActivity;

import java.util.HashMap;

import retrofit2.Call;

/**
 * 发送验证码
 * Created by cdkj on 2017/8/8.
 */
public class SendPhoneCodePresenter {

    private SendCodeInterface mListener;
    private Activity activity;
    private Call call;

    public int AL_IVERIFICATION_REQUEST_CODE = 100;

    private SendVerificationCode sendVerificationCode;

    public SendPhoneCodePresenter(SendCodeInterface view, Activity activity) {
        this.mListener = view;
        this.activity = activity;
    }


    /**
     * 启动安全验证界面
     *
     * @param sendVerificationCode
     */
    public void openVerificationActivity(SendVerificationCode sendVerificationCode) {
        this.sendVerificationCode = sendVerificationCode;
        if (sendVerificationCode == null) {
            return;
        }
        if (TextUtils.isEmpty(sendVerificationCode.getPhone())) {
            ToastUtil.show(activity, this.activity.getString(R.string.activity_mobile_mobile_hint));
            return;
        }
        VerificationAliActivity.open(activity, AL_IVERIFICATION_REQUEST_CODE);
    }

    /**
     * 处理验证后回调 并发送验证码
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null || sendVerificationCode == null) return;
        if (requestCode == AL_IVERIFICATION_REQUEST_CODE && resultCode == VerificationAliActivity.RESULT_CODE) {
            String sessionid = data.getStringExtra(VerificationAliActivity.SESSIONID);
            sendVerificationCode.setSessionID(sessionid);
            if (TextUtils.isEmpty(sendVerificationCode.getCountryCode()) || TextUtils.isEmpty(sessionid)) {
                return;
            }
            request(sendVerificationCode);
        }
    }


    /**
     * 请求
     */
    private void request(SendVerificationCode sendVerificationCode) {

        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put("systemCode", AppConfig.SYSTEMCODE);
        hashMap.put("companyCode", AppConfig.COMPANYCODE);
        hashMap.put("mobile", sendVerificationCode.getPhone());
        hashMap.put("bizType", sendVerificationCode.getBizType());
        hashMap.put("kind", sendVerificationCode.getKind());
        hashMap.put("interCode", sendVerificationCode.getCountryCode()); //国际区号
        hashMap.put("sessionId", sendVerificationCode.getSessionID()); //阿里验证
        call = RetrofitUtils.getBaseAPiService().successRequest("805953", StringUtils.getRequestJsonString(hashMap));

        mListener.StartSend();
        call.enqueue(new BaseResponseModelCallBack<IsSuccessModes>(activity) {
            @Override
            protected void onSuccess(IsSuccessModes data, String SucMessage) {
                if (data.isSuccess()) {
                    ToastUtil.show(activity, activity.getString(R.string.smscode_send_success));
                    mListener.CodeSuccess(activity.getString(R.string.smscode_send_success));
                } else {
                    mListener.CodeFailed("", activity.getString(R.string.smscode_send_success));
                }
            }


            @Override
            protected void onFinish() {
                mListener.EndSend();
            }
        });
    }

    //处理持有对象
    public void clear() {
        if (this.call != null) {
            this.call.cancel();
            this.call = null;
        }
        this.mListener = null;
        this.activity = null;
    }


}
