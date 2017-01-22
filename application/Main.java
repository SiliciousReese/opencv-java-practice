package application;

import org.opencv.core.Core;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;

public class Main extends Application {

	/** Standard Javafx startup. */
	@Override
	public void start(Stage primaryStage) {
		try {

			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("Sample.fxml"));
			BorderPane root = (BorderPane) loader.load();
			Scene scene = new Scene(root, 400, 400);
			Controller controller = loader.getController();

			scene.getStylesheets().add(
					getClass().getResource("application.css").toExternalForm());

			primaryStage.setScene(scene);
			primaryStage.show();

			primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
				public void handle(WindowEvent we) {
					controller.setClosed();
				}
			}));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Load opencv library and launch javafx. */
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		launch(args);
	}
}
