# GePi WebApp

NOTE: Edits to the README.md file must be done to the file in the `readme-raw` folder! The README.md file in the project root is a Maven-filtered copy of the file in `readme-raw` for automatic insertion of the current version. 

## Webapp Deployment

GePi is basically a Java servlet web application. Realized with Apache Tapestry, it follows the conventions of packaging into a WAR file which is then deployed into a servlet container, e.g. Jetty or Tomcat. For details, refer to the `README.md` in the parent `gepi` directory.


## Required Python Packages for Result Excel Sheet Creation

* openpyxl v3.0.9
* xlsxwriter v3.0.1

## Development

Some notes and hints useful in development of the Web application.

### Adding elements to the input form

New elements must be taken into account in the following places:
* `GepiInput#reset()`
* `GepiQueryParameters` class
* `gepiinput.js#setupInputExamples`

After the addition of a component it is a good idea to issue a search and then go back to the input form and hit the `Clear input` button. Through ajax-reloading the element IDs are updated with random numbers unless the `t:clientId` attribute is defined. However, this does not seem to work on radio buttons where `t:clientId` ID is not used as the actual ID but rendered as `clientid="..."` attribute. So this is better checked not to throw errors.