package jpabook.jpashop.Service;

import jpabook.jpashop.domain.Delivery;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.repository.ItemRepository;
import jpabook.jpashop.repository.MemberRepository;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    // 주문
    @Transactional
    public Long order(Long memberId, Long itemId, int count) {
        // 엔티티 조회
        Member member = memberRepository.findOne(memberId);
        Item item = itemRepository.findOne(itemId);

        // 배송정보 생성
        Delivery delivery = new Delivery();
        delivery.setAddress(member.getAddress());

        // 주문 상품 생성
        OrderItem orderItem = OrderItem.createOrderItem(item, item.getPrice(), count);

        // 주문 생성
        Order order = Order.createOrder(member, delivery, orderItem);

        // 주문 저장
        /**
         * 원래 위에 있는 모든 생성을 다 repository를 통해서 저장해줘야 되는데
         * order에서 orderItems와 delivery에 cascade 속성을 넣어줌
         * 그러면 order에 persist되면 얘네도 같이 persist가 날라감
         * 그래서 하나만 해줘도 되는 것.
         * cascade의 범위는 보통 참조하는게 private owner인 경우에만 써야됨.
         * delivery는 order말고 사용되는 곳이 없음. orderItem도 order만 참조해서 사용함.
         * 다른 곳에서 orderItem을 참조하는 곳이 없음.
         * 그래서 라이프 사이클이 동일할때 의미가 있음.
         *
         * 만약 delivery를 다른 애들이 참조해서 사용한다면 cascade를 함부로 사용하면 안 됨
         */
        orderRepository.save(order);
        return order.getId();
    }

    // 취소
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findOne(orderId);
        order.cancel();
    }

    // 검색
    // 이 메서드는 위임만 하므로 그냥 controller에서 바로 불러도 괜찮음
    public List<Order> findOrders(OrderSearch orderSearch) {
        return orderRepository.findAllByCriteria(orderSearch);
    }
}
