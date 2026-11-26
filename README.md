# presenters-api

The service provides presenter details and statements for CHS filings.

## Endpoints

The examples below show request paths relative to the service URL; authentication and authorisation must be
supplied by the calling environment.

| Endpoint | Description | Requirements | Example |
|---|---|---|---|
| `GET /presenters/presenter-types` | Returns the presenter types available to the authenticated user for the specified form. | - `form_type` query parameter<br>- OAuth2 user| `GET /presenters/presenter-types`<br>`?form_type=AD01` |
| `GET /presenters/presenter-details` | Returns the authenticated user's details and available statements for the specified form and presenter type. | - `form_type` and `presenter_type` query parameters<br>- OAuth2 user | `GET /presenters/presenter-details`<br>`?form_type=AD01`<br>`&presenter_type=officer-employee` |
| `POST /presenters/validation` | Checks whether the submitted data types and statement IDs form a valid combination for a permitted presenter type and form. Returns HTTP 200 for a valid combination, HTTP 400 for invalid request values, or HTTP 422 for a mismatch. | - Internal user API key<br>- JSON request body | `POST /presenters/validation`<br><br>`{`<br>`"form_type": "AD01",`<br>`"data_types": [`<br>`"personal-code", "name",`<br>`"date-of-birth"`<br>`],`<br>`"statement_ids": [`<br>`"officer-employee-statement",`<br>`"delivery-on-behalf-of-firm",`<br>`"verified-statement"`<br>`]`<br>`}` |

Configuration
-------------
System properties for presenters-api are defined in `application.properties`. These are normally configured per
environment.

Certain form types will be sent by presenters-api to FES while other form types will be emailed to the relevant org
unit. Properties relevant to either scenario are labelled accordingly.

| Variable                       | Description                                                                                                                                                         | Example            | Mandatory (always, email, FES, local) |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|---------------------------------------|
| ACCOUNTS_API_URL               | The Account API base URL used for account user lookups                                                                                                     |  | always                    |
| CHS_API_KEY                    | The internal api key for calling internal services                                                                                                                  | dummy-key          | always                                |
| DOCUMENT_API_LOCAL_URL         | A URL for calling the Document API. Unused by presenters-api but required by the SDK                                                                                | dummy-key          | always                                |
| FEATURE_FLAG_PRESENTERS_230926 | Enables the presenter REST endpoints only when set to `true`. Defaults to `false`; all endpoints are disabled (requests will respond with HTTP 404). Does not affect the healthcheck. | true               | no                                    |
| INTERNAL_API_URL               | The internal API base URL used for identity-verification and ACSP membership requests                                                                               |  | always                       |
| INTERNAL_CHS_API_KEY           | An api key for calling services without an internal privilege constraint                                                                                            | dummy-internal-key | always                                |
