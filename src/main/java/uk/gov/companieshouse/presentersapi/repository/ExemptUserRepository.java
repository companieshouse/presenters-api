package uk.gov.companieshouse.presentersapi.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;

import java.util.Optional;

@Repository
public interface ExemptUserRepository extends MongoRepository<ExemptUserDao, String> {

    Optional<ExemptUserDao> findByEmail(String email);

    Optional<ExemptUserDao> findById(String id);

    boolean existsByEmail(String email);

    boolean existsById(String id);

}
