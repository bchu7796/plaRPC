package com.plarpc.client;

import com.google.protobuf.ByteString;
import com.plarpc.grpchandler.GrpcHandlerClient;
import com.plarpc.serialization.SerializationToolApi;
import com.plarpc.serialization.SerializationToolImpl;
import com.plarpc.servicemapper.ServiceMapperApi;
import com.plarpc.servicemapper.ServiceMapperImpl;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PlaRpcHandlerClientImpl<T> implements PlaRpcHandlerClientApi, InvocationHandler{
    private static final Logger logger = Logger.getLogger(PlaRpcHandlerClientImpl.class.getName());
    private static final ServiceMapperApi serviceMapper = new ServiceMapperImpl();
    private Constructor<?> proxyConstructor;
    private String className;

    /**
     * Initialize the proxy constructor with the class we wish to send the RPC to.
     *
     * @param clazz Class we wish to send the RPC to.
     *
     * @throws RuntimeException
     */
    public PlaRpcHandlerClientImpl(Class<T> clazz) {
        this.className = clazz.getName();
        try {
            this.proxyConstructor = Proxy.getProxyClass(clazz.getClassLoader(),
                    new Class[] { clazz }).getConstructor(InvocationHandler.class);
        } catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String targetClassName = this.className;
        String address = serviceMapper.getLocationByName(targetClassName);
        logger.info(targetClassName + "::" + method.getName() +
                "() RPC Invoked  Address: " + address);

        String[] splitAddress = address.split(":");
        String host = splitAddress[0];
        int port = Integer.valueOf(splitAddress[1]);

        List<ByteString> serializedObjects = new ArrayList<>();
        if(args != null) {
            SerializationToolApi serializationTool = new SerializationToolImpl();
            for(int i = 0; i < args.length; i++) {
                serializedObjects.add(serializationTool.toString(args[i]));
            }
        }

        GrpcHandlerClient grpcClient = new GrpcHandlerClient(host, port);
        Object returnObject = null;
        try {
            returnObject = grpcClient.callMethod(method.getName(), serializedObjects);
        } finally {
            grpcClient.shutdown();
        }

        return returnObject;
    }


    /**
     * Build a proxy for the class we wish to RPC to.
     *
     * @return a proxy instance
     *
     * @throws RuntimeException
     */
    @Override
    public T rpc() {
        try{
            return (T) proxyConstructor.newInstance(new Object[] {this});
        } catch (InstantiationException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}