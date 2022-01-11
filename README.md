# GPDM

## Description

This repository is part of the overarching CatenaX project.

GPDM is a german abbreviation for Geschäfts-Partner-Daten-Management (business partner data management). 
This project lets other CatenaX services query, add and change information on CatenaX business partners.

## How to run

GPDM is a SpringBoot Kotlin software project managed by Maven. 

The project can be run with the following command:`mvn clean spring-boot:run`

On default configuration the project initializes and starts an in-memory H2 database. 
After the project has finished initializing you can access the database on `http://localhost:8080/h2-console/` 
as per default configuration. You can find the standard user and password as well as further database configurations 
int the `application.properties` file in the resource folder.

### Prerequisites

1. Maven
2. JDK11



## Repository Structure

The root of the project is reserved for basic repository files and the Maven project file (pom.xml).
The source folder is split between test and application files. Source code files are in the kotlin subdirectory (analogous to java source folders). 
Additional files such as configuration files can be found in the resources subdirectory.

As per Spring framework's default structure the domain model and persistence object information is encapsulated in entities. 
Each entity in the project derives from the `BaseEntity` type which contains standard fields/columns such as identifier 
and timestamps.