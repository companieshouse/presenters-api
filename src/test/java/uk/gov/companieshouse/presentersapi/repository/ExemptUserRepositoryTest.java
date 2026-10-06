package uk.gov.companieshouse.presentersapi.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.companieshouse.presentersapi.PresentersApiApplication;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;
import uk.gov.companieshouse.presentersapi.repository.ExemptUserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PresentersApiApplication.class)
class ExemptUserRepositoryTest {

    @DynamicPropertySource
    static void mongoDbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://localhost:27017/presenters-test");
        registry.add("internal.api.url", () -> "http://localhost:4001");
    }

    @Autowired
    private ExemptUserRepository repository;

    @Test
    void getsExemptUserByEmail() {
        String email = "demo1@companieshouse.gov.uk";
        ExemptUserDao result = repository.findByEmail(email);
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
    }
    @Test
    void getsExemptUserById(){
        String objectId = "6abce8e5ec5300857ea71981";
        ExemptUserDao result = repository.findByObjectId(objectId);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(objectId);
    }

    @Test
    void noSuchExemptUserByEmail(){
        String email = "";

        ExemptUserDao result = repository.findByEmail(email);

        assertThat(result).isNull();
    }

    @Test
    void noSuchExemptUserById(){
        String objectId = "";
        ExemptUserDao result = repository.findByObjectId(objectId);

        assertThat(result).isNull();
    }

}
