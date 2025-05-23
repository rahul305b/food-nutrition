# Rahul Shankar's Food Nutrition Calculator for Dialysis Patients 🥗

This is a JavaFX-based desktop application designed to help dialysis patients track the potassium content in their meals. The application allows users to add ingredients, specify their weight, and instantly view the total potassium content of the dish.

---

## 🛠 Features

- Enter dish name and add multiple ingredients.
- Calculate potassium content based on the weight of each ingredient.
- Prevent duplicate ingredient entries in a single dish.
- Delete selected ingredients and re-enable them for future selection.
- View real-time updates of total potassium content.
- User-friendly interface with JavaFX TableView and ComboBox.

---

## 💡 Technologies Used

- Java 17+
- JavaFX
- FXML
- Maven

---

## 🚀 How to Run

### Prerequisites

- JDK 17 or higher installed
- Maven installed
- JavaFX SDK (configured if needed)

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/food-nutrition-dialysis.git
   cd food-nutrition-dialysis

2.Run the application using Maven:
-mvn clean javafx:run
ℹ️ Make sure your pom.xml includes the javafx-maven-plugin.

📁 Project Structure

src/
 └── main/
     ├── java/
     │    └── com.rahulshankar.dialysisapp/
     │          └── controllers/
     │               └── FoodPreparationController.java
     ├── resources/
     │    └── fxml/
     │         └── FoodPreparationUI.fxml
     └── App.java (Main application entry)


🧠 How It Works
Each ingredient has a predefined potassium value per 100g.

When the user adds an ingredient and specifies weight, potassium is calculated as:

Copy
Edit
(PotassiumPer100g * Weight) / 100
All added ingredients are shown in a table.

The total potassium is dynamically calculated on submission.

🙋‍♂️ Author
Rahul Shankar
🔗 LinkedIn
📧 https://www.linkedin.com/in/rahul-s-6a9a3a91/

