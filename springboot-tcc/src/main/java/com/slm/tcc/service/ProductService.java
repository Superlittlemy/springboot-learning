package com.slm.tcc.service;

import com.slm.tcc.entity.Product;
import com.slm.tcc.entity.ProductConsumptionDetail;
import com.slm.tcc.mapper.ProductConsumptionDetailMapper;
import com.slm.tcc.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 商品服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductConsumptionDetailMapper productConsumptionDetailMapper;
    @Autowired
    @Lazy
    private OrderService orderService;

    /**
     * 获取商品信息
     *
     * @param id
     * @return
     */
    @Transactional(readOnly = true)
    public Product get(Long id) {
        return productMapper.getById(id).orElseThrow(() -> new RuntimeException("商品不存在"));
    }

    /**
     * 商品库存预扣减
     *
     * @param id 商品id
     * @param num 数量
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long productPreDeduct(Long id, Long num, Long orderId) {
        Product product = this.get(id);
        if (product.getNum() < num) {
            throw new RuntimeException("库存不足");
        }
        // 扣减库存
        Long m = productMapper.deductCount(id, num);
        if (m == 0) {
            throw new RuntimeException("库存不足");
        }
        ProductConsumptionDetail detail = new ProductConsumptionDetail();
        detail.setProductId(id);
        detail.setNum(num);
        detail.setOrderId(orderId);
        detail.setStatus(0); // 待确认中间态
        detail.setCreatedTime(LocalDateTime.now());
        // 记录商品消费明细
        productConsumptionDetailMapper.create(detail);
        log.info("商品{}库存{}预购订单{}采购数量{}", id, product.getNum(), orderId, num);
        return detail.getId();
    }

    /**
     * 下单确认
     *
     * @param orderId 订单id
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId) {
        // 模拟确认异常，如果发生异常情况 taskUtil 定时任务会触发订单自动关闭
        boolean success = true;
        if (success) {
            productConsumptionDetailMapper.confirm(orderId);
            orderService.confirm(orderId);
            log.info("订单{}确认", orderId);
        }
    }

    /**
     * 下单取消
     *
     * @param orderId 订单id
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId) {
        productConsumptionDetailMapper.getByOrderId(orderId).ifPresent(detail -> {
            // 撤销库存扣减
            productMapper.cancelDeduct(detail.getProductId(), detail.getId());
            // 关闭消费明细
            productConsumptionDetailMapper.cancel(detail.getId());
        });
        log.error("订单{}撤销", orderId);
    }

}
