# Statement Matrix

| Version | Date | Description |
| ------- | ---- | ----------- |
| 1.0.0 | 16-12-26 | Initial version of the statement matrix |

This document will describe the design for the statement matrix. It will be versioned, so that changes can be tracked over time.

The statement matrix is data structure made of several yaml files, used to calculate the required data objects and statements for presenters.

## Definitions

### Presenter Types

These are the different roles a user could be presenting as, and represent the options given to a use on the "What role are you today?" page.

| ID | Description |
| -- | ----------- |
| officer-employee | An officer or employee filing on behalf of their firm, must be verified. |
| corporate-officer-employee | An officer or employee of a corporate officer filing on behalf of the firm, must be verified. |
| judicial-factor | A judicial factor appointed to act on behalf of the firm, must be verified. |
| officer-employee-same-group | An officer or employee of a firm in the same corporate group, must be verified. |
| official-receiver | An official receiver who is exempt from being verified. |
| acsp | The owner, officer or employee of an ACSP firm. |
| individual-filing-for-self | An individual filing a form for themselves. |
| individual-representing | An individual filing a form for another person. |

### Form Groups

All forms have been collected into groups that have the same presenter requirements.

| ID | officer-employee | other-verified-firm-type | official-receiver | acsp | individual-filing-for-self | individual-representing |
| -- | -- | -- | -- | -- | -- | -- |
| firm-delivery | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |
| individual-delivery | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| multi-delivery | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ |
| acsp-only | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| verified-individual-delivery | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| self-delivery | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| incorporation | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ |
| officer-only | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| wu-sub-forms | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ |
| rec-five | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| exempt-form | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

### Data Objects

| ID | Description | Source |
| -- | ----------- | ------ |
| personal-code | Personal code/UVID | Identity Verification API |
| name | Preferred Name (Forename Surname) | Identity Verification API or ACSP Manage Users API |
| date-of-birth | Date of Birth | Identity Verification API |
| email | Exempt or ACSP email address | Exempt Users Collection or Authentication Service |
| acsp-code | New ACSP Code | ACSP Manage Users API |
| acsp-name | ACSP registered name | ACSP Manage Users API |

### Delivery Statements

Statement describing on who's behalf the statement is delivered, includes the name of the firm or individual.

| ID | Description |
| -- | ----------- |
| delivery-on-behalf-of-firm | Statement declaring the form is delivered on behalf of a firm |
| delivery-on-behalf-of-individual | Statement declaring the form is delivered on behalf of another individual |

### Association Statements

Statement describing in what role the user is acting.

| ID | Description |
| -- | ----------- |
| officer-employee-statement | Statement declaring the presenter is an officer or an employee of the firm being delivered for |
| corporate-director-employee-statement | Statement declaring the presenter is an officer or an employee of a corporate officer of the firm being delivered for |
| judicial-factor-statement | Statement declaring the presenter is appointed as a judicial factor representing the firm being delivered for |
| officer-employee-same-group-statement | Statement declaring the presenter is an officer or an employee or an officer or an employee of a corporate officer of a firm in the same group as the firm being delivered for |
| official-receiver-statement | Statement declaring the presenter is exempt from IDV requirements as they are an official receiver |
| acsp-statement | Statement declaring the presenter is an owner, officer or employee of an ACSP |


### Verification Statements

Statement describing the user's verification status

| ID | Description |
| -- | ----------- |
| verified-statement | Statement declaring the user has verified their identity |


## Statement Matrix

The statement matrix is made up of seven yaml files.

| File | Description |
| ---- | ----------- |
| [form-group-to-form](./form-group-to-form.yaml) | Lists the forms in each form group |
| [form-group-to-presenter-types](./form-group-to-presenter-types.yaml) | Lists the presenter types possible for each form group |
| [presenter-type-to-data-obj](./presenter-type-to-data-obj.yaml) | Lists the data objects required for each presenter type |
| [presenter-type-to-assoc-statement](./presenter-type-to-assoc-statement.yaml) | Lists the association statement required for each presenter type |
| [presenter-type-to-verification-statement](./presenter-type-to-verification-statement.yaml) | Lists the verification statement required for each presenter type |
| [presenter-type-to-delivery-statement](./presenter-type-to-delivery-statement.yaml) | Lists the delivery statement required for each presenter type excluding ACSPs and official receivers |
| [form-group-to-delivery-statement](./form-group-to-delivery-statement.yaml) | Lists the delivery statements possible for each form group, used for ACSPs and official receivers |

## Process

The yaml files will be used in the three main Presenters API endpoints, the process for each is as follows

### Get Presenter Types

![Get Presenter Types](./plant-uml/Get%20Presenter%20Types.svg)

### Get Presenter Details for a Verified Individual

![Get Presenter Details Individual](./plant-uml/Get%20Details%20Individual.svg)

### Get Presenter Details for an ACSP

![Get Presenter Details Individual](./plant-uml/Get%20Details%20ACSP.svg)

### Get Presenter Details for an Officeial Receiver

![Get Presenter Details Individual](./plant-uml/Get%20Details%20Official%20Receiver.svg)

### Validation Endpoint

![Validation Endpoint](./plant-uml/Validation%20Endpoint.svg)

