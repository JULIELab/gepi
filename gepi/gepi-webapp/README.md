# GePi WebApp

NOTE: Edits to the README.md file must be done to the file in the `readme-raw` folder! The README.md file in the project root is a Maven-filtered copy of the file in `readme-raw` for automatic insertion of the current version. 

## Webapp Deployment

GePi is basically a Java servlet web application. Realized with Apache Tapestry, it follows the conventions of packaging into a WAR file which is then deployed into a servlet container, e.g. Jetty or Tomcat. For details, refer to the `README.md` in the parent `gepi` directory.


## Required Python Packages for Result Excel Sheet Creation

* openpyxl v3.0.9
* xlsxwriter v3.0.1
