package com.dotcms.spa.proxy;

public class ProxyResponse {


    /**
     * The Response.
     */
    final private byte[] response;

    /**
     * The Response Code.
     */
    final int responseCode;

    public ProxyResponse(int rc, byte[] out) {
        this.response = out;
        this.responseCode = rc;
    }

    public byte[] getResponse() {
        return this.response;
    }

    public int getResponseCode() {
        return this.responseCode;
    }




}
