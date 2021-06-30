# Openchain Demo For Java



## nft demo请看

NftContractFlow.java 文件



## 1. 背景
开放联盟链旨在打造为低成本、低门槛开放普惠的，生态最繁荣的区块链服务网络。为了方便Java开发者
接入并使用开放联盟链，我们提供该示例工程。本示例工程只支持使用用户创建的非托管链上账户
(非托管账户即通过开放联盟链页面创建的自己保存私钥、密码的账户，而非从标示（密码托管）处创建的链上账户)。               

## 2. 工程简介
OpenchainDemoApplication，启动后会自动执行DemoFlowRunner，DemoFlowRunner会执行我们定义好的链操作示例流程。

DepositFlow是存证场景下的示例操作，包括如下流程：
1. 向开放联盟链存入数据
2. 查询存入的数据

ContractFlow是合约相关场景的示例操作，resource/contract.txt中保存了一个Solidity积分合约和字节码，可以在Cloud IDE
编译合约得到字节码。如果要部署新合约，需要将Cloud IDE中调试好的合约字节码放到contract.txt中，示例包括如下流程：
1. 部署Solidity合约
2. 调用Solidity合约


## 3. 配置说明
开发者运行该示例工程时，需要对配置文件进行相应的更新，配置文件中属性的详见含义如下：
```java
bizid: 开放联盟链的链ID
cipher-suit: 加密算法，ec表示采用椭圆曲线数字签名算法
rest-url: 访问开放联盟链的地址
access-id: *开发者需要替换* 替换为准备环境步骤申请AccessKey时返回的Access-id
access-secret: *开发者需要替换* 将access.key文件内容替换为准备环境步骤申请AccessKey时返回的密钥
default-account: *开发者需要替换* 创建账户时的账户名
default-account-key: *开发者需要替换* 创建账户时返回的私钥
default-account-pwd: *开发者需要替换* 创建账户时的密码
read-file-from-ext: 表示读取配置文件的路径，默认为false，表示在resource目录下读取
```

## 4. 快速开始
更新相应配置，然后运行OpenchainDemoApplication。

## 5. 文档
如下是一个简单的合约实例，contract.txt是对应的字节码。这是蚂蚁区块链合约平台对积分管理方案的简单实现，主要实现了积分的发放、转账和查询三个方法。
```solidity
pragma solidity ^0.4.0;
contract CreditManager {
    int256 creditLimit = 1000000000;   // the issue limit
    int256 issueAmount = 0;           // the issue total amount
    identity admin;                    // the administrator in contract
    mapping(identity=>int256) credit;
    
    event IssueEvent(identity indexed to, int256 indexed value);
    event TransferEvent(identity indexed from, identity indexed to, int256 indexed value);
    
    function CreditManager() {
        admin = msg.sender;
    }
    
    // modifier
    modifier onlyAdmin() {
        require(msg.sender == admin,"Permission denied");
        _;
    }
    
    // issue credit and only admin can 
    function Issue(identity account, int256 value) public onlyAdmin returns(bool) {
        // the value should bigger than 0, and issueAmount add value should small than issueAmount
        require(issueAmount + value <= creditLimit && issueAmount + value > issueAmount && value > 0, "Invalid value!");
        credit[account] += value;
        issueAmount += value;
        IssueEvent(account,value);
        return true;
    }
    
    function Transfer(identity account,int256 value) public returns(bool) {
        require(credit[msg.sender] >= value, "balance not enough!");
        require(value > 0 && value <= creditLimit, "Invalid value!");
        credit[msg.sender] -= value;
        credit[account] += value;
        TransferEvent(msg.sender,account,value);
        return true;
    }
    
    function Query(identity account) public returns(int256) {
        return credit[account];
    }
}
```
