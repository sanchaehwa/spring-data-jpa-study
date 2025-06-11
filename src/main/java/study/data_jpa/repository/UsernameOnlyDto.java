package study.data_jpa.repository;

public class UsernameOnlyDto {
    private final String username;
    //생성자에 파라미터 이름으로 매핑을 시켜서 프로젝션도 됨.
    public UsernameOnlyDto(String username) {
        this.username = username;
    }
    public String getUsername() {
        return username;
    }
}
