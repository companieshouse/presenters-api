package uk.gov.companieshouse.presentersapi.service.impl;


import org.springframework.stereotype.Service;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;
import uk.gov.companieshouse.presentersapi.repository.ExemptUserRepository;
import uk.gov.companieshouse.presentersapi.service.ExemptUserService;

import java.util.Map;

@Service
public class ExemptUserServiceImpl implements ExemptUserService {

    private final ExemptUserRepository repository;
    private final Logger logger;
    public ExemptUserServiceImpl(ExemptUserRepository repository, Logger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    @Override
    public ExemptUserDao getExemptUserByEmail(String email) {
        Map<String, Object> logMap = Map.of("email", email);
        ExemptUserDao exemptUserDao = repository.findByEmail(email);
        if (exemptUserDao != null) {
            logger.infoContext(email, "Retrieved exempt user by email: ", logMap);
            return exemptUserDao;
        }  else {
            logger.infoContext(email, "No Such exempt user with email: ", logMap);
            return null;
        }
    }
    @Override
    public ExemptUserDao getExemptUserById(String id) {
        Map<String, Object> logMap = Map.of("id", id);
        ExemptUserDao exemptUserDao = repository.findByObjectId(id);
        if (exemptUserDao != null) {
            logger.infoContext(id, "Retrieved exempt user by id: ", logMap);
            return exemptUserDao;
        }  else {
            logger.infoContext(id, "No Such exempt user with id: ", logMap);
            return null;
        }
    }

    @Override
    public Boolean isExemptUserByEmail(String email) {
        return repository.findByEmail(email) != null;
    }

    @Override
    public Boolean isExemptUserById(String id) {
        return repository.findByObjectId(id) != null;
    }

    @Override
    public String getExemptUserStatement(ExemptUserDao exemptUserDao) {
        return exemptUserDao.getExemptPresenterStatement();
    }


}
