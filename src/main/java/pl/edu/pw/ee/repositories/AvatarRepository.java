package pl.edu.pw.ee.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pl.edu.pw.ee.entities.Avatar;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface AvatarRepository extends CrudRepository<Avatar, Long> {
}