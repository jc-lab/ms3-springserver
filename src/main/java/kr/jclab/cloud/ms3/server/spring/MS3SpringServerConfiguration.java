/*
 * Copyright 2018 JC-Lab. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.jclab.cloud.ms3.server.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MS3SpringServerConfiguration implements BeanPostProcessor, WebMvcConfigurer {

    @Autowired
    private ApplicationContext applicationContext;

    private MS3SpringServer server = null;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MS3SpringServerConfigurerAdapter) {
            MS3SpringServerConfigurerAdapter configurerAdapter = (MS3SpringServerConfigurerAdapter)bean;
            server = new MS3SpringServer(configurerAdapter);
            applicationContext.getAutowireCapableBeanFactory().autowireBean(server);
            applicationContext.getAutowireCapableBeanFactory().initializeBean(server, null);
        }
        return bean;
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // For prevent a HttpMediaTypeNotAcceptableException
        configurer.favorPathExtension(false);
    }
}
