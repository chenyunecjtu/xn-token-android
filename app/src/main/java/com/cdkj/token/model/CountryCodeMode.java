package com.cdkj.token.model;

/**
 * 国家编号
 * Created by cdkj on 2018/7/2.
 */

public class CountryCodeMode {


    /**
     * interCode : 0086
     * interName : China
     * chineseName : 中国
     * interSimpleCode : CN
     * continent : 亚洲
     * orderNo : 1
     * status : 1
     */

    private String interCode;
    private String code;
    private String interName;
    private String chineseName;
    private String localName;
    private String interSimpleCode;
    private String continent;
    private int orderNo;
    private String status;
    private String pic;

    // 首字母
    private String sort = "";

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public String getInterCode() {
        return interCode;
    }

    public void setInterCode(String interCode) {
        this.interCode = interCode;
    }

    public String getInterName() {
        return interName;
    }

    public void setInterName(String interName) {
        this.interName = interName;
    }

    public String getChineseName() {
        return chineseName;
    }

    public void setChineseName(String chineseName) {
        this.chineseName = chineseName;
    }

    public String getInterSimpleCode() {
        return interSimpleCode;
    }

    public void setInterSimpleCode(String interSimpleCode) {
        this.interSimpleCode = interSimpleCode;
    }

    public String getContinent() {
        return continent;
    }

    public void setContinent(String continent) {
        this.continent = continent;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
