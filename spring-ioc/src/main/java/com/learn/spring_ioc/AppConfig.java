package com.learn.spring_ioc;

import com.learn.spring_ioc.entity.Student;
import com.learn.spring_ioc.entity.Teacher;
import org.springframework.context.annotation.*;

@Configuration
@ComponentScan("com.learn.spring_ioc")
public class AppConfig {

	@Bean
	public Student getStudent(){
		return new Student();
	}

	@Bean
	public Teacher getTeacher(){
		getStudent();
		return new Teacher();
	}

}
