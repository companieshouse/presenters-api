package uk.gov.companieshouse.presentersapi.service.impl;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;
import uk.gov.companieshouse.presentersapi.repository.ExemptUserRepository;
import uk.gov.companieshouse.presentersapi.service.ExemptUserService;

import java.util.Map;
import java.util.Optional;

@Service
public class ExemptUserServiceImpl implements ExemptUserService {

    private final ExemptUserRepository repository;
    private final Logger logger;
    public ExemptUserServiceImpl(ExemptUserRepository repository, Logger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    @Override
    public Optional<ExemptUserDao> getExemptUserByEmail(String email) {
        Map<String, Object> logMap = Map.of("email", email);
        Optional<ExemptUserDao> exemptUserDao = repository.findByEmail(email);
        if (exemptUserDao.isPresent()) {
            logger.infoContext(email, "Retrieved exempt user by email: ", logMap);
        } else {
            logger.infoContext(email, "No Such exempt user with email: ", logMap);
        }
        return exemptUserDao;
    }
    @Override
    public Optional<ExemptUserDao> getExemptUserById(String id) {
        Map<String, Object> logMap = Map.of("id", id);
        Optional<ExemptUserDao> exemptUserDao = repository.findById(id);
        if (exemptUserDao.isPresent()) {
            logger.infoContext(id, "Retrieved exempt user by id: ", logMap);
        }  else {
            logger.infoContext(id, "No Such exempt user with id: ", logMap);
        }
        return exemptUserDao;
    }

    @Override
    public boolean isExemptUserByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean isExemptUserById(String id) {
        return repository.existsById(id);
    }



}
