package pl.edu.pw.ee.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.edu.pw.ee.entities.User;
import pl.edu.pw.ee.entities.WorkTime;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface WorkTimeRepository extends CrudRepository<WorkTime, Long> {
    @Query("SELECT w FROM WorkTime w WHERE w.stop IS NULL AND w.user = :user")
    WorkTime findByUser(@Param("user") User user);

    @Query("SELECT w FROM WorkTime w WHERE w.start >= :start AND w.stop <= :stop AND w.user = :user")
    Iterable<WorkTime> findByInterval(@Param("start") Long start, @Param("stop") Long stop, @Param("user") User user);
}