package com.deem.zkui.bo;

/**
 * 封装认证结果
 */
public class AuthResult {
    private Boolean authed=false;
    private String errMsg;

    public AuthResult() {
    }

    public Boolean getAuthed() {
        return authed;
    }

    public void setAuthed(Boolean authed) {
        this.authed = authed;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
}
