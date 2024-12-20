package com.xcu.config;

import com.xcu.constants.Constants;
import com.xcu.constants.RedisConstant;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RateLimiterConfig {

    private final RedissonClient redissonClient;

    /**
     * 全局的限流器，用来保证系统的带宽安全，具体来参照服务器的带宽值(这里就先不设置过期时间了)
     * @return
     */
    @Bean
    public RRateLimiter globalRateLimiter() {
        RRateLimiter globalRateLimiter = redissonClient.getRateLimiter(RedisConstant.GLOBAL_RATE_LIMITER);
        globalRateLimiter.trySetRate(
                RateType.OVERALL,
                Constants.GLOBAL_LIMIT_SPEED,
                Constants.GLOBAL_LIMIT_SPEED * 2,
                RateIntervalUnit.SECONDS
        );
        return globalRateLimiter;
    }

}
