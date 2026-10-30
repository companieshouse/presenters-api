package uk.gov.companieshouse.presentersapi.service.impl;

import com.mongodb.MongoException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;
import uk.gov.companieshouse.presentersapi.repository.ExemptUserRepository;
import uk.gov.companieshouse.presentersapi.service.ExemptUserService;

import java.util.Map;
import java.util.Optional;

@Service
public class ExemptUserServiceImpl implements ExemptUserService {

    private static final String DATA_ACCESS_MESSAGE = "Unable to retrieve exempt user data";
    private static final String DATA_INTEGRITY_MESSAGE = "Exempt user data integrity issue";
    private static final String EXISTENCE_CHECK_DATA_ACCESS_MESSAGE =
            "Unable to check exempt user existence";

    private final ExemptUserRepository repository;
    private final Logger logger;
    public ExemptUserServiceImpl(ExemptUserRepository repository, Logger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    @Override
    public Optional<ExemptUserDao> getExemptUserByEmail(String email) {
        Map<String, Object> logMap = Map.of("email", email);
        try {
            Optional<ExemptUserDao> exemptUserDao = repository.findByEmail(email);
            if (exemptUserDao.isPresent()) {
                logger.infoContext(email, "Retrieved exempt user by email: ", logMap);
            } else {
                logger.infoContext(email, "No Such exempt user with email: ", logMap);
            }
            return exemptUserDao;
        } catch (IncorrectResultSizeDataAccessException exception) {
            logger.errorContext(email, "Non-unique exempt user query result by email", exception, logMap);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, DATA_INTEGRITY_MESSAGE, exception);
        } catch (DataAccessException | MongoException exception) {
            logger.errorContext(email, "Failed to retrieve exempt user by email", exception, logMap);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, DATA_ACCESS_MESSAGE, exception);
        }
    }

    @Override
    public Optional<ExemptUserDao> getExemptUserByUserId(String userId) {
        Map<String, Object> logMap = Map.of("user_id", userId);
        try {
            Optional<ExemptUserDao> exemptUserDao = repository.findByUserId(userId);
            if (exemptUserDao.isPresent()) {
                logger.infoContext(userId, "Retrieved exempt user by user id: ", logMap);
            } else {
                logger.infoContext(userId, "No such exempt user with user id: ", logMap);
            }
            return exemptUserDao;
        } catch (IncorrectResultSizeDataAccessException exception) {
            logger.errorContext(userId, "Non-unique exempt user query result by user id", exception, logMap);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, DATA_INTEGRITY_MESSAGE, exception);
        } catch (DataAccessException | MongoException exception) {
            logger.errorContext(userId, "Failed to retrieve exempt user by user id", exception, logMap);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, DATA_ACCESS_MESSAGE, exception);
        }
    }

    @Override
    public Optional<ExemptUserDao> getExemptUserById(String id) {
        Map<String, Object> logMap = Map.of("id", id);
        try {
            Optional<ExemptUserDao> exemptUserDao = repository.findById(id);
            if (exemptUserDao.isPresent()) {
                logger.infoContext(id, "Retrieved exempt user by id: ", logMap);
            } else {
                logger.infoContext(id, "No Such exempt user with id: ", logMap);
            }
            return exemptUserDao;
        } catch (IncorrectResultSizeDataAccessException exception) {
            logger.errorContext(id, "Non-unique exempt user query result by id", exception, logMap);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, DATA_INTEGRITY_MESSAGE, exception);
        } catch (DataAccessException | MongoException exception) {
            logger.errorContext(id, "Failed to retrieve exempt user by id", exception, logMap);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, DATA_ACCESS_MESSAGE, exception);
        }
    }

    @Override
    public boolean isExemptUserByEmail(String email) {
        Map<String, Object> logMap = Map.of("email", email);
        try {
            return repository.existsByEmail(email);
        } catch (DataAccessException | MongoException exception) {
            logger.errorContext(email, "Failed to check exempt user existence by email", exception, logMap);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    EXISTENCE_CHECK_DATA_ACCESS_MESSAGE, exception);
        }
    }

    @Override
    public boolean isExemptUserByUserId(String userId) {
        Map<String, Object> logMap = Map.of("user_id", userId);
        try {
            return repository.existsByUserId(userId);
        } catch (DataAccessException | MongoException exception) {
            logger.errorContext(userId, "Failed to check exempt user existence by user id", exception, logMap);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    EXISTENCE_CHECK_DATA_ACCESS_MESSAGE, exception);
        }
    }

    @Override
    public boolean isExemptUserById(String id) {
        Map<String, Object> logMap = Map.of("id", id);
        try {
            return repository.existsById(id);
        } catch (DataAccessException | MongoException exception) {
            logger.errorContext(id, "Failed to check exempt user existence by id", exception, logMap);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    EXISTENCE_CHECK_DATA_ACCESS_MESSAGE, exception);
        }
    }
}
