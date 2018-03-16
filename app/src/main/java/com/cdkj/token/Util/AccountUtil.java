package com.cdkj.token.Util;


import com.cdkj.token.R;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import static java.math.BigDecimal.ROUND_HALF_DOWN;

/**
 * Created by lei on 2017/10/20.
 */

public class AccountUtil {

    public static BigDecimal UNIT_MIN = new BigDecimal("10");
    public static BigDecimal UNIT_OGC = UNIT_MIN.pow(8);

    public static final String OGC = "OGC";
    public static final int OGCSCALE = 8;

    /**
     * 货币单位转换
     *
     * @param amount
     * @param coin
     * @return
     */
    public static String amountFormatUnit(BigDecimal amount, String coin, int scale) {

        if (amount == null) {
            return "0";
        }

        return scale(amount.divide(getUnit(coin)).toPlainString(), scale);

    }

    /**
     * 货币单位转换 带单位 OGC
     *
     * @param amount
     * @param coin
     * @return
     */
    public static String amountFormatUnitForShow(BigDecimal amount, String coin, int scale) {

        if (amount == null) {
            return "0 " + OGC;
        }

        return scale(amount.divide(getUnit(coin)).toPlainString(), scale) + " " + OGC;

    }

    /**
     * 格式化输出的金额格式，最多8位小数
     *
     * @param s
     * @return
     */
    public static String scale(String s, int scale) {
        String amount[] = s.split("\\.");
        if (amount.length > 1) {
            if (amount[1].length() > scale) {
                return amount[0] + "." + amount[1].substring(0, scale);
            } else {
                return amount[0] + "." + amount[1];
            }
        } else {
            return amount[0];
        }
    }

    /**
     * 根据货币获取最小单位
     *
     * @param coin
     * @return
     */
    public static BigDecimal getUnit(String coin) {

        switch (coin) {

            case OGC:
                return UNIT_OGC;

            default:
                return new BigDecimal(0);
        }

    }


    public static String formatDouble(double money) {
        DecimalFormat df = new DecimalFormat("#######0.000");
        String showMoney = df.format(money);

        return showMoney.substring(0, showMoney.length() - 1);
    }

    public static String formatInt(double money) {
        DecimalFormat df = new DecimalFormat("#######0.000");
        String showMoney = df.format(money);

        return showMoney.substring(0, showMoney.length() - 1).split("\\.")[0];
    }

    public static String formatDoubleDiv(String money, int scale) {

        DecimalFormat df = new DecimalFormat("#######0.000");
        String showMoney = df.format((Double.parseDouble(money) / scale));

        return showMoney.substring(0, showMoney.length() - 1);
    }

    public static String getCurrency(String currency) {
        switch (currency) {
            case "ETH":
                return StringUtil.getString(R.string.coin_eth);

            case "SC":
                return StringUtil.getString(R.string.coin_sc);

            case "BTC":
                return StringUtil.getString(R.string.coin_btc);

            default:
                return "";

        }
    }

    public static String formatBizType(String bizType) {
        switch (bizType) {
            case "charge":
                return StringUtil.getString(R.string.biz_type_charge);

            case "withdraw":
                return StringUtil.getString(R.string.biz_type_withdraw);

            case "autofill":
                return StringUtil.getString(R.string.biz_type_autofill);

            case "buy":
                return StringUtil.getString(R.string.biz_type_buy);

            case "sell":
                return StringUtil.getString(R.string.biz_type_sell);

            case "tradefrozen":
                return StringUtil.getString(R.string.biz_type_tradefrozen);

            case "tradeunfrozen":
                return StringUtil.getString(R.string.biz_type_tradeunfrozen);

            case "withdrawfrozen":
                return StringUtil.getString(R.string.biz_type_withdrawfrozen);

            case "withdrawunfrozen":
                return StringUtil.getString(R.string.biz_type_withdrawunfrozen);

            case "tradefee":
                return StringUtil.getString(R.string.biz_type_tradefee);

            case "withdrawfee":
                return StringUtil.getString(R.string.biz_type_withdrawfee);

            case "invite":
                return StringUtil.getString(R.string.biz_type_invite);
            case "trans":
                return StringUtil.getString(R.string.biz_type_trans);
            default:
                return "";

        }
    }


    public static String formatBillStatus(String status) {

        switch (status) {

            case "1":
                return StringUtil.getString(R.string.biz_status_daiduizhang);

            case "3":
                return StringUtil.getString(R.string.biz_status_yiduiyiping);

            case "4":
                return StringUtil.getString(R.string.biz_status_zhangbuping);

            case "5":
                return StringUtil.getString(R.string.biz_status_yiduibuping);

            case "6":
                return StringUtil.getString(R.string.biz_status_wuxuduizhang);

            default:
                return "";
        }

    }

    /**
     * 提供精确加法计算的add方法
     *
     * @param value1 被加数
     * @param value2 加数
     * @return 两个参数的和
     */
    public static String add(double value1, double value2) {
        BigDecimal b1 = new BigDecimal(Double.valueOf(value1));
        BigDecimal b2 = new BigDecimal(Double.valueOf(value2));
        return b1.add(b2).toPlainString();
    }

    /**
     * 提供精确减法运算的sub方法
     *
     * @param value1 被减数
     * @param value2 减数
     * @return 两个参数的差
     */
    public static String sub(double value1, double value2, String coin) {
        BigDecimal b1 = new BigDecimal(Double.valueOf(value1));
        BigDecimal b2 = new BigDecimal(Double.valueOf(value2));
        return amountFormatUnit(b1.subtract(b2), coin, 8);
    }

    /**
     * 提供精确乘法运算的mul方法
     *
     * @param value1 被乘数
     * @param value2 乘数
     * @return 两个参数的积
     */
    public static String mul(double value1, double value2) {
        BigDecimal b1 = new BigDecimal(Double.valueOf(value1));
        BigDecimal b2 = new BigDecimal(Double.valueOf(value2));
        return b1.multiply(b2).toPlainString();
    }

    /**
     * 提供精确的除法运算方法div
     *
     * @param value1 被除数
     * @param value2 除数
     * @param scale  精确范围
     * @return 两个参数的商
     * @throws IllegalAccessException
     */
    public static String div(double value1, double value2, int scale) throws IllegalAccessException {
        //如果精确范围小于0，抛出异常信息
        if (scale < 0) {
            throw new IllegalAccessException("精确度不能小于0");
        }
        BigDecimal b1 = new BigDecimal(Double.valueOf(value1));
        BigDecimal b2 = new BigDecimal(Double.valueOf(value2));


        return b1.divide(b2, scale, ROUND_HALF_DOWN).toPlainString();
    }

    /**
     * 去掉末尾的零
     *
     * @param str
     * @return
     */
    public static String trim(String str) {
        if (str.indexOf(".") != -1 && str.charAt(str.length() - 1) == '0') {
            return trim(str.substring(0, str.length() - 1));
        } else {
            return str.charAt(str.length() - 1) == '.' ? str.substring(0, str.length() - 1) : str;
        }
    }
}
