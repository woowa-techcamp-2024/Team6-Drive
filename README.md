## 👀 Team Memebers

<div align="center">

| <center>강승훈</center>                                                                                      | <center>김수현</center>                                                                                | <center>김승수</center>                                                                                  | <center>윤중진</center>                                                                                  |
|-----------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| <a href="https://github.com/seungh1024"><img width="100px" src="https://github.com/seungh1024.png" /></a> | <a href="https://github.com/i960107"><img width="100px" src="https://github.com/i960107.png" /></a> | <a href="https://github.com/KoKimSS"><img width="100px" src="https://github.com/KoKimSS.png" /></a> | <a href="https://github.com/kariskan"><img width="100px" src="https://github.com/kariskan.png" /></a> |

</div>

## Branch Strategy

- main branch: 실제 서버가 배포되는 branch
- dev branch: feature branch에서 병합되는 branch
- feature branch: 기능 단위 하나를 작성하는 branch
  - feature/#{issue number}
    - feature/#56

## merge Strategy
1. Approve가 최소 페어 당 1명 이상
2. Sonarqube 통과
   - Major Issue가 모두 해결됐을 때만 merge
3. 코드 리뷰는 상대 pair 2명이 모두 남겼을 때만 merge 가능
4. main branch에 push할 때만 실제 서버에 배포

## Issue
### Label
- Feature: 기능개발
- Refactor: 리팩토링
- Fix: 기능 수정, 버그 해결
- Docs: 문서 작업
- Test: 테스트 코드 작성
- Chore: 배포 및 설정 변경

### Template
- 제목: 기능
  - 문장 형태로 작성(ex: 배포 환경에서 MySQL DB를 연결한다.)
- 내용
    - ✨ 구현할 기능이 무엇인가요?
    - ✅ 해야할 테스크들을 작성해주세요.

## Git Convention

### 포맷

```
type: subject

body
```

#### type

- 하나의 커밋에 여러 타입이 존재하는 경우 상위 우선순위의 타입을 사용한다.
- fix: 버스 픽스
- feat: 새로운 기능 추가
- refactor: 리팩토링 (버그픽스나 기능추가없는 코드변화)
- docs: 문서만 변경
- style: 코드의 의미가 변경 안 되는 경우 (띄어쓰기, 포맷팅, 줄바꿈 등)
- test: 테스트코드 추가/수정
- chore: 빌드 테스트 업데이트, 패키지 매니저를 설정하는 경우 (프로덕션 코드 변경 X)

#### subject

- 제목은 50글자를 넘지 않도록 한다.
- 개조식 구문 사용
    - 중요하고 핵심적인 요소만 간추려서 (항목별로 나열하듯이) 표현
- 마지막에 특수문자를 넣지 않는다. (마침표, 느낌표, 물음표 등)

#### body (optional)

- 각 라인별로 balled list로 표시한다.
    - 예시) - AA
- 가능하면 한줄당 72자를 넘지 않도록 한다.
- 본문의 양에 구애받지 않고 최대한 상세히 작성
- “어떻게” 보다는 “무엇을" “왜” 변경했는지 설명한다.