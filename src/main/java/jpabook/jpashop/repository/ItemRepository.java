package jpabook.jpashop.repository;

import jpabook.jpashop.domain.item.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ItemRepository {

    private final EntityManager em;

    public void save(Item item) {
        if (item.getId() == null) { // 완전히 새로 생성한 객체라는 것
            em.persist(item); // 신규 등록
        } else { // 이미 db에 등록된걸 가져온 것
            /**
             * merge는 모든 속성을 변경.
             * 하나의 값에 변경 값이 없으면 걔는 변경이 안 되는 게 아닌
             * null 값으로 변경되는 것.
             * 그러므로 웬만하면 merge보다는 변경감지(settter)를 사용하는 게 좋음
             */
            em.merge(item);
        }
    }

    public Item findOne(Long id) {
        return em.find(Item.class, id);
    }

    public List<Item> findAll() {
        return em.createQuery("select i from Item i", Item.class)
                .getResultList();
    }


}
