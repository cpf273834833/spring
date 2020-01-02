package com.learn.spring_ioc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class MyInvocationHandler implements InvocationHandler {
	private BaseMapper h;

	public MyInvocationHandler(BaseMapper h){
		this.h = h;
	}
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("before");
		method.invoke(h,args);
		System.out.println("after");
		return null;
	}
}
