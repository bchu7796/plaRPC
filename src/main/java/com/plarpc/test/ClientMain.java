package com.plarpc.test;

import com.plarpc.client.PlaRpcHandlerClientApi;
import com.plarpc.client.PlaRpcHandlerClientImpl;

import java.io.IOException;

public class ClientMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        PlaRpcHandlerClientApi<Test> test = new PlaRpcHandlerClientImpl(Test.class);
        test.rpc().hello();
        test.rpc().helloName("Brian");
        test.rpc().square(10);
        test.rpc().sum(10, 10);
    }
}