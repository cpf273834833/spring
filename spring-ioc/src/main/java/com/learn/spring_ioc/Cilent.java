package com.learn.spring_ioc;

import com.learn.spring_ioc.entity.Student;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Cilent {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
	}
}
