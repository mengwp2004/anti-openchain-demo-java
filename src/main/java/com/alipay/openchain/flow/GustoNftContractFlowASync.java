/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alipay.openchain.flow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.domain.account.Identity;
import com.alipay.mychain.sdk.domain.transaction.LogEntry;
import com.alipay.mychain.sdk.utils.ByteUtils;
import com.alipay.mychain.sdk.vm.EVMOutput;
import com.antfinancial.mychain.baas.tool.restclient.RestClient;
import com.antfinancial.mychain.baas.tool.restclient.RestClientProperties;
import com.antfinancial.mychain.baas.tool.restclient.model.CallRestBizParam;
import com.antfinancial.mychain.baas.tool.restclient.model.ClientParam;
import com.antfinancial.mychain.baas.tool.restclient.model.Method;
import com.antfinancial.mychain.baas.tool.restclient.model.ReceiptDecoration;
import com.antfinancial.mychain.baas.tool.restclient.response.BaseResp;
import com.antfinancial.mychain.baas.tool.utils.ContractParameterUtils;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;

/**
 * 合约相关场景下操作流，示例代码提供
 */
@Service
public class GustoNftContractFlowASync {
    //private static final String CONTRACT_NAME = "gusto_bluesky_assert2"; // mengwp_2012账号　
    private static final String CONTRACT_NAME = "gustoUser2"; // mengwp_2012账号　

    private static final String COMMODITY_CONTRACT_NAME = "media4";
    private static final String SUPPER_MARKET_CONTRACT_NAME = "mediaMarket1";
    private static final String NFT_721_CONTRACT_NAME = "gustoNft1";
    private static final String RENT_CONTRACT_NAME = "rent2";

    @Autowired
    private RestClient restClient;

    @Autowired
    private RestClientProperties restClientProperties;

    public void runFlow() throws Exception {

        //铸造
        //String csHash = mintMedia();

        //注销，只有没有卖的nft才可以注销
        //burnMedia();

        //购买
        //String csHash = saleOneTo();

        //token 转移
        transferFrom();
        //查询指定账户的nft数目
        //String csHash = balanceOf();

        //根据token Id查询owner
        //ownerOf();

        //添加租赁人
        //addRent();

        //去除租赁人
        //removeRent();

        //queryResult(csHash);
        //解析合约返回值
        //showOutPut(csHash);



    }

    //部署Solidity合约 也可以通过Cloud Ide进行部署
    public String deploySolidityContract() throws Exception {
        String path = GustoNftContractFlowASync.class.getClassLoader().getResource("contract.txt").getPath();
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
                abi("[\"identity\"]").                                      //TODO 合约返回值类型需自己根据合约修改
                mykmsKeyId(restClientProperties.getKmsId()).build();    //TODO 默认为application.yaml中KmsId
        BaseResp baseResp = restClient.chainCallForBiz(callRestBizParam);
        assert (baseResp.isSuccess());
        System.out.println("show" + baseResp);
        return baseResp;
    }


    //调用Solidity合约
    //合约方法签名  注意：1.中间不要带空格  2.只写参数类型,不要带参数名 如:uint256 a,uint256 b  3.参数类型填写争取  请不要填写如int  需要填写完整uint
    /*
      功能说明:铸造
      输入参数:
                cid:uint256
                _author:identity
                _name:string
                _pDescription:string
                _url:string
                _totalSupply:uint256
      事件:

            MintMedia(
            _cid,
            _name,
            _author,
            _pDescription,
            _url,
            _totalSupply
            );

    */

    public String mintMedia() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(BigInteger.valueOf(3));
        jsonArray.add(new Identity("be8785e54d8c4c6f265ab7628099491c7deaf6c8d80459f3ad11881aa92bea46"));
        jsonArray.add("music 1");
        jsonArray.add("music desc");
        jsonArray.add("https://");
        jsonArray.add(1000);

        String orderId = "order_" + System.currentTimeMillis();
        CallRestBizParam callRestBizParam = CallRestBizParam.builder()
                .orderId(orderId)
                .bizid(restClientProperties.getBizid())
                .account("gustoUser2")
                .contractName(COMMODITY_CONTRACT_NAME)
                .methodSignature("mintMedia(uint256,identity,string,string,string,uint256)")
                .inputParamListStr(jsonArray.toJSONString())
                .outTypes("void")
                .mykmsKeyId("Z6pJGriuKGPAQENO1625040533402")
                .method(Method.CALLCONTRACTBIZASYNC)
                .tenantid(restClientProperties.getTenantid())
                .gas(1000000L).build();
        BaseResp baseResp = restClient.chainCallForBiz(callRestBizParam);

        System.out.println("mintMedia " + JSONObject.toJSONString(baseResp));
        if (baseResp.getCode().compareToIgnoreCase("200") == 0) {
            String hash = baseResp.getData();
            BaseResp queryBaseResp = restClient.chainCall(hash, restClientProperties.getBizid(), "", Method.QUERYRECEIPT);
            String s = queryBaseResp.getData();

            if (queryBaseResp.getCode().compareToIgnoreCase("200") == 0) {
                ReceiptDecoration transaction = JSON.parseObject(queryBaseResp.getData(), ReceiptDecoration.class);

                for (LogEntry log : transaction.getLogs()) {
                    if (log.getLogData().length > 0) {

                        //传入回执中的logdata转换为EVMoutput
                        System.out.println("mintMedia query receipt successful " + JSONObject.toJSONString(baseResp));
                        EVMOutput logOutput = new EVMOutput(Hex.toHexString(log.getLogData()));
                        //根据事件传入类型按顺序传值,如event test(string a,uint256 b); 则填写asList("string","uint256")
                        List<Object> resultList = ContractParameterUtils.getEVMOutput(logOutput,
                                asList( "uint256", "string", "identity", "string", "string", "uint256"));
                        for (Object o : resultList) {
                            System.out.println("mintMedia  param:" + o.toString());
                        }
                        break;
                    }
                }
            } else {
                System.out.println("mintMedia query receipt error " + JSONObject.toJSONString(baseResp));
                if (queryBaseResp.getCode().compareToIgnoreCase("10201") == 0) {
                    System.out.println("mintMedia  alread create media  " + JSONObject.toJSONString(queryBaseResp));
                }
            }
            return null;
        } else {
            System.out.println("mintMedia call error " + JSONObject.toJSONString(baseResp));
            return null;
        }
    }

    //调用Solidity合约
    //合约方法签名  注意：1.中间不要带空格  2.只写参数类型,不要带参数名 如:uint256 a,uint256 b  3.参数类型填写争取  请不要填写如int  需要填写完整uint
    /*
      功能说明:铸造
      输入参数:
                _cid:uint256
                _burnNum:uint256
      事件:

            BurnMedia(
            _cid,
            _burnNum,
            _totalSupply
            );

    */

    public String burnMedia() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(BigInteger.valueOf(3));
        jsonArray.add(10);

        String orderId = "order_" + System.currentTimeMillis();
        CallRestBizParam callRestBizParam = CallRestBizParam.builder()
                .orderId(orderId)
                .bizid(restClientProperties.getBizid())
                .account("gustoUser2")
                .contractName(COMMODITY_CONTRACT_NAME)
                .methodSignature("burnMedia(uint256,uint256)")
                .inputParamListStr(jsonArray.toJSONString())
                .outTypes("void")
                .mykmsKeyId("Z6pJGriuKGPAQENO1625040533402")
                .method(Method.CALLCONTRACTBIZASYNC)
                .tenantid(restClientProperties.getTenantid())
                .gas(1000000L).build();
        BaseResp baseResp = restClient.chainCallForBiz(callRestBizParam);

        System.out.println("burnMedia " + JSONObject.toJSONString(baseResp));
        if (baseResp.getCode().compareToIgnoreCase("200") == 0) {
            String hash = baseResp.getData();
            BaseResp queryBaseResp = restClient.chainCall(hash, restClientProperties.getBizid(), "", Method.QUERYRECEIPT);
            String s = queryBaseResp.getData();

            if (queryBaseResp.getCode().compareToIgnoreCase("200") == 0) {
                ReceiptDecoration transaction = JSON.parseObject(queryBaseResp.getData(), ReceiptDecoration.class);

                for (LogEntry log : transaction.getLogs()) {
                    if (log.getLogData().length > 0) {

                        //传入回执中的logdata转换为EVMoutput
                        System.out.println("burnMedia query receipt successful " + JSONObject.toJSONString(baseResp));
                        EVMOutput logOutput = new EVMOutput(Hex.toHexString(log.getLogData()));
                        //根据事件传入类型按顺序传值,如event test(string a,uint256 b); 则填写asList("string","uint256")
                        List<Object> resultList = ContractParameterUtils.getEVMOutput(logOutput,
                                asList( "uint256", "uint256","uint256"));
                        for (Object o : resultList) {
                            System.out.println("burnMedia  param:" + o.toString());
                        }
                        break;
                    }
                }
            } else {
                System.out.println("burnMedia query receipt error " + JSONObject.toJSONString(baseResp));
                if (queryBaseResp.getCode().compareToIgnoreCase("10201") == 0) {
                    System.out.println("burnMedia  alread create media  " + JSONObject.toJSONString(queryBaseResp));
                }
            }
            return null;
        } else {
            System.out.println("burnMedia call error " + JSONObject.toJSONString(baseResp));
            return null;
        }
    }


    /*
    功能说明:客户买入
    输入参数:
               _cid:uint256
               _sid:uint256
               _to:identity
    事件:
               SaleOneTo( _cid, _sid, _tokenID, msg.sender);
   */
    public String saleOneTo() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(BigInteger.valueOf(1));
        jsonArray.add(BigInteger.valueOf(1));
        jsonArray.add(new Identity("0bdaeaefe144a0eee2e11cf1030d7607a9c53abaf4606db55d3053029dd6bda9"));

        String orderId = "order_" + System.currentTimeMillis();
        CallRestBizParam callRestBizParam = CallRestBizParam.builder()
                .orderId(orderId)
                .bizid(restClientProperties.getBizid())
                .account("gustoUser2")
                .contractName(SUPPER_MARKET_CONTRACT_NAME)
                .methodSignature("saleOneTo(uint256,uint256,identity)")
                .inputParamListStr(jsonArray.toJSONString())
                .outTypes("void")
                .mykmsKeyId("Z6pJGriuKGPAQENO1625040533402")
                .method(Method.CALLCONTRACTBIZASYNC)
                .tenantid(restClientProperties.getTenantid())
                .gas(1000000L).build();
        BaseResp baseResp = restClient.chainCallForBiz(callRestBizParam);


        /*
        BaseResp baseResp = new BaseResp();
        baseResp.setCode("200");
        baseResp.setData("e2b08b023b53eb17d2c21b11daec1d1e66ff9d96848038b0cb3244819dceabb7");
        baseResp.setSuccess(true);
        */


        System.out.println("saleOneTo " + JSONObject.toJSONString(baseResp));
        if (baseResp.getCode().compareToIgnoreCase("200") == 0) {
            Thread.sleep(3000);
            String hash = baseResp.getData();
            BaseResp queryBaseResp = restClient.chainCall(hash, restClientProperties.getBizid(), "", Method.QUERYRECEIPT);
            String s = queryBaseResp.getData();

            if (queryBaseResp.getCode().compareToIgnoreCase("200") == 0) {
                ReceiptDecoration transaction = JSON.parseObject(queryBaseResp.getData(), ReceiptDecoration.class);

                for (LogEntry log : transaction.getLogs()) {

                    if (log.getLogData().length > 0) {

                        //传入回执中的logdata转换为EVMoutput
                        System.out.println("saleOneTo query receipt successful " + JSONObject.toJSONString(baseResp));
                        EVMOutput logOutput = new EVMOutput(Hex.toHexString(log.getLogData()));
                        //根据事件传入类型按顺序传值,如event test(string a,uint256 b); 则填写asList("string","uint256")
                        List<Object> resultList = ContractParameterUtils.getEVMOutput(logOutput,
                                asList("uint256", "uint256", "uint256", "identity"));
                        for (Object o : resultList) {
                            System.out.println("saleOneTo buyOne param:" + o.toString());
                        }
                        break;
                    }
                }
            } else {
                System.out.println("saleOneTo query receipt error " + JSONObject.toJSONString(queryBaseResp));
                if (queryBaseResp.getCode().compareToIgnoreCase("10201") == 0) {
                    System.out.println("buyOneNoFee  alread   " + JSONObject.toJSONString(queryBaseResp));
                }
            }
            return null;
        } else {
            System.out.println("saleOneTo call error " + JSONObject.toJSONString(baseResp));
            return null;
        }

    }


    /*
   功能说明:token转移
   输入参数:
              _from:identity
              _to:identity
              _tokenId:uint256
   事件:
              Transfer(from, _to, _tokenId);
  */
    public String transferFrom() throws Exception {
        JSONArray jsonArray = new JSONArray();
        //jsonArray.add(new Identity("4b61cd266a5d6ce40ddec2e43ef75c1aadc5cedb113da980802ebec4532d1e5f"));
        jsonArray.add(new Identity("0bdaeaefe144a0eee2e11cf1030d7607a9c53abaf4606db55d3053029dd6bda9"));
        jsonArray.add(new Identity("4b61cd266a5d6ce40ddec2e43ef75c1aadc5cedb113da980802ebec4532d1e5f"));
        jsonArray.add(BigInteger.valueOf(1));
        String orderId = "order_" + System.currentTimeMillis();
        CallRestBizParam callRestBizParam = CallRestBizParam.builder()
                .orderId(orderId)
                .bizid(restClientProperties.getBizid())
                .account("gustoUser2")
                .contractName(NFT_721_CONTRACT_NAME)
                .methodSignature("transferFrom(identity,identity,uint256)")
                .inputParamListStr(jsonArray.toJSONString())
                .outTypes("void")
                .mykmsKeyId("Z6pJGriuKGPAQENO1625040533402")
                .method(Method.CALLCONTRACTBIZASYNC)
                .tenantid(restClientProperties.getTenantid())
                .gas(1000000L).build();
        BaseResp baseResp = restClient.chainCallForBiz(callRestBizParam);

        /*
        BaseResp baseResp = new BaseResp();
        baseResp.setCode("200");
        baseResp.setData("e2b08b023b53eb17d2c21b11daec1d1e66ff9d96848038b0cb3244819dceabb7");
        baseResp.setSuccess(true);
        */


        System.out.println("transferFrom " + JSONObject.toJSONString(baseResp));
        if (baseResp.getCode().compareToIgnoreCase("200") == 0) {
            Thread.sleep(3000);
            String hash = baseResp.getData();
            BaseResp queryBaseResp = restClient.chainCall(hash, restClientProperties.getBizid(), "", Method.QUERYRECEIPT);
            String s = queryBaseResp.getData();

            if (queryBaseResp.getCode().compareToIgnoreCase("200") == 0) {
                ReceiptDecoration transaction = JSON.parseObject(queryBaseResp.getData(), ReceiptDecoration.class);

                Integer i = 0;
                //LogEntry log = transaction.getLogs().get(1);
                for (LogEntry log : transaction.getLogs()) {

                    i++;
                    if (log.getLogData().length > 0) {
                    //if(i == 2){

                        //传入回执中的logdata转换为EVMoutput
                        System.out.println("transferFrom query receipt successful " + JSONObject.toJSONString(baseResp));
                        EVMOutput logOutput = new EVMOutput(Hex.toHexString(log.getLogData()));
                        //根据事件传入类型按顺序传值,如event test(string a,uint256 b); 则填写asList("string","uint256")
                        List<Object> resultList = ContractParameterUtils.getEVMOutput(logOutput,
                                asList( "identity", "identity","uint256"));
                        for (Object o : resultList) {
                            System.out.println("transferFrom param:" + o.toString());
                        }
                        break;
                    }
                }
            } else {
                System.out.println("transferFrom query receipt error " + JSONObject.toJSONString(queryBaseResp));
                if (queryBaseResp.getCode().compareToIgnoreCase("10201") == 0) {
                    System.out.println("transferFrom  alread   " + JSONObject.toJSONString(queryBaseResp));
                }
            }
            return null;
        } else {
            System.out.println("transferFrom call error " + JSONObject.toJSONString(baseResp));
            return null;
        }

    }


    /*
     功能说明:查询指定账户拥有的nft数目
     输入参数:
           _owner:identity

     返回值:
           num:uint256

     */
    public String balanceOf() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(new Identity("be8785e54d8c4c6f265ab7628099491c7deaf6c8d80459f3ad11881aa92bea46"));

        String orderId = "order_" + System.currentTimeMillis();
        CallRestBizParam callRestBizParam = CallRestBizParam.builder()
                .orderId(orderId)
                .bizid(restClientProperties.getBizid())
                .account("gusto_test1")
                .contractName(NFT_721_CONTRACT_NAME)
                .methodSignature("balanceOf(identity)")
                .inputParamListStr(jsonArray.toJSONString())
                .outTypes("uint256")
                .mykmsKeyId("Il726LGGKGPAQENO1624004353911")
                .method(Method.CALLCONTRACTBIZASYNC)
                .tenantid(restClientProperties.getTenantid())
                .gas(1000000L).build();
        BaseResp baseResp = restClient.chainCallForBiz(callRestBizParam);

        //System.out.println(JSONObject.toJSONString(baseResp));
        if (baseResp.getCode().compareToIgnoreCase("200") == 0) {
            Thread.sleep(3000);
            String hash = baseResp.getData();
            BaseResp queryBaseResp = restClient.chainCall(hash, restClientProperties.getBizid(), "", Method.QUERYRECEIPT);
            String s = queryBaseResp.getData();

            String output = s.substring(s.indexOf("output") + 9, s.indexOf("result")).replace("\"", "").replace(",", "");
            if (0 != output.length()) {
                byte[] content = Hex.encode(Base64.decode(output));
                CallRestBizParam _callRestBizParam = CallRestBizParam.builder().
                        bizid(restClientProperties.getBizid()).method(Method.PARSEOUTPUT).
                        tenantid(restClientProperties.getTenantid()).
                        orderId("order_" + System.currentTimeMillis()).
                        vmTypeEnum(VMTypeEnum.EVM).content(new String(content)).
                        abi("[\"uint256\"]").                                      //TODO 合约返回值类型需自己根据合约修改
                        mykmsKeyId(restClientProperties.getKmsId()).build();    //TODO 默认为application.yaml中KmsId
                BaseResp _baseResp = restClient.chainCallForBiz(_callRestBizParam);
                if (_baseResp.isSuccess()) {
                    String data = _baseResp.getData();
                    JSONArray identities = JSONObject.parseArray(data);
                    System.out.println("balanceOf balance:" + identities.getString(0));
                    return identities.getString(0);
                } else {
                    System.out.print("balanceOf parse output error " + JSONObject.toJSONString(_baseResp));
                    return null;
                }
            } else {
                System.out.print("balanceOf query receipt error " + JSONObject.toJSONString(baseResp));
                return null;
            }
        } else {
            System.out.print("balanceOf call error " + JSONObject.toJSONString(baseResp));
            return null;
        }


    }


    /*
    功能说明:根据tokenId查找owner
    输入参数:
             _tokenId:uint256
    返回值:
             owner:identity
    */
    public String ownerOf() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(2);

        String orderId = "order_" + System.currentTimeMillis();
        CallRestBizParam callRestBizParam = CallRestBizParam.builder()
                .orderId(orderId)
                .bizid(restClientProperties.getBizid())
                .account("gusto_test1")
                .contractName(NFT_721_CONTRACT_NAME)
                .methodSignature("ownerOf(uint256)")
                .inputParamListStr(jsonArray.toJSONString())
                .outTypes("identity")
                .mykmsKeyId("Il726LGGKGPAQENO1624004353911")
                .method(Method.CALLCONTRACTBIZASYNC)
                .tenantid(restClientProperties.getTenantid())
                .gas(1000000L).build();
        BaseResp baseResp = restClient.chainCallForBiz(callRestBizParam);

        //System.out.println(JSONObject.toJSONString(baseResp));
        if (baseResp.getCode().compareToIgnoreCase("200") == 0) {
            Thread.sleep(3000);
            String hash = baseResp.getData();
            BaseResp queryBaseResp = restClient.chainCall(hash, restClientProperties.getBizid(), "", Method.QUERYRECEIPT);
            String s = queryBaseResp.getData();

            String output = s.substring(s.indexOf("output") + 9, s.indexOf("result")).replace("\"", "").replace(",", "");
            if (0 != output.length()) {
                byte[] content = Hex.encode(Base64.decode(output));
                CallRestBizParam _callRestBizParam = CallRestBizParam.builder().
                        bizid(restClientProperties.getBizid()).method(Method.PARSEOUTPUT).
                        tenantid(restClientProperties.getTenantid()).
                        orderId("order_" + System.currentTimeMillis()).
                        vmTypeEnum(VMTypeEnum.EVM).content(new String(content)).
                        abi("[\"identity\"]").                                      //TODO 合约返回值类型需自己根据合约修改
                        mykmsKeyId(restClientProperties.getKmsId()).build();    //TODO 默认为application.yaml中KmsId
                BaseResp _baseResp = restClient.chainCallForBiz(_callRestBizParam);
                if (_baseResp.isSuccess()) {
                    String data = _baseResp.getData();
                    JSONArray identities = JSONObject.parseArray(data);
                    System.out.println("ownerOf owner:" + identities.getString(0));
                    return identities.getString(0);
                } else {
                    System.out.print("ownerOf parse output error " + JSONObject.toJSONString(_baseResp));
                    return null;
                }
            } else {
                System.out.print("ownerOf query receipt error " + JSONObject.toJSONString(baseResp));
                return null;
            }
        } else {
            System.out.print("ownerOf call error " + JSONObject.toJSONString(baseResp));
            return null;
        }


    }


    /*
    功能说明:添加租赁人
    输入参数:
               _tokenId:uint256
               _to:identity
    事件:
               AddRent( _tokenID, _to);
   */
    public String addRent() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(BigInteger.valueOf(1));
        jsonArray.add(new Identity("0bdaeaefe144a0eee2e11cf1030d7607a9c53abaf4606db55d3053029dd6bda9"));

        String orderId = "order_" + System.currentTimeMillis();
        CallRestBizParam callRestBizParam = CallRestBizParam.builder()
                .orderId(orderId)
                .bizid(restClientProperties.getBizid())
                .account("gustoUser2")
                .contractName(RENT_CONTRACT_NAME)
                .methodSignature("addRent(uint256,identity)")
                .inputParamListStr(jsonArray.toJSONString())
                .outTypes("void")
                .mykmsKeyId("Z6pJGriuKGPAQENO1625040533402")
                .method(Method.CALLCONTRACTBIZASYNC)
                .tenantid(restClientProperties.getTenantid())
                .gas(1000000L).build();
        BaseResp baseResp = restClient.chainCallForBiz(callRestBizParam);

        System.out.println("addRent " + JSONObject.toJSONString(baseResp));
        if (baseResp.getCode().compareToIgnoreCase("200") == 0) {
            Thread.sleep(3000);
            String hash = baseResp.getData();
            BaseResp queryBaseResp = restClient.chainCall(hash, restClientProperties.getBizid(), "", Method.QUERYRECEIPT);
            String s = queryBaseResp.getData();

            if (queryBaseResp.getCode().compareToIgnoreCase("200") == 0) {
                ReceiptDecoration transaction = JSON.parseObject(queryBaseResp.getData(), ReceiptDecoration.class);

                for (LogEntry log : transaction.getLogs()) {

                    if (log.getLogData().length > 0) {

                        //传入回执中的logdata转换为EVMoutput
                        System.out.println("addRent query receipt successful " + JSONObject.toJSONString(baseResp));
                        EVMOutput logOutput = new EVMOutput(Hex.toHexString(log.getLogData()));
                        //根据事件传入类型按顺序传值,如event test(string a,uint256 b); 则填写asList("string","uint256")
                        List<Object> resultList = ContractParameterUtils.getEVMOutput(logOutput,
                                asList("uint256", "identity"));
                        for (Object o : resultList) {
                            System.out.println("addRent  param:" + o.toString());
                        }
                        break;
                    }
                }
            } else {
                System.out.println("addRent query receipt error " + JSONObject.toJSONString(queryBaseResp));
                if (queryBaseResp.getCode().compareToIgnoreCase("10201") == 0) {
                    System.out.println("addRent  alread   " + JSONObject.toJSONString(queryBaseResp));
                }
            }
            return null;
        } else {
            System.out.println("addRent call error " + JSONObject.toJSONString(baseResp));
            return null;
        }

    }


    /*
    功能说明:添加租赁人
    输入参数:
               _tokenId:uint256
               _to:identity
    事件:
               RemoveRent( _tokenID, _to);
   */
    public String removeRent() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(BigInteger.valueOf(1));
        jsonArray.add(new Identity("0bdaeaefe144a0eee2e11cf1030d7607a9c53abaf4606db55d3053029dd6bda9"));

        String orderId = "order_" + System.currentTimeMillis();
        CallRestBizParam callRestBizParam = CallRestBizParam.builder()
                .orderId(orderId)
                .bizid(restClientProperties.getBizid())
                .account("gustoUser2")
                .contractName(RENT_CONTRACT_NAME)
                .methodSignature("removeRent(uint256,identity)")
                .inputParamListStr(jsonArray.toJSONString())
                .outTypes("void")
                .mykmsKeyId("Z6pJGriuKGPAQENO1625040533402")
                .method(Method.CALLCONTRACTBIZASYNC)
                .tenantid(restClientProperties.getTenantid())
                .gas(1000000L).build();
        BaseResp baseResp = restClient.chainCallForBiz(callRestBizParam);

        System.out.println("removeRent " + JSONObject.toJSONString(baseResp));
        if (baseResp.getCode().compareToIgnoreCase("200") == 0) {
            Thread.sleep(3000);
            String hash = baseResp.getData();
            BaseResp queryBaseResp = restClient.chainCall(hash, restClientProperties.getBizid(), "", Method.QUERYRECEIPT);
            String s = queryBaseResp.getData();

            if (queryBaseResp.getCode().compareToIgnoreCase("200") == 0) {
                ReceiptDecoration transaction = JSON.parseObject(queryBaseResp.getData(), ReceiptDecoration.class);

                for (LogEntry log : transaction.getLogs()) {

                    if (log.getLogData().length > 0) {

                        //传入回执中的logdata转换为EVMoutput
                        System.out.println("removeRent query receipt successful " + JSONObject.toJSONString(baseResp));
                        EVMOutput logOutput = new EVMOutput(Hex.toHexString(log.getLogData()));
                        //根据事件传入类型按顺序传值,如event test(string a,uint256 b); 则填写asList("string","uint256")
                        List<Object> resultList = ContractParameterUtils.getEVMOutput(logOutput,
                                asList("uint256", "identity"));
                        for (Object o : resultList) {
                            System.out.println("removeRent  param:" + o.toString());
                        }
                        break;
                    }
                }
            } else {
                System.out.println("removeRent query receipt error " + JSONObject.toJSONString(queryBaseResp));
                if (queryBaseResp.getCode().compareToIgnoreCase("10201") == 0) {
                    System.out.println("removeRent  alread   " + JSONObject.toJSONString(queryBaseResp));
                }
            }
            return null;
        } else {
            System.out.println("removeRent call error " + JSONObject.toJSONString(baseResp));
            return null;
        }

    }



}