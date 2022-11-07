package com.yy.sdk.abooster;

/**
 * Create by nls on 2022/8/11
 * description: StateCode
 */
public enum StateCode {
    NORMAL,
    STATE1_OVERTIME,
    STATE2_INIT,
    STATE3_HTTP_FAIL,
    STATE4_SDK_ERROR,
    STATE5_SDK_ERROR2,
    STATE6_UNINIT;

    private StateCode() {
    }
}
