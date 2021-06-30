/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alipay.openchain.flow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.antfinancial.mychain.baas.tool.restclient.RestClient;
import com.antfinancial.mychain.baas.tool.restclient.RestClientProperties;
import com.antfinancial.mychain.baas.tool.restclient.model.CallRestBizParam;
import com.antfinancial.mychain.baas.tool.restclient.model.ClientParam;
import com.antfinancial.mychain.baas.tool.restclient.model.Method;
import com.antfinancial.mychain.baas.tool.restclient.response.BaseResp;
import com.antfinancial.mychain.baas.tool.restclient.response.ReplyTransaction;
import com.antfinancial.mychain.baas.tool.restclient.response.TransactionDO;
import com.google.gson.JsonObject;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 存证场景示例
 */
@Service
public class DepositFlow {
    @Autowired
    private RestClient restClient;

    @Autowired
    private RestClientProperties restClientProperties;

    public void runFlow() throws Exception {
        //发起存证   接收返回的交易hash
        String hash=depositData();

        //因此此处演示查询之前的交易Hash
        String hash2="6b17b9d4ec66005fb29b4123b0d380973491d8e9a064a92d0bedc4971b045e42";
        queryTransaction(hash2);

    }

    /**
     * 数据上链存证
     */
    public String depositData() throws Exception {
        //存证内容
        String data = "Test deposit data at " + System.currentTimeMillis();
        //gas参数根据交易复杂度进行调整。越复杂需要越多，当前5W能满足，如果存证内容需改变，请相应修改gas参数
        ClientParam clientParam = restClient.createDepositTransaction(restClientProperties.getDefaultAccount(), data, 50000l);
        //发起存证
        BaseResp result = restClient.chainCall(clientParam.getHash(), clientParam.getSignData(), Method.DEPOSIT);
       System.out.println("[DepositFlow-存证数据] Hash: " + clientParam.getHash() + "Result: " + result);

        String hash = clientParam.getHash();
        Thread.sleep(3000);
        //查询交易回执（存证交易是否成功需要根据该接口返回值确定,BaseResp返回200，并不代表成功）
        result = restClient.chainCall(hash, restClientProperties.getBizid(), "", Method.QUERYRECEIPT);
        System.out.println("[DepositFlow-查询交易回执] " + result);
        return clientParam.getHash();
    }

    /**
     * 通过交易Hash查询链上数据
     */
    public BaseResp queryTransaction(String hash) throws Exception {
        BaseResp result = restClient.chainCall(hash, null, Method.QUERYTRANSACTION);
        if(result.isSuccess() && "200".equals(result.getCode())) {
            ReplyTransaction transaction = JSON.parseObject(result.getData(), ReplyTransaction.class);
            TransactionDO transactionDO = transaction.getTransactionDO();
            String data = new String(transactionDO.getData());
            System.out.println("[DepositFlow-存证内容] " + data);
        }
        return result;
    }


}