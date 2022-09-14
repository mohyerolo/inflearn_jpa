package jpabook.jpashop.Service;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional // overriding
    public void saveItem(Item item) {
        itemRepository.save(item);
    }

//    @Transactional
//    public void updateItem(Long itemId, Book param) {
//        Item findItem = itemRepository.findOne(itemId);
//        // findItem에 대한 의미있는 메서드로 해주는게 좋지(변경 추적 용이) set을 밑에처럼 다 설정하면 안 됨
//        // ex) findItem.change(price, name, stockQuantity);
//        findItem.setPrice(param.getPrice());
//        findItem.setName(param.getName());
//        findItem.setStockQuantity(param.getStockQuantity());
//        // ...
//        /**
//         * findItem으로 찾아온거는 영속 상태.
//         * set으로 변경을 하면 transaction에 의해 commit이 됨.
//         * commit이 되면 jpa는 flush를 날려 변경 사항을 찾아 db에 update 쿼리를 날린다.
//         */
//    }

    @Transactional
    public void updateItem(Long itemId, String name, int price, int stockQuantity) {
        Item findItem = itemRepository.findOne(itemId);

        findItem.setName(name);
        findItem.setPrice(price);
        findItem.setStockQuantity(stockQuantity);
    }

    public List<Item> findItems() {
        return itemRepository.findAll();
    }

    public Item findOne(Long itemId) {
        return itemRepository.findOne(itemId);
    }

}
