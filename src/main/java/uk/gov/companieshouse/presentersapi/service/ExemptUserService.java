package uk.gov.companieshouse.presentersapi.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;

import java.util.Optional;

@Service
public interface ExemptUserService {

    Optional<ExemptUserDao> getExemptUserByEmail(String email);

    Optional<ExemptUserDao> getExemptUserById(String id);

    boolean isExemptUserByEmail(String email);

    boolean isExemptUserById(String id);
}
