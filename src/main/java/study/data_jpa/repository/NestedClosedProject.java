package study.data_jpa.repository;

public interface NestedClosedProject {
    String getUsername();
    TeamInfo getTeam();

    interface TeamInfo{
        String getName();
    }
}
