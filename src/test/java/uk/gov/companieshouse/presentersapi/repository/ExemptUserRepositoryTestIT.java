package uk.gov.companieshouse.presentersapi.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.presentersapi.TestUtils.ExemptUserTestObjects;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataMongoTest
@Testcontainers
class ExemptUserRepositoryTestIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:8"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ExemptUserRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private List<ExemptUserDao> savedExemptUsers;

    @BeforeEach
    void setUp() {
        savedExemptUsers = repository.saveAll(ExemptUserTestObjects.createExemptUserDaoList());
    }

    @AfterEach
    void afterEach() {
        mongoTemplate.dropCollection(ExemptUserDao.class);
    }

    @Test
    void getsExemptUserByEmail() {
        String exemptUserTestEmail = savedExemptUsers.getFirst().getEmail();
        Optional<ExemptUserDao> exemptUser = repository.findByEmail(exemptUserTestEmail);

        assertThat(exemptUser).isNotNull();
        assertThat(exemptUser.get().getEmail()).isEqualTo(exemptUserTestEmail);
    }

    @Test
    void getsExemptUserById() {
        String objectId = savedExemptUsers.getFirst().getId();
        Optional<ExemptUserDao> exemptUser = repository.findById(objectId);
        assertThat(exemptUser).isNotNull();
        assertThat(exemptUser.get().getId()).isEqualTo(objectId);
    }

    @Test
    void noSuchExemptUserByEmail() {
        String email = "";
        Optional<ExemptUserDao> result = repository.findByEmail(email);
        assertThat(result).isEmpty();
    }

    @Test
    void noSuchExemptUserById() {
        String objectId = "";
        Optional<ExemptUserDao> result = repository.findById(objectId);
        assertThat(result).isEmpty();
    }
}
