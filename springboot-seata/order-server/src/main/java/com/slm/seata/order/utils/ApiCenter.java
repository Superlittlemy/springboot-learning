package com.slm.seata.order.utils;

import com.slm.seata.order.enums.ResultStatus;
import com.slm.seata.order.model.ApiResponse;
import com.slm.seata.order.model.client.storage.Merchandise;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiCenter {

    @Value("${application.storage.url}")
    private String storageClientUrl;
    @Value("${application.account.url}")
    private String accountClientUrl;

    private final HttpClient httpClient;

    /**
     * 获取商品信息
     *
     * @param id 商品id
     * @return 商品信息
     */
    public Merchandise getMerchandiseInfo(Long id) {
        ApiResponse<Merchandise> resp = httpClient.request(HttpMethod.GET, storageClientUrl + "/merchandises/" + id, null, null, Merchandise.class);
        if (ResultStatus.SUCCESS.getCode() != resp.getCode()) {
            log.error("接口状态码异常：{}", resp);
            throw new RuntimeException(resp.getMessage() + "：" + resp.getData());
        }
        return resp.getData();
    }

    /**
     * 扣减库存
     *
     * @param id 商品id
     * @param quantity 扣减数量
     */
    public void merchandiseDeduct(Long id, BigDecimal quantity, Long accountId) {
        ApiResponse<Merchandise> resp = httpClient.request(HttpMethod.PUT, storageClientUrl + "/merchandises/" + id + "/deduct?quantity=" + quantity + "&buyerId=" + accountId, null, null, null);
        if (ResultStatus.SUCCESS.getCode() != resp.getCode()) {
            log.error("接口状态码异常：{}", resp);
            throw new RuntimeException(resp.getMessage() + "：" + resp.getData());
        }
    }

    /**
     * 账户支付
     *
     * @param id 账户id
     * @param amount 支付金额
     */
    public void accountPay(Long id, BigDecimal amount) {
        ApiResponse<Merchandise> resp = httpClient.request(HttpMethod.POST, accountClientUrl + "/accounts/" + id + "/pay?amount=" + amount, null, null, null);
        if (ResultStatus.SUCCESS.getCode() != resp.getCode()) {
            log.error("接口状态码异常：{}", resp);
            throw new RuntimeException(resp.getMessage() + "：" + resp.getData());
        }
    }

}
