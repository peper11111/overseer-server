package pl.edu.pw.ee.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.edu.pw.ee.entities.Location;
import pl.edu.pw.ee.entities.User;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface LocationRepository extends CrudRepository<Location, Long> {
    @Query("SELECT l FROM Location l WHERE l.user = :user")
    Iterable<Location> findByUser(@Param("user") User user);
}