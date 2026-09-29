# presenters-api
The service provides presenter details and statements for CHS filings.

Configuration
-------------
System properties for efs-submission-api are defined in `application.properties`. These are normally configured per environment.

Certain form types will be sent by efs-submission-api to FES while other form types will be emailed to the relevant org unit. Properties relevant to either scenario are labelled accordingly.

| Variable                                     | Description                                                                                                                                 | Example                                             |Mandatory (always, email, FES, local)|
----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------|--------|
 CHS_API_KEY                     | The internal api key for calling internal services                                                            | dummy-key                                  |always
 INTERNAL_CHS_API_KEY                        | An api key for calling services without an internal privilege constraint                                                       | dummy-internal-key                                  |always
