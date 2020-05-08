/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		/**
		 * 用来存放 BeanDefinitionRegistryPostProcessors 的全类名
		 */
		Set<String> processedBeans = new HashSet<>();

		/**
		 *  beanFactory 是 DefaultListableBeanFactory 是 BeanDefinitionRegistry 实现类
		 *  把所有的之后处理器一分为二：BeanDefinitionRegistryPostProcessor 和 BeanFactoryPostProcessor
		 *  1、执行所有的	BeanDefinitionRegistryPostProcessor 的 postProcessBeanDefinitionRegistry
		 *  2、执行所有的	BeanDefinitionRegistryPostProcessor 的 父类 BeanFactoryPostProcessor 的 postProcessBeanFactory 方法
		 *  3、执行所有 BeanFactoryPostProcessor 的 postProcessBeanFactory
		 *
		 */
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			/**
			 *  用来存放 BeanFactoryPostProcessor
			 */
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			/**
			 *  用来存放 BeanDefinitionRegistryPostProcessor
			 *  	BeanDefinitionRegistryPostProcessor 扩展了 BeanFactoryPostProcessor
			 */
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			/**
			 *  遍历 beanFactoryPostProcessors 存放进不同的List
			 */
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				}
				else {
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			/**
			 * 用来存放 beanFactory 中 BeanDefinitionRegistryPostProcessor 临时变量
			 */
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			/**
			 *  拿到Spring内置的 BeanDefinitionRegistryPostProcessor
			 *  	org.springframework.context.annotation.internalConfigurationAnnotationProcessor ：ConfigurationClassPostProcessor
			 *  	这是在前面 this() 方法中 初始化读取器中被初始化放入 beanFactory 中的
			 *  我们自己自定义的 BeanDefinitionRegistryPostProcessor 就算加了 @Component 注解 依旧是拿不到的
			 *  这里Spring 还没有完成 bean 的扫描
			 */
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);

			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					/**
					 * 把 ConfigurationClassPostProcessor 放进 currentRegistryProcessors
					 * 		ConfigurationClassPostProcessor 实现了 BeanDefinitionRegistryPostProcessor
					 * 		BeanDefinitionRegistryPostProcessor 扩展了 BeanFactoryPostProcessor
					 * 	ConfigurationClassPostProcessor 一般被用来扫描 Bean, Import, ImportSource
					 * 		处理配置类
					 */
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					/**
					 * 	把name 放进 processedBeans 后面会根据这个集合判断时候执行过
					 */
					processedBeans.add(ppName);
				}
			}
			/**
			 * 排序
			 */
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			/**
			 * 合并 等后面统一处理
			 */
			registryProcessors.addAll(currentRegistryProcessors);
			/**
			 * 遍历执行beanFactory 中的 BeanDefinitionRegistryPostProcessor
			 * 		这里可以理解为 执行 ConfigurationClassPostProcessor # processConfigBeanDefinitions 方法
			 * 		ConfigurationClassPostProcessor ： Spring 中最重要的后置处理器
			 * 		在这个方法里面 参与BeanFactory的建造，在这个类中，会解析
			 * 			@Configuration的配置类
			 * 			@ComponentScan、@ComponentScans注解扫描的包
			 * 			@Import等注解
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			/**
			 * 清除 临时变量
			 */
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			/**
			 * 再次获取 beanFactory 中 BeanDefinitionRegistryPostProcessor
			 * 		这个时候我们就可以获取到我们自定义的 BeanDefinitionRegistryPostProcessor
			 * 		因为上面在执行	ConfigurationClassPostProcessor # postProcessBeanDefinitionRegistry 方法时候
			 * 		会把我们配置的	BeanDefinitionRegistryPostProcessor 加载到BeanFactory 中
			 *
			 */
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				/**
				 * 判断 processedBeans 里面有没有 ppName 主要用来判断 有没有被执行过
				 * 	实现 Ordered 接口
				 */
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					/**
					 * 没有被执行过就放到 currentRegistryProcessors 等待执行
					 */
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			/**
			 * 排序
			 */
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			/**
			 * 合并 等后面统一处理
			 */
			registryProcessors.addAll(currentRegistryProcessors);
			/**
			 * 	执行我们自己定义的 BeanDefinitionRegistryPostProcessor
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			/**
			 * 清除临时变量
			 */
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			/**
			 *	主要执行 自定义的 BeanDefinitionRegistryPostProcessor
			 *		上面是实现 Ordered 接口 的  这里是没有实现 Ordered 接口的
			 */
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}


			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			/**
			 * 统一处理：执行父类方法 postProcessBeanFactory
			 * 		BeanDefinitionRegistryPostProcessor 继承 BeanFactoryPostProcessor
			 */
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			/**
			 * 执行入参中的 BeanFactoryPostProcessor 中的 postProcessBeanFactory 方法
			 */
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		/**
		 * 获取所有的 BeanFactoryPostProcessor
		 */
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		/**
		 *  存放实现 PriorityOrdered 接口的
		 */
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		/**
		 *  存放实现 Ordered 接口的
		 */
		List<String> orderedPostProcessorNames = new ArrayList<>();
		/**
		 *  存放除上面以外的
		 */
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		/**
		 * 先执行 实现 PriorityOrdered 接口的 BeanFactoryPostProcessor # postProcessBeanFactory() 方法
		 * 再按顺序执行 实现 Ordered 接口的 BeanFactoryPostProcessor # postProcessBeanFactory() 方法
		 * 最后执行其他的  BeanFactoryPostProcessor # postProcessBeanFactory() 方法
		 */
		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
		/**
		 * 	从bdMap 中找到 BeanPostProcessor
		 *	在this() 里面 初始化读取器的时候注册了5个bd，其中一个是 ConfigurationClassPostProcessor（BeanFactoryPostProcessor类型）
		 *  	还有两个 BeanPostProcessor 类型的bd  AutowiredAnnotationBeanPostProcessor  CommonAnnotationBeanPostProcessor
		 */
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		/**
		 *  3+1+2 = 6
		 * beanFactory.getBeanPostProcessorCount() 有3个
		 * 		refresh() -> prepareBeanFactory() -> beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		 * 		refresh() -> prepareBeanFactory() -> beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));
		 * 		refresh() -> invokeBeanFactoryPostProcessors() 执行ConfigurationClassPostProcessor 的父类方法 postProcessBeanFactory() 中
		 * 				 	beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
		 */
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		/**
		 * 注册BeanPostProcessor
		 * 		本质就是：beanFactory 里面 beanPostProcessors 集合里面 add 一个 beanPostProcessor
		 */
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		/**
		 * 注册一个beanPostProcessor
		 * 		到此为止一共有6个 BeanPostProcessor
		 * 			AutowiredAnnotationBeanPostProcessor  处理 byType byName
		 * 			CommonAnnotationBeanPostProcessor  处理 @PostConstruct
		 * 			ApplicationContextAwareProcessor  拿到上下文
		 * 			ApplicationListenerDetector
		 * 			ImportAwareBeanPostProcessor  拿到类上的注解
		 * 			BeanPostProcessorChecker  检查有没有执行 BeanPostProcessor
		 */
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
