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
## Demo food profile importer

The demo importer is intended for software testing and UI demonstrations only. It stores records in a separate SQLite database:

- `~/.foodnutrition/foodnutrition-demo.db`

It does **not** add demo records to the food catalogue used by the nutrition calculator. Demo profile data must not be used for dietary planning, clinical decisions, or patient care.

Open **Manage Food Catalogue** and use **View Imported Profiles** to view imported demo records. The profile window provides a confirmed delete-all action that removes demo records only.

### CSV format

The importer requires these headers (the column order can vary):

`demo_id,food_name,local_name_tamil,category,preparation,basis,energy_kcal,protein_g,carbohydrates_g,fat_g,fibre_g,potassium_mg,phosphorus_mg,sodium_mg,calcium_mg,magnesium_mg,iron_mg,data_source,source_record_id,data_status,citation,license_note`

- Each `demo_id` must be unique within the imported database.
- Nutrient values must be finite, non-negative numeric values or blank (unknown).
- Blank and `N/A` values are treated as unknown.
- The importer validates the required headers and row field count.
- Duplicate IDs are skipped.
- The demo database is independent of `foodnutrition.db`.

**Safety notice:** Demo data values may be fabricated. They are explicitly marked as synthetic test data and must never be interpreted as nutrition guidance.
