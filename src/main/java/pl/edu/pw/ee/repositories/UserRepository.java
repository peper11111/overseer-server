package pl.edu.pw.ee.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.edu.pw.ee.entities.User;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface UserRepository extends CrudRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.username = :username")
    User findByUsername(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.token = :token")
    User findByToken(@Param("token") String token);

    @Query("SELECT u FROM User u WHERE u.supervisor = :id")
    Iterable<User> findBySupervisor(@Param("id") Long id);
}