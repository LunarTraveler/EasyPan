package com.xcu.config;

import com.xcu.interceptor.LoginInterceptor;
import com.xcu.json.JacksonObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class MvcConfiguration implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    // 多拦截器有先后顺序问题，要么是编码先后，还可以是 .order(number is litter the order is bigger)
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/checkCode",
                        "/sendEmailCode",
                        "/register",
                        "/login",
                        "/resetPwd");
    }

    /**
     * 增加额外的类型转换器
     * @param converters
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 创建一个消息转化器对象
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        // 对象转换器也就是Java对象序列化为json对象
        converter.setObjectMapper(new JacksonObjectMapper());
        // 把这个添加到容器中
        converters.add(0, converter);
    }

}
