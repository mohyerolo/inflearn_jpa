package jpabook.jpashop.Service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
/**
 * readOnly = true: jpa가 조회하는 곳에서는 성능 최적화.
 * 더티체킹 안 하고 데이터베이스에 따라서는 읽기 전용 트랜잭션이면
 * db에 읽기용으로 가져오라는 드라이버도 있음
 * 읽기에는 가급적으로 넣어주기.
 * 그런데 여기에는 조회 메서드가 더 많으니 전체에 적용되도록 위에 넣어준 것
 */
@Transactional(readOnly = true)
//@AllArgsConstructor // 생성자 전부 생성
@RequiredArgsConstructor // final이 있는 필드로 생성자를 만들어줌
public class MemberService {

    /**
     * field injection
     * 장: 간편
     * 단: mock을 주입하기 힘들다.
     * 
     * final: 변경할 일이 없기때문에 final 붙이는 걸 권장. compile 시점에 check할 수 있는 게 있으므로.
     */
//    @Autowired
    private final MemberRepository memberRepository;

    /**
     * setter injection
     * 장: 중간에 테스트 할 때, mock을 주입해줄 수 있음.
     * 단: 실제 application이 돌아갈때 set으로 누군가가 바꿀 수 있음
     * 그런데 애플리케이션 로딩 시점에 setting이 다 끝나서 바꿀 일을 없음
     * 그러면 setter injection이 안 좋음
     */
//    @Autowired
//    public void setMemberRepository(MemberRepository memberRepository) {
//        this.memberRepository = memberRepository;
//    }

    /**
     * 생성자 injection (권장)
     * 스프링이 뜰 때 생성자에서 injection
     * 한 번 생성할때 완성되므로 중간에 set으로 바꿀 수 없음.
     * 테스트 케이스 작성할 때 memeberService에 주입하는 걸 안 놓칠 수 있음.
     * 요즘 스프링은 @Autowired를 안 써도 생성자가 하나만 있으면 자동으로 적용
     * lombok을 사용하면 RequiredArgs로 간편하게 가능ㄴ
     */
//    @Autowired
//    public MemberService(MemberRepository memberRepository) {
//        this.memberRepository = memberRepository;
//    }

    /**
     * 회원가입
     * 따로 Transactional을 적어주면 얘는 default값인
     * readOnly = false로 적용해서 트랜잭션됨
     */
    @Transactional
    public Long join(Member member) {
        validateDuplicateMember(member); // 중복 회원 검증
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
        /**
         * 이 로직은 멤버 A가 동시에 DB insert를 하게 되면 동시에 이 로직을 호출하게 됨
         * 멀티쓰레드 상황을 고려해서 DB에 member name에 unique 제약 조건을 걸어주는 게 안전
         */
        List<Member> findMembers = memberRepository.findByName(member.getName());
        if (!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    // 회원 전체 조회
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public Member findOne(Long memberId) {
        return memberRepository.findOne(memberId);
    }

    @Transactional
    public void update(Long id, String name) {
        Member member = memberRepository.findOne(id);
        member.setName(name);
    }
}
