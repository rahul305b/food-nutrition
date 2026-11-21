# Renal Food Nutrition Calculator

A JavaFX desktop application for estimating the potassium, phosphorus and sodium contributed by the ingredients in a dish. It is intended as a calculation and tracking aid for people who need to monitor dietary minerals, including people receiving dialysis.

## Current features

- Enter a dish name and add multiple food ingredients.
- Enter the weight of each ingredient in grams.
- Calculate potassium, phosphorus and sodium for each ingredient and for the complete dish.
- Remove ingredients and add them again; duplicate ingredient rows are prevented.
- Maintain a local food catalogue: add, update and delete foods and nutrient values per 100 g.
- Persist the food catalogue automatically in a local SQLite database.
- Treat unentered nutrient values as unknown. If any ingredient is missing a value for a nutrient, the dish total for that nutrient is shown as **Incomplete data**, not as zero.

## Technology

- Java 23
- JavaFX 23
- Maven
- SQLite (local database)

## Run

Prerequisites: JDK 23 and Maven.

From the foodnutrition directory, run:

    mvn clean javafx:run

The SQLite database is created automatically under the current user's home directory:

- Windows: %USERPROFILE%\.foodnutrition\foodnutrition.db
- Linux/macOS: ~/.foodnutrition/foodnutrition.db

No PostgreSQL server, database username or password is needed.

## Food data

The application seeds the three potassium values already present in the original project (onion, tomato and potato). These are retained for backward continuity and are **not certified clinical reference values**. Phosphorus and sodium are left blank until verified data is entered.

Use **Manage Food Catalogue** to add or correct foods. All nutrient entries must be in milligrams per 100 grams of edible food. Leave a field blank when its value is not known; do not enter zero as a substitute for missing data.

Food composition varies with variety, brand, edible portion, preparation and cooking method. Verify catalogue values against a reliable food-composition source and have a renal dietitian or treating clinician review any dietary use.

## Scope and safety

This program performs arithmetic using food values supplied by the user. It does not prescribe a renal diet, set personal nutrient limits, model cooking losses, or replace advice from a renal dietitian or clinician. Calculated values are estimates and should not be used as the sole basis for treatment or dietary decisions.

## Data and privacy

Food catalogue data is stored locally on the computer under the current user's home directory. The app does not upload food entries or meal information to a server.

## Author

Rahul Shankar — https://github.com/rahul305b