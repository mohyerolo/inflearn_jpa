package jpabook.jpashop.Service.query;

import jpabook.jpashop.api.OrderApiController;
import jpabook.jpashop.domain.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Transactional(readOnly = true)
public class OrderQueryService {
    // Transaction 안에서 돌아가니까 open in view를 꺼도 이 안에 있는 코드는 다 돌아감
}
