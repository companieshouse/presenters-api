package uk.gov.companieshouse.presentersapi.TestUtils;

import uk.gov.companieshouse.presentersapi.model.dao.ExemptUserDao;
import java.util.List;

public class ExemptUserTestObjects {

    private static final String EXEMPT_USER_ID_1 = "123456789";
    private static final String EXEMPT_USER_EMAIL_1 = "demo1@companieshouse.gov.uk";

    private static final String EXEMPT_USER_ID_2 = "234567891";
    private static final String EXEMPT_USER_EMAIL_2 = "demo2@companieshouse.gov.uk";
    private static final String EXEMPT_USER_ID_3 = "345678912";
    private static final String EXEMPT_USER_EMAIL_3 = "demo3@companieshouse.gov.uk";

    public static List<ExemptUserDao> createExemptUserDaoList() {
        ExemptUserDao exemptUserDao1 = new ExemptUserDao();
        exemptUserDao1.setId(EXEMPT_USER_ID_1);
        exemptUserDao1.setEmail(EXEMPT_USER_EMAIL_1);
        ExemptUserDao exemptUserDao2 = new ExemptUserDao();
        exemptUserDao2.setId(EXEMPT_USER_ID_2);
        exemptUserDao2.setEmail(EXEMPT_USER_EMAIL_2);
        ExemptUserDao exemptUserDao3 = new ExemptUserDao();
        exemptUserDao3.setId(EXEMPT_USER_ID_3);
        exemptUserDao3.setEmail(EXEMPT_USER_EMAIL_3);

        return List.of(exemptUserDao1, exemptUserDao2, exemptUserDao3);
    }

}
