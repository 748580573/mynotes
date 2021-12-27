# SpringBoot之EnableAutoConfiguration注解源码解析

代码调用栈：

````java
InitApplication.main
	|->SpringApplication.run(InitApplication.class,args)
		|->return SpringApplication.run(primarySources=[InitApplication.class], args=[]);
			|->return new SpringApplication(primarySources=[InitApplication.class]).run(args=[]);
				|->SpringApplication.refreshContext(context=ref);
					|->SpringApplication.refresh(context=ref)
						|->ServletWebServerApplicationContext.refresh();
							|->AbstractApplicationContext.invokeBeanFactoryPostProcessors(beanFactory);
								|->PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
									|->invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
										|->postProcessor.postProcessBeanDefinitionRegistry(registry); //postProcessor=org.springframework.context.annotation.ConfigurationClassPostProcessor
											|->ConfigurationClassPostProcessor.processConfigBeanDefinitions(registry);
												|->ConfigurationClassParser.parse(candidates); //这里对InitApplication进行BeanDefinition的解析
													|->ConfigurationClassParser.process();
														|->ConfigurationClassParser$DeferredImportSelectorGroupingHandler.processGroupImports();
															|->ConfigurationClassParser$DeferredImportSelectorGroupingHandler.getImports();
																|->ConfigurationClassParser.group.process(deferredImport.getConfigurationClass().getMetadata(),
																				deferredImport.getImportSelector());
																	|->AutoConfigurationImportSelector.getAutoConfigurationEntry(annotationMetadata);
																		|->AutoConfigurationImportSelector.getCandidateConfigurations(annotationMetadata, attributes);
																			|->SpringFactoriesLoader.loadFactoryNames(getSpringFactoriesLoaderFactoryClass(),getBeanClassLoader()); //这里找到spi对应的实现类
											|->ConfigurationClassPostProcessor.reader.loadBeanDefinitions(configClasses);  //这里将spi对应的实现类注册为spring ioc中的bean
															
														
````

