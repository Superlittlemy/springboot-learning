package com.slm.resttemplate;

import com.slm.resttemplate.model.TradeEntity;
import com.slm.resttemplate.model.TradeEntityCreate;
import com.slm.resttemplate.model.TradeEntityType;
import com.slm.resttemplate.utils.HttpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.Map;

@SpringBootTest
public class HttpTest {

    @Autowired
    private HttpUtil httpUtil;

    @Test
    void request() throws IOException {
        TradeEntity request = httpUtil.request(HttpMethod.GET, "http://localhost:8080/trade-entities/1/detail", Map.of(), null, new TradeEntity());
        Long id = httpUtil.request(HttpMethod.POST, "http://localhost:8080/trade-entities", Map.of(),
                TradeEntityCreate.builder().name("测试").type(TradeEntityType.BUYER).build(), 1L);
        System.out.println(id);
        byte[] bytes = httpUtil.downloadFile(HttpMethod.GET, "http://localhost:9100/files", Map.of());
    }

}
