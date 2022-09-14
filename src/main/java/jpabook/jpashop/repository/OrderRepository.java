package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    public List<Order> findAll(OrderSearch orderSearch) {
        // 동적 쿼리를 만들어야됨
        return em.createQuery("select o from Order o join o.member m " +
                "where o.status = :status " +
                "and m.name like :name", Order.class)
                .setParameter("status", orderSearch.getOrderStatus())
                .setParameter("name", orderSearch.getMemberName())
                .setMaxResults(1000) // 최대 1000건
                .getResultList();

    }

    public List<Order> findAllByString(OrderSearch orderSearch) {
        //language=JPAQL
        String jpql = "select o From Order o join o.member m";
        boolean isFirstCondition = true;
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }
        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000); //최대 1000건
        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }
        return query.getResultList();
    }

    // JPA Criteria로 처리
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Order, Member> m = o.join("member", JoinType.INNER); //회원과 조인
        List<Predicate> criteria = new ArrayList<>();
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"),
                    orderSearch.getOrderStatus());
            criteria.add(status);
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name =
                    cb.like(m.<String>get("name"), "%" +
                            orderSearch.getMemberName() + "%");
            criteria.add(name);
        }
        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000);
        return query.getResultList();
    }


    /**
     * 외부의 모습을 건들이지 않은 상태. 내부에서 원하는 것만 fetch join으로 성능 튜닝을 한 것
     * entity를 조회해서 비즈니스 로직에서 데이터 변경 가능
     */
    public List<Order> findAllWithMemberDelivery() {
        // order, member, delivery를 join하고 select 절에 다 넣어서 가져오는 것
        // 이 경우에는 LAZY는 다 무시됨
        return em.createQuery("select o from Order o" +
                " join fetch o.member m" +
                " join fetch o.delivery d", Order.class)
                .getResultList();
    }

    // 페이징 쿼리
    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return em.createQuery("select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }


    /**
     * distinct 없이 실행하면 id가 중복된 결과가 그대로 나옴
     * distinct를 여기서 넣고 실행하면 db 쿼리에서는 모든 결과가 안 똑같아야 안 뽑힘.
     * 그런데 모든 결과가 똑같지 않으니까 db에서는 결국 다 뽑혀도 결과는 id 중복된게 없이 나오기는 함
     * 결국 jpa에서 자체적으로 order가 같은 값이면 중복을 제거해주는 방식으로 리스트에 담아서 반환.
     * 애플리케이션에 다 가져와서 order 객체의 id가 같으면 중복을 제거하고 컬렉션에 담는 것.
     *
     * distinct 키워드의 역할
     * 1. db에 distinct 키워드를 날림
     * 2. 루트 엔티티가 중복인 경우에 중복을 없애고 보내줌
     */
    public List<Order> findAllWithItem() {
        return em.createQuery(
                "select distinct o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d" +
                        " join fetch o.orderItems oi" +
                        " join fetch oi.item i", Order.class)
                .setFirstResult(1)
                .setMaxResults(100)
                .getResultList();
    }



    /**
     * v3보다는 최적화됐지만 재사용성이 적음. 딱 이때만 사용할 수 있는 것
     * DTO를 조회해서 데이터 변경을 할 수 없음.
     * 논리적으로 계층이 깨져있음. repository가 화면을 의지하고 있음
     * api가 바뀌면 얘도 다 뜯어고쳐야 되는 것.
     * repository는 entity를 조회하는 것까지만 써야됨
     * 그러면 이 경우에는 이런 쿼리를 다루는 패키지 하나를 만드는 것도 방법
     * order.simplequery.OrderSimpleQueryRepository
     */
//    public List<OrderSimpleQueryDto> findOrderDtos() {
//        return em.createQuery("select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
//                        " from Order o" +
//                " join o.member m" +
//                " join o.delivery d", OrderSimpleQueryDto.class)
//                .getResultList();
//    }
}
