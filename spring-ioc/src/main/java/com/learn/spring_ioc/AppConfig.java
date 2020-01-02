package com.learn.spring_ioc;

import com.learn.spring_ioc.dao.IndexDao;
import com.learn.spring_ioc.dao.IndexDao1;
import com.learn.spring_ioc.dao.IndexDao2;
import com.learn.spring_ioc.dao.MyImportSelector;
import com.learn.spring_ioc.entity.Student;
import com.learn.spring_ioc.entity.Teacher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

@Configuration
@ComponentScan("com.learn.spring_ioc")
@Import({MyImportSelector.class, IndexDao1.class, IndexDao2.class})
public class AppConfig {

	@Autowired
	private TestConfig testConfig;

	@Bean
	public Student getStudent(){
		Student student = new Student();
		return student;
	}

	@Bean
	public Teacher get(){
		Student student = getStudent();
		return new Teacher();
	}

}
