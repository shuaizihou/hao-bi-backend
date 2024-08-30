package com.wh.haobi.config;

import io.github.briqt.spark4j.SparkClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 讯飞配置
 * @author <a href="https://github.com/shuaizihou>甩子候</a>
 */
@Configuration
@ConfigurationProperties(prefix = "xunfei.client")
@Data
public class XingHuoConfig {
  private String appid;
  
  private String apiSecret;
  
  private String apiKey;

  @Bean
  public SparkClient sparkClient() {
    SparkClient sparkClient = new SparkClient();
    sparkClient.apiKey = apiKey;
    sparkClient.apiSecret = apiSecret;
    sparkClient.appid = appid;
    return sparkClient;
   }
}