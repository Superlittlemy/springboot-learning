package com.slm.seata.storage.service;

import com.slm.seata.storage.entity.Merchandise;
import com.slm.seata.storage.model.MerchandiseCreate;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.math.BigDecimal;

/**
 * 商品服务
 */
@LocalTCC
public interface MerchandiseService {

    /**
     * 创建商品
     *
     * @param merchandiseCreate 商品创建信息
     * @return 完整商品信息
     */
    Merchandise create(MerchandiseCreate merchandiseCreate);

    /**
     * 获取商品信息
     *
     * @param id 商品id
     * @return 商品信息
     */
    Merchandise get(Long id);

    /**
     * 扣减库存
     *
     * @param requestId 请求id
     * @param id 商品id
     * @param quantity 扣减数量
     * @param buyerId 购买账户
     */
    @TwoPhaseBusinessAction(name = "prepare", useTCCFence = true)
    void deduct(@BusinessActionContextParameter(paramName = "requestId") String requestId, Long id, BigDecimal quantity, Long buyerId);

    boolean commit(BusinessActionContext context);

    boolean rollback(BusinessActionContext context);

}
