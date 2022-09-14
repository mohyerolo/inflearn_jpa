package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    // 엔티티를 노출
    @GetMapping("/api/v1/orders")
    private List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
            // order item도 프록시 초기화, 이름도 필요하니까 그것까지 다 초기화
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return all;
    }

    // 쿼리가 매우 많이 나옴 (collection 써서 그럼)
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }

    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
//        for (Order order :
//                orders) {
//            System.out.println("order = " + order + ", " + "orderId = " + order.getId());
//            System.out.println("orderItems = " + order.getOrderItems());
//            System.out.println();
//        }
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }

    /**
     * OSIV를 끄고나면 (application.yml에 false로 설정함)
     * Service.query.OrderQueryService 안에 위의 로직을 전부 옮기고
     * 여기서는 그냥 return orderQueryService.ordersV3();로 끝내면 됨
     */



    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset
                                        , @RequestParam(value = "limit", defaultValue = "100") int limit) {
        // ToOne 관계는 Order를 기준으로 패치 조인
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }

    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        /**
         * 여기서 밑에 있는 OrderDto를 참조하면
         * repository가 controller를 참조하게 만드니까
         * 따로 Dto를 만드는 것
         */
        return orderQueryRepository.findOrderQueryDtos();
    }

    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /**
     * 이거의 결과는
     * [
     *     {
     *         "orderId": 4,
     *         "name": "userA",
     *         "orderDate": "2022-09-14T13:57:04.377149",
     *         "orderStatus": "ORDER",
     *         "address": {
     *             "city": "서울",
     *             "street": "1",
     *             "zipcode": "1111"
     *         },
     *         "itemName": "JPA1 BOOK",
     *         "orderPrice": 10000,
     *         "count": 1
     *     },
     *     {
     *         "orderId": 4,
     *         "name": "userA",
     *         "orderDate": "2022-09-14T13:57:04.377149",
     *         "orderStatus": "ORDER",
     *         "address": {
     *             "city": "서울",
     *             "street": "1",
     *             "zipcode": "1111"
     *         },
     *         "itemName": "JPA2 BOOK",
     *         "orderPrice": 20000,
     *         "count": 2
     *     },
     *     {
     *         "orderId": 11,
     *         "name": "userB",
     *         "orderDate": "2022-09-14T13:57:04.437966",
     *         "orderStatus": "ORDER",
     *         "address": {
     *             "city": "진주",
     *             "street": "2",
     *             "zipcode": "2222"
     *         },
     *         "itemName": "SPRING1 BOOK",
     *         "orderPrice": 20000,
     *         "count": 3
     *     },
     *     {
     *         "orderId": 11,
     *         "name": "userB",
     *         "orderDate": "2022-09-14T13:57:04.437966",
     *         "orderStatus": "ORDER",
     *         "address": {
     *             "city": "진주",
     *             "street": "2",
     *             "zipcode": "2222"
     *         },
     *         "itemName": "SPRING2 BOOK",
     *         "orderPrice": 40000,
     *         "count": 4
     *     }
     * ]
     * 이런식으로 나옴. 이거를 v5를 돌렸을 때와 같은 방식으로 받고싶다면 변경 필요
     */
//    @GetMapping("/api/v6/orders")
//    public List<OrderFlatDto> ordersV6() {
//        return orderQueryRepository.findAllByDto_flat();
//    }

    /**
     * flat으로 루프를 돌림. OrderFlatDto를 OrderQueryDto로 바꾸는 것.
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(),
                                o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(),
                                o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
                        e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }
    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            // address는 밸류 오브젝트라 바뀔 일이 없어서 그냥 써도 됨.
            address = order.getDelivery().getAddress();
//            이거는 orderItem을 OrderItemDto가 아닌 엔티티인 OrderItem을 가지고 왔을 때 얘기
//            order.getOrderItems().stream().forEach(o -> o.getItem().getName()); // 프록시 초기화. 이거 안 하면 orderItem은 엔티티여서 안 보임
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;    // 상품 명
        private int orderPrice;     // 주문 가격
        private int count;          // 주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
