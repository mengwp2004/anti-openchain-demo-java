package com.alipay.openchain.flow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.antfinancial.mychain.baas.tool.restclient.RestClient;
import com.antfinancial.mychain.baas.tool.restclient.RestClientProperties;
import com.antfinancial.mychain.baas.tool.restclient.model.CallRestParam;
import com.antfinancial.mychain.baas.tool.restclient.model.Method;
import com.antfinancial.mychain.baas.tool.restclient.model.ReceiptDecoration;
import com.antfinancial.mychain.baas.tool.restclient.response.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * block chain data query operations
 */
@Service
public class QueryChainData {
    @Autowired
    private RestClient restClient;

    @Autowired
    private RestClientProperties restClientProperties;

    public void runFlow () throws Exception {
        queryLastBlock();
        queryBlockHead();
        queryBlockBody();
        queryTransaction();
        queryTransReceipt();
    }

    /**
     * 查询最近区块
     */
    public void queryLastBlock() throws Exception {
        CallRestParam param = CallRestParam.builder().
                // 查询最近区块方法名称，固定值
                method(Method.QUERYLASTBLOCK).
                build();
        BaseResp resp = restClient.chainCall(param);
        if(resp.isSuccess()) {
            // 解析返回结果
            ReplyBlock replyBlock = JSON.parseObject(resp.getData(), ReplyBlock.class);
            BlockHeader header = replyBlock.getBlock().getBlockHeader();
            System.out.println("查询最近区块成功， 区块信息：高度 " + header.getNumber() + ", 哈希 " + header.getHash());
        } else {
            System.err.println("查询最近区块失败: " + resp.getCode() + ", " + resp.getData());
        }
    }

    /**
     * 查询指定区块头
     */
    public void queryBlockHead() throws Exception {
        CallRestParam param = CallRestParam.builder().
                // 查询最近区块头方法名称，固定值
                method(Method.QUERYBLOCK).
                // 查询的区块高度
                requestStr(String.valueOf(100L)).
                build();
        BaseResp resp = restClient.chainCall(param);
        if(resp.isSuccess()) {
            // 解析返回结果
            ReplyBlock replyBlock = JSON.parseObject(resp.getData(), ReplyBlock.class);
            BlockHeader header = replyBlock.getBlock().getBlockHeader();
            System.out.println("查询区块头成功， 区块信息：高度 " + header.getNumber() + ", 哈希 " + header.getHash());
        } else {
            System.err.println("查询区块头失败: " + resp.getCode() + ", " + resp.getData());
        }
    }

    /**
     * 查询指定区块内容
     */
    public void queryBlockBody() throws Exception {
        CallRestParam param = CallRestParam.builder().
                // 查询最近区块内容方法名称，固定值
                method(Method.QUERYBLOCKBODY).
                // 查询的区块高度
                requestStr(String.valueOf(100L)).
                // 如果查询配置文件中指定默认链外的其他链, 设置对应的`bizid`
                // bizid("").
                build();
        BaseResp resp = restClient.chainCall(param);
        if(resp.isSuccess()) {
            // 解析返回结果
            JSONObject jsonObject = JSONObject.parseObject(resp.getData());
            com.alipay.mychain.sdk.domain.block.Block block = new com.alipay.mychain.sdk.domain.block.Block();
            block.fromJson(jsonObject);
            com.alipay.mychain.sdk.domain.block.BlockHeader header = block.getBlockHeader();
            System.out.println("查询区块内容成功， 区块信息：高度 " + header.getNumber() + ", 哈希 " + header.getHash());
        } else {
            System.err.println("查询区块内容失败: " + resp.getCode() + ", " + resp.getData());
        }
    }

    /**
     * 查询指定交易
     */
    public void queryTransaction() throws Exception {
        final String txHash = "cd3b5e4db44d121024a645c93b27ba3e49ca87d6dcc94ffb35e96faf1c1840a8";
        CallRestParam param = CallRestParam.builder().
                // 查询交易的方法名称，固定值
                method(Method.QUERYTRANSACTION).
                // 查询的交易Hash
                hash(txHash).
                // 如果查询配置文件中指定默认链外的其他链, 设置对应的`bizid`
                // bizid("").
                build();
        BaseResp resp = restClient.chainCall(param);
        if(resp.isSuccess()) {
            // 解析返回结果
            ReplyTransaction transaction = JSON.parseObject(resp.getData(), ReplyTransaction.class);
            TransactionDO transactionDO = transaction.getTransactionDO();
            System.out.println("查询交易成功， 区块信息：区块高度 " + transaction.getBlockNumber() + ", 交易哈希 " + transaction.getHash() + ", 交易时间 " + transactionDO.getTimestamp());
        } else {
            System.err.println("查询交易失败: " + resp.getCode() + ", " + resp.getData());
        }
    }

    /**
     * 查询指定交易回执
     */
    public void queryTransReceipt() throws Exception {
        final String txHash = "cd3b5e4db44d121024a645c93b27ba3e49ca87d6dcc94ffb35e96faf1c1840a8";

        CallRestParam param = CallRestParam.builder().
                // 查询交易回执的方法名称，固定值
                method(Method.QUERYRECEIPT).
                // 查询的交易Hash
                hash(txHash).
                // 如果查询配置文件中指定默认链外的其他链, 设置对应的`bizid`
                // bizid("").
                build();
        BaseResp resp = restClient.chainCall(param);
        if(resp.isSuccess()) {
            // 解析返回结果
            ReceiptDecoration receipt = JSON.parseObject(resp.getData(), ReceiptDecoration.class);
            System.out.println("查询交易回执成功， 区块信息：区块高度 " + receipt.getBlockNumber() + ", 消耗燃料 " + receipt.getGasUsed() + ", 交易结果 " + receipt.getResult());
        } else {
            System.err.println("查询交易回执失败: " + resp.getCode() + ", " + resp.getData());
        }
    }
}
