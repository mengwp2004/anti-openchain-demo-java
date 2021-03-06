/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alipay.openchain.flow;

import com.alibaba.fastjson.JSONArray;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.utils.ByteUtils;
import com.antfinancial.mychain.baas.tool.restclient.RestClient;
import com.antfinancial.mychain.baas.tool.restclient.RestClientProperties;
import com.antfinancial.mychain.baas.tool.restclient.model.CallRestBizParam;
import com.antfinancial.mychain.baas.tool.restclient.model.ClientParam;
import com.antfinancial.mychain.baas.tool.restclient.model.Method;
import com.antfinancial.mychain.baas.tool.restclient.response.BaseResp;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigInteger;
import java.util.UUID;

/**
 * 合约相关场景下操作流，示例代码提供
 */
@Service
public class ContractFlowTest {
    private static final String CONTRACT_NAME = "gusto_bluesky_assert6";

    @Autowired
    private RestClient restClient;

    @Autowired
    private RestClientProperties restClientProperties;

    public void runFlow() throws Exception {
        //部署合约
        //String dsHash = deploySolidityContract();
        //queryResult(dsHash);
        //建议在Cloud Ide部署完合约后，在使用下面的调用合约
//        String csHash = callSolidityContract();
        String csHash = callSolidityContract();
        queryResult(csHash);
        //解析合约返回值
//        showOutPut(csHash);
    }

    //部署Solidity合约 也可以通过Cloud Ide进行部署
    public String deploySolidityContract() throws Exception {
        String path = ContractFlowTest.class.getClassLoader().getResource("contract.txt").getPath();
        byte[] creditBytes = FileUtils.readFileToByteArray(new File(path));
        String hexString = new String(creditBytes);
        System.out.println("文件" + hexString);
        ClientParam clientParam = restClient.createDeployContractTransaction(restClientProperties.getDefaultAccount(), CONTRACT_NAME,
                ByteUtils.hexStringToBytes(hexString), VMTypeEnum.EVM, 500000L);
        BaseResp resp = restClient.chainCall(clientParam.getHash(), clientParam.getSignData(), Method.DEPLOYCONTRACT);
        String deploySolidityContractHash = clientParam.getHash();
        System.out.println("[ContractFlow-部署合约] Hash: " + deploySolidityContractHash + "   Result: " + resp);
        return deploySolidityContractHash;
    }

    //部署带构造方法合约(需要使用密钥托管账户)
    public String deploySolidityContract2() throws Exception {
        CallRestBizParam restBizParam = CallRestBizParam.builder().
                // EVM合约部署方法名称，固定值
                        method(Method.DEPLOYCONTRACTFORBIZASYNC).
                // 唯一请求ID
                        orderId(UUID.randomUUID().toString()).
                // 执行交易的区块链账户
                        account(restClientProperties.getAccount()).
                // 执行交易的区块链账户KMS密钥ID
                        mykmsKeyId(restClientProperties.getKmsId()).
                // 合约名称
                        contractName("deployContractName").
                // 合约部署字节码
                        contractCode("deployContent").
                // 合约初始化方法签名
                        methodSignature("").
                // 合约初始化参数
                        inputParamListStr("").
                // 确保设置的Gas参数足够大，且执行创建的账户中有足够Gas，并且账户燃料要大于参数数值
                        gas(100000L).
                        build();
        BaseResp resp = restClient.bizChainCallWithReceipt(restBizParam);
        System.out.println("[ContractFlow-部署合约] Hash: " + restBizParam.getHash() + "   Result: " + resp);
        return restBizParam.getHash();
    }

    //调用Solidity合约
    public String callSolidityContract() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(Utils.getIdentityByName(restClientProperties.getDefaultAccount()));
        jsonArray.add(BigInteger.valueOf(100));
        ClientParam clientParam = restClient.createCallContractTransaction(
                restClientProperties.getDefaultAccount(),
                CONTRACT_NAME,                          //合约名  可以修改为cloud ide中部署的合约名
                "",                             //合约返回值类型
                //合约方法签名  注意：1.中间不要带空格  2.只写参数类型,不要带参数名 如:uint256 a,uint256 b  3.参数类型填写争取  请不要填写如int  需要填写完整uint
                "issue(identity,uint256)",
                jsonArray.toJSONString(),
                false,
                null,
                300000L);
        BaseResp resp = restClient.chainCall(clientParam.getHash(), clientParam.getSignData(), Method.CALLCONTRACT);
        System.out.println("[CallContract-调用合约] Hash: " + clientParam.getHash() + "    Result:" + resp);
        return clientParam.getHash();
    }

    //查询交易是否成功
    public void queryResult(String hash) throws Exception {
        Thread.sleep(2000);
        BaseResp queryBaseResp = restClient.chainCall(hash, restClientProperties.getBizid(), "", Method.QUERYRECEIPT);
        System.out.println(queryBaseResp);
    }

    //解析调用合约返回值  （解析返回值请填写show方法中参数，需要使用密钥托管账户）
    public void showOutPut(String hash) throws Exception {
        Thread.sleep(3000);
        BaseResp queryBaseResp = restClient.chainCall(hash, restClientProperties.getBizid(), "", Method.QUERYRECEIPT);
        String s = queryBaseResp.getData();
        String output = s.substring(s.indexOf("output") + 9, s.indexOf("result")).replace("\"", "").replace(",", "");
        if (0 != output.length()) {
            BaseResp show = show(output);
            System.out.println(show);
        } else {
            System.out.println(s);
        }
    }

    private BaseResp show(String output) throws Exception {
        byte[] content = Hex.encode(Base64.decode(output));
        CallRestBizParam callRestBizParam = CallRestBizParam.builder().
                bizid(restClientProperties.getBizid()).method(Method.PARSEOUTPUT).
                tenantid(restClientProperties.getTenantid()).
                orderId("order_" + System.currentTimeMillis()).
                vmTypeEnum(VMTypeEnum.EVM).content(new String(content)).
                abi("[\"bool\"]").                                      //TODO 合约返回值类型需自己根据合约修改
                mykmsKeyId(restClientProperties.getKmsId()).build();    //TODO 默认为application.yaml中KmsId
        BaseResp baseResp = restClient.chainCallForBiz(callRestBizParam);
        assert (baseResp.isSuccess());
        System.out.println("show" + baseResp);
        return baseResp;
    }
}