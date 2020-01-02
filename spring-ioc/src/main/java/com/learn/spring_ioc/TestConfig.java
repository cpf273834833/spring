package com.learn.spring_ioc;


import com.learn.spring_ioc.entity.Student;
import com.learn.spring_ioc.entity.Teacher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class TestConfig {


	public static void main(String[] args) {
		List<Integer> list = new ArrayList<>();
		list.add(2);
		list.add(7);
		list.add(0);
		list.sort((o1,o2)->{
			return Integer.compare(o1,o2);
		});
		System.out.println(list.toString());
	}
}
