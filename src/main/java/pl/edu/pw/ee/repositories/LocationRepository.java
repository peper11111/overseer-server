package pl.edu.pw.ee.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pl.edu.pw.ee.entities.Location;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface LocationRepository extends CrudRepository<Location, Long> {
}