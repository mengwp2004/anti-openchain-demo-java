package com.alipay.openchain;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Security;

@SpringBootApplication
public class OpenchainDemoApplication {
    public static void main(String[] args){
        try {
            Security.addProvider(new BouncyCastleProvider());
            SpringApplication springApplication = new SpringApplication(OpenchainDemoApplication.class);
            springApplication.run(args);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
