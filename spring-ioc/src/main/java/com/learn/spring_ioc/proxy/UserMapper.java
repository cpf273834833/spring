package com.learn.spring_ioc.proxy;

public class UserMapper implements BaseMapper {
	@Override
	public void logic() {
		System.out.println("logic");
	}
}
