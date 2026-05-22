package com.jomariabejo.connectly_api.orders_api.service;

import com.jomariabejo.connectly_api.orders_api.dto.OrderItemDto;
import com.jomariabejo.connectly_api.orders_api.entity.Order;
import com.jomariabejo.connectly_api.orders_api.entity.OrderItem;
import com.jomariabejo.connectly_api.orders_api.mapper.OrderItemMapper;
import com.jomariabejo.connectly_api.orders_api.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderItemMapper orderItemMapper;

    public OrderItemService(OrderItemRepository orderItemRepository, OrderItemMapper orderItemMapper) {
        this.orderItemRepository = orderItemRepository;
        this.orderItemMapper = orderItemMapper;
    }

    @Transactional
    public void createOrderItems(Order order, List<OrderItemDto> itemDtos) {
        List<OrderItem> items = itemDtos.stream()
                .map(dto -> {
                    OrderItem item = orderItemMapper.toEntity(dto);
                    item.setOrder(order);
                    return item;
                })
                .toList();
        orderItemRepository.saveAll(items);
    }

    public List<OrderItemDto> getOrderItems(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return orderItemMapper.toDtoList(items);
    }

    @Transactional
    public void deleteOrderItems(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        orderItemRepository.deleteAll(items);
    }
}
