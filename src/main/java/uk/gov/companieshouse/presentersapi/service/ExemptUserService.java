package uk.gov.companieshouse.presentersapi.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;

@Service
public interface ExemptUserService {

    ExemptUserDao getExemptUserByEmail(String email);

    ExemptUserDao getExemptUserById(String id);

    Boolean isExemptUserByEmail(String email);

    Boolean isExemptUserById(String id);

    String getExemptUserStatement(ExemptUserDao exemptUserDao);
}
