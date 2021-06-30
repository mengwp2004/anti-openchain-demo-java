/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alipay.openchain.flow;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.domain.account.Identity;
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
import java.util.ArrayList;
import java.util.UUID;

/**
 * 合约相关场景下操作流，示例代码提供
 */
@Service
public class NftContractFlow {
    //private static final String CONTRACT_NAME = "gusto_bluesky_assert2"; // mengwp_2012账号　
    private static final String CONTRACT_NAME = "gusto_nft_test1"; // mengwp_2012账号　

    private static final String COMMODITY_CONTRACT_NAME = "commodity10";
    private static final String SUPPER_MARKET_CONTRACT_NAME = "supper16";
    private static final String AUTHOR_CONTRACT_NAME = "Author2";
    private static final String BRAND_CONTRACT_NAME = "Brand2";
    private static final String NFT_721_CONTRACT_NAME = "SNFT11";

    @Autowired
    private RestClient restClient;

    @Autowired
    private RestClientProperties restClientProperties;

    public void runFlow() throws Exception {

        //新建作者
        //String csHash = registeredAuthor();

        //新建品牌
        String csHash = registeredBrand();
        //铸造
        //String csHash = createCommodity();

        //挂售
        //String csHash = createOrder();

        //购买
        //String csHash = buyOneNoFee();

        //查询指定账户的nft数目
        //String csHash = balanceOf();

        //根据token Id查询owner
        //String csHash = ownerOf();


        queryResult(csHash);
        //解析合约返回值
        showOutPut(csHash);
    }

    //部署Solidity合约 也可以通过Cloud Ide进行部署
    public String deploySolidityContract() throws Exception {
        String path = NftContractFlow.class.getClassLoader().getResource("contract.txt").getPath();
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

    /*
    功能说明:新建作者
    输入参数:
               _author:identity
               _url:string
               _name:string
               _introduction:string
    事件:

   */
    public String registeredAuthor() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(new Identity("4b61cd266a5d6ce40ddec2e43ef75c1aadc5cedb113da980802ebec4532d1e5f"));
        jsonArray.add("url");
        jsonArray.add("gusto user2");
        jsonArray.add("gusto user2 desc ");
        ClientParam clientParam = restClient.createCallContractTransaction(
                restClientProperties.getDefaultAccount(),
                AUTHOR_CONTRACT_NAME,                          //合约名  可以修改为cloud ide中部署的合约名
                "",                             //合约返回值类型
                "registeredAuthor(identity,string,string,string)",
                jsonArray.toJSONString(),
                false,
                null,
                1000000L);
        BaseResp resp = restClient.chainCall(clientParam.getHash(), clientParam.getSignData(), Method.CALLCONTRACT);
        System.out.println("[registeredAuthor CallContract-调用合约] Hash: " + clientParam.getHash() + "    Result:" + resp);
        return clientParam.getHash();
    }

    /*
      功能说明:新建品牌
      输入参数:
               _bID:uint256
               _description:string
               _feeToken:identity   //要求是代币合约地址或1,1 代币使用链的币
               _fee:uint256
               _name:string

      事件:
               RegisteredBrand(_bID,_description,_feeToken,_fee,_name);


    */
    public String registeredBrand() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(BigInteger.valueOf(3));
        jsonArray.add("brand 3 desc");
        jsonArray.add(new Identity("0xe8639b12b0f2f59fe4bb52380abd7f6d578fba27035cc7f628f6de99838c3a3e"));
        jsonArray.add(BigInteger.valueOf(5));

        jsonArray.add("brand 3 ");
        ClientParam clientParam = restClient.createCallContractTransaction(
                restClientProperties.getDefaultAccount(),
                BRAND_CONTRACT_NAME,                          //合约名  可以修改为cloud ide中部署的合约名
                "",                             //合约返回值类型
                "registeredBrand(uint256,string,identity,uint256,string)",
                jsonArray.toJSONString(),
                false,
                null,
                1000000L);
        BaseResp resp = restClient.chainCall(clientParam.getHash(), clientParam.getSignData(), Method.CALLCONTRACT);
        System.out.println("[registeredAuthor CallContract-调用合约] Hash: " + clientParam.getHash() + "    Result:" + resp);
        return clientParam.getHash();
    }

    //调用Solidity合约
    //合约方法签名  注意：1.中间不要带空格  2.只写参数类型,不要带参数名 如:uint256 a,uint256 b  3.参数类型填写争取  请不要填写如int  需要填写完整uint
    /*
      功能说明:铸造
      输入参数:
                cid:uint256
                _bId:uint256
                _author:identity
                _name:string
                _pDescription:string
                _url:string
                _cAttributeS:string[]
                _cAttributeU:uint256[]
                _totalSupply:uint256
                _authorFeeRate:uint256
      事件:

            1  CreateCommodity(
            _cid,
            _bID,
            _name,
            _author,
            _pDescription,
            _url,
            _cAttributeS,
            _cAttributeU,
            _totalSupply,
            _authorFeeRate
            );

            2  CommidityStateChange(_cid,commodity[_cid].cState);

    */

    public String createCommodity() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(BigInteger.valueOf(4));
        jsonArray.add(BigInteger.valueOf(2));
        jsonArray.add(new Identity("be8785e54d8c4c6f265ab7628099491c7deaf6c8d80459f3ad11881aa92bea46"));
        jsonArray.add("music 1");
        jsonArray.add("music desc");
        jsonArray.add("https://");
        jsonArray.add(""); // 只能空，蚂蚁链合约限制
        jsonArray.add(""); // 只能空，蚂蚁链合约限制
        ArrayList<BigInteger> attrU = new ArrayList<BigInteger>();
        attrU.add(BigInteger.valueOf(1));
        jsonArray.add(attrU);
        jsonArray.add(1000);
        jsonArray.add(0);

        ClientParam clientParam = restClient.createCallContractTransaction(
                restClientProperties.getDefaultAccount(),
                COMMODITY_CONTRACT_NAME,                          //合约名  可以修改为cloud ide中部署的合约名
                "",                             //合约返回值类型
                "createCommodity(uint256,uint256,identity,string,string,string,string,string,uint256[],uint256,uint256)",
                jsonArray.toJSONString(),
                false,
                null,
                1000000L);
        BaseResp resp = restClient.chainCall(clientParam.getHash(), clientParam.getSignData(), Method.CALLCONTRACT);
        System.out.println("[createCommodity CallContract-调用合约] Hash: " + clientParam.getHash() + "    Result:" + resp);
        return clientParam.getHash();
    }

    /*
     功能说明:商家挂售
     输入参数:
               _cid:uint256
               _startTime:uint256
               _price:uint256
               _reciveToken:identity
     事件:
               CreateOrder(_cid,_startTime,_price,_reciveToken);
   */
    public String createOrder() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(BigInteger.valueOf(4));
        jsonArray.add(BigInteger.valueOf(100));
        jsonArray.add(BigInteger.valueOf(20));
        jsonArray.add(new Identity("0xe8639b12b0f2f59fe4bb52380abd7f6d578fba27035cc7f628f6de99838c3a3e")); //erc20 contract address

        ClientParam clientParam = restClient.createCallContractTransaction(
                restClientProperties.getDefaultAccount(),
                SUPPER_MARKET_CONTRACT_NAME,                          //合约名  可以修改为cloud ide中部署的合约名
                "",                             //合约返回值类型
                "createOrder(uint256,uint256,uint256,identity)",
                jsonArray.toJSONString(),
                false,
                null,
                1000000L);
        BaseResp resp = restClient.chainCall(clientParam.getHash(), clientParam.getSignData(), Method.CALLCONTRACT);
        System.out.println("[createOrder CallContract-调用合约] Hash: " + clientParam.getHash() + "    Result:" + resp);
        return clientParam.getHash();
    }

    /*
    功能说明:客户买入
    输入参数:
               _cid:uint256
               _sid:uint256
    事件:
               BuyOne( _cid, _sid, _tokenID, msg.sender);
   */
    public String buyOneNoFee() throws Exception {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(BigInteger.valueOf(4));
        jsonArray.add(BigInteger.valueOf(2));

        ClientParam clientParam = restClient.createCallContractTransaction(
                restClientProperties.getDefaultAccount(),
                SUPPER_MARKET_CONTRACT_NAME,                          //合约名  可以修改为cloud ide中部署的合约名
                "",                             //合约返回值类型
                "buyOneNoFee(uint256,uint256)",
                jsonArray.toJSONString(),
                false,
                null,
                1000000L);
        BaseResp resp = restClient.chainCall(clientParam.getHash(), clientParam.getSignData(), Method.CALLCONTRACT);
        System.out.println("[buyOneNoFee CallContract-调用合约] Hash: " + clientParam.getHash() + "    Result:" + resp);
        return clientParam.getHash();
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

        ClientParam clientParam = restClient.createCallContractTransaction(
                restClientProperties.getDefaultAccount(),
                NFT_721_CONTRACT_NAME,                          //合约名  可以修改为cloud ide中部署的合约名
                "uint256",                             //合约返回值类型
                "balanceOf(identity)",
                jsonArray.toJSONString(),
                false,
                null,
                100000L);
        BaseResp resp = restClient.chainCall(clientParam.getHash(), clientParam.getSignData(), Method.CALLCONTRACT);
        System.out.println("[balanceOf CallContract-调用合约] Hash: " + clientParam.getHash() + "    Result:" + resp);

        return clientParam.getHash();
    }

    /*

    */
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

        ClientParam clientParam = restClient.createCallContractTransaction(
                restClientProperties.getDefaultAccount(),
                NFT_721_CONTRACT_NAME,                          //合约名  可以修改为cloud ide中部署的合约名
                "identity",                             //合约返回值类型
                "ownerOf(uint256)",
                jsonArray.toJSONString(),
                false,
                null,
                100000L);
        BaseResp resp = restClient.chainCall(clientParam.getHash(), clientParam.getSignData(), Method.CALLCONTRACT);
        System.out.println("[ownerOf CallContract-调用合约] Hash: " + clientParam.getHash() + "    Result:" + resp);

        showOutPut(clientParam.getHash());
        return clientParam.getHash();
    }

}