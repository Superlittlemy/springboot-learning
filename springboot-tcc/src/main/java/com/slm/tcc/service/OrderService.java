package com.slm.tcc.service;

import com.slm.tcc.Application;
import com.slm.tcc.entity.Product;
import com.slm.tcc.entity.TccOrder;
import com.slm.tcc.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    @Autowired
    @Lazy
    private ProductService productService;

    /**
     * 创建订单
     *
     * @param productId 商品id
     * @param num 数量
     */
    @Transactional(rollbackFor = Exception.class)
    public TccOrder createOrder(Long productId, Long num) {
        // 查询商品信息
        Product product = productService.get(productId);
        // 创建订单
        TccOrder order = new TccOrder();
        order.setProductId(product.getId());
        order.setProductName(product.getProductName());
        order.setProductNum(num);
        order.setProductPrice(product.getPrice());
        order.setOrderAmount(product.getPrice().multiply(new BigDecimal(String.valueOf(num))));
        order.setOrderStatus(0);
        order.setOrderTime(LocalDateTime.now());
        orderMapper.create(order);
        // 事务钩子（确认事务状态） - 注册提前，避免后续报错导致没有注册事务同步
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == 0) { // 事务提交成功 Confirm
                    productService.confirm(order.getId());
                } else { // 事务回滚 Rollback
                    productService.cancel(order.getId());
                }
            }
        });
        // 扣减商品库存 Try 阶段
        try {
            productService.productPreDeduct(productId, num, order.getId());
        } catch (Exception e) {
            log.error("商品库存扣减失败：{}", e.getMessage());
            throw e;
        }
        // 模式事务内报错
        boolean hasError = false;
        if (hasError) {
            throw new RuntimeException("发生异常");
        }
        // 正常返回
        return order;
    }

    /**
     * 订单确认
     *
     * @param orderId 订单id
     */
    public void confirm(Long orderId) {
        orderMapper.confirm(orderId);
    }

    /**
     * 撤销订单
     *
     * @param id 订单id
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        // 撤销订单
        orderMapper.cancel(id);
        // 扣减商品库存
        productService.cancel(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void autoclose() {
        // 获取需要自动关闭的订单
        orderMapper.getAutocloseOrder().forEach(order -> {
            // 每个订单的关闭采用独立事务，避免多个订单自动关闭其中一个失败导致其余已经关闭成功的订单无法关闭
            Application.context.getBean(OrderService.class).invokeOrderClose(order.getId());
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void invokeOrderClose(Long id) {
        try {
            productService.cancel(id);
            orderMapper.cancel(id);
        } catch (Exception e) {
            log.error("自动关闭订单{}失败：{}", id, e.getMessage());
        }
    }

}
