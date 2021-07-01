/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package com.alipay.openchain;

import com.alipay.openchain.flow.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 接入开放联盟链和常见操作示例
 */
@Component
public class DemoFlowRunner implements CommandLineRunner {
    @Autowired
    DepositFlow depositFlow;

    @Autowired
    NftContractFlow nftContractFlow;

    @Autowired
    NftContractFlowASync nftContractFlowASync;

    @Autowired
    DepositFlowforkms depositFlowforkms;

    @Autowired
    ContractFlow contractFlow;

    @Autowired
    ContractFlowforkms contractFlowforkms;

    @Autowired
    OperateChainAccount operateChainAccount;

    @Autowired
    QueryChainData queryChainData;
    public void run(String... strings) throws Exception {

        /*
        nft同步测试
         */
        //nftContractFlow.runFlow();

        /*
        nft异步测试
         */
        nftContractFlowASync.runFlow();

        /**
         * 执行存证场景下操作流（密钥非托管账户）
         */
        //depositFlow.runFlow();
        /**
         * 执行合约相关场景下操作流（密钥非托管账户）
         */
       //contractFlow.runFlow();
        /**
         * 执行存证场景下操作流（密钥托管账户）
         */
//        DepositFlowforkms.runFlow();
        /**
         * 执行合约相关场景下操作流（密钥托管账户）
         */
        //ContractFlowforkms.runFlow();

        /**
         * 执行创建账户场景下操作流
         */
//        operateChainAccount.runFlow();

        /**
         * 执行查询区块场景下操作流
         */
        //queryChainData.runFlow();
    }
}