package guessmarket.ui;

import guessmarket.engine.EngineImpl;
import guessmarket.engine.GuessMarketEngine;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/guessmarket/ui/main.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        GuessMarketEngine engine = new EngineImpl();
        controller.setEngine(engine);

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Guess Market");
        stage.show();
    }
}