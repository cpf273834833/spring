package com.learn.spring_ioc;

import com.learn.spring_ioc.dao.IndexDao;
import com.learn.spring_ioc.dao.IndexDao1;
import com.learn.spring_ioc.dao.IndexDao2;
import com.learn.spring_ioc.dao.MyImportSelector;

import org.springframework.context.annotation.*;

@Configuration
@ComponentScan("com.learn.spring_ioc")
@Import({MyImportSelector.class, IndexDao1.class, IndexDao2.class})
public class AppConfig {


}
