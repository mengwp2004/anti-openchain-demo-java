package com.alipay.openchain.flow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.mychain.sdk.domain.account.Account;
import com.antfinancial.mychain.baas.tool.restclient.RestClient;
import com.antfinancial.mychain.baas.tool.restclient.RestClientProperties;
import com.antfinancial.mychain.baas.tool.restclient.model.AccountRequest;
import com.antfinancial.mychain.baas.tool.restclient.model.CallRestBizParam;
import com.antfinancial.mychain.baas.tool.restclient.model.Method;
import com.antfinancial.mychain.baas.tool.restclient.response.BaseResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * block chain account operations
 */
@Service
public class OperateChainAccount {
    @Autowired
    private RestClient restClient;

    @Autowired
    private RestClientProperties restClientProperties;

    public void runFlow () throws Exception {
        createChainAccount();
        queryChainAccount();
    }
    /**
     * 创建KMS托管密钥的区块链账户
     */
    public void createChainAccount() throws Exception {

        CallRestBizParam param = CallRestBizParam.builder().
                // 创建账户方法名称，固定值
                method(Method.TENANTCREATEACCUNT).
                // 本次交易请求ID
                orderId(UUID.randomUUID().toString()).
                // 执行创建账户交易的已有区块链账户
                account(restClientProperties.getAccount()).
                // 执行创建账户交易的已有区块链账户KMS密钥ID
                mykmsKeyId(restClientProperties.getKmsId()).
                // 新建区块链账户   不可重复  包括失败的账户名也不可使用
                newAccountId("newAccount").
                // 新建区块链账户KMS密钥ID 可重复
                newAccountKmsId("newKmsId").
                // 确保设置的Gas参数足够大，且执行创建的账户中有足够Gas，并且账户燃料要大于参数数值
                gas(100000L).
                build();
        BaseResp resp = restClient.chainCallForBiz(param);
        if(resp.isSuccess()) {
            System.out.println("创建账户成功，密钥:" + resp.getData());
        } else {
            System.err.println("创建账户失败: " + resp.getCode() + ", " + resp.getData());
        }
    }

    /**
     * 查询指定名称的区块链账户
     */
    public void queryChainAccount() throws Exception {
        AccountRequest request = restClient.createQueryAccountForBiz(
                restClientProperties.getAccount()  //需要查询的账户名
        );

        BaseResp resp = restClient.chainCall(
                null,
                JSON.toJSONString(request),
                Method.QUERYACCOUNT);
        System.out.println("response: " + JSON.toJSONString(resp));
        if(resp.isSuccess()) {
            JSONObject jsonObject = JSONObject.parseObject(resp.getData());
            Account account = new Account();
            account.fromJson(jsonObject);
            System.out.println("查询账户成功， 账户信息: ID " + account.getIdentity() + ", 余额 " + account.getBalance());
        } else {
            System.err.println("查询账户失败: " + resp.getCode() + ", " + resp.getData());
        }
    }
}
