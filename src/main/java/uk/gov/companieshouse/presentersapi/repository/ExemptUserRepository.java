package uk.gov.companieshouse.presentersapi.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;

@Repository
public interface ExemptUserRepository extends MongoRepository<ExemptUserDao, String> {

    @Query("{ 'email' : {$eq: ?0, $type: 'string' }}")
    ExemptUserDao findByEmail(String email);

    @Query ("{_id : ?0}")
    ExemptUserDao findByObjectId(String id);

}
