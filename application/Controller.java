package application;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

/**
 * This code is a modified version of code from a computer vision course.
 * original code: https://github.com/opencv-java/getting-started
 */
public class Controller {
	/**
	 * Time in milliseconds to wait before getting another frame from the
	 * camera.
	 */
	private static final int REFRESH_IMAGE_DELAY_MILIS = 50;

	/* Both ready messages should match the message on the button at startup. */

	/** The message to display when video is not being capture. */
	private static final String VID_CAP_BUT_READY_MESSAGE = "Capture Video";
	/** The message to display when video is being capture. */
	private static final String VID_CAP_BUT_STOP_MESSAGE = "Cancel Capture";

	/** The message to display when not searching an image. */
	private static final String IMG_PROC_BUT_READY_MESSAGE = "Search Image";
	/** The message to display when searching an image. */
	private static final String IMG_PROC_BUT_STOP_MESSAGE = "Cancel Search";

	/** The color of the rectangle to draw around each detected object in RGB format. */
	private static final Scalar OBJ_RECT_COL = new Scalar(255, 255, 255);

	/** the id of the camera to be used */
	private static final int CAMERA_ID = 0;

	/** Button to start and stop capturing video from video device. */
	@FXML
	private Button videoCaptureButton;

	/** Button to process an image. */
	@FXML
	private Button processImageButton;

	/** Viewer for images from the video device. */
	@FXML
	private ImageView imageViewer;

	/** Get user input for the path to the image. */
	@FXML
	private TextField imgLoc;

	/** Get user input for the location of the cascade file. */
	@FXML
	private TextField cascLoc;

	/** User input for the scale value in object detection. */
	@FXML
	private TextField detectScale;

	/** User input for the minimum number of neihbors for object detection. */
	@FXML
	private TextField detectMinNeighbor;

	/** User input number of times to shrink the image. */
	@FXML
	private TextField numShrinks;

	/** User input number of times to grow the image. */
	@FXML
	private TextField numGrows;

	/** Output to show the number of objects detected. */
	@FXML
	private Label numDetected;

	/** a flag to change the button behavior */
	private boolean cameraActive = false;

	/** A flag for whether an image is currently being prcoessed. */
	private boolean isProscessing = false;

	/** A timer for getting the video stream. */
	private ScheduledExecutorService timer;

	/** the OpenCV object that realizes the video capture */
	private VideoCapture capture = new VideoCapture();

	/** Start image processing in a seperate thread. */
	private ExecutorService detectorExecutor = Executors
			.newSingleThreadExecutor();

	/** Used to kill the image processing thread if it does not finish. */
	private Future<?> processImageTask;

	/** Method called when capture button is pressed. */
	@FXML
	protected void capture() {
		if (!cameraActive) {
			/* Start the video capture. */
			capture.open(CAMERA_ID);

			cameraActive = true;

			/* Grab a frame periodically. */
			Runnable frameGrabber = new Runnable() {
				/** Only create the large objects once. */
				private final Mat frame = new Mat();
				private final MatOfRect rects = new MatOfRect();
				private final CascadeClassifier cascadeClassifier = new CascadeClassifier();
				private BufferedImage image;
				private byte[] imageDataBufferByte;

				private boolean firstRun = true;

				@Override
				public void run() {
					double recogScaleFactor = Double
							.parseDouble(detectScale.getText());

					int recogMinNeighbor = Integer
							.parseInt(detectMinNeighbor.getText());
					
					String cascPath = cascLoc.getText();

					/* read the current frame. */
					capture.read(frame);

					cascadeClassifier.load(cascPath);

					/*
					 * This assumes the image size will never change, so it is
					 * safe to use the same buffered image object every time.
					 */
					if (firstRun) {
						image = new BufferedImage(frame.width(), frame.height(),
								BufferedImage.TYPE_3BYTE_BGR);

						imageDataBufferByte = ((DataBufferByte) image
								.getRaster().getDataBuffer()).getData();

						firstRun = false;

						imageViewer.setFitWidth(image.getWidth());
						imageViewer.setFitHeight(image.getHeight());

						recogScaleFactor = Double
								.parseDouble(detectScale.getText());
						recogMinNeighbor = Integer
								.parseInt(detectMinNeighbor.getText());
					}

					/* Use a face cascade to search the image. */
					cascadeClassifier.detectMultiScale(frame, rects,
							recogScaleFactor, recogMinNeighbor, 0, new Size(),
							new Size());

					/*
					 * Here is the code that handles face recognition. To simply
					 * detect whether or not any faces are on the screen, just
					 * put the code you want to run before the loop.
					 */
					for (Rect rect : rects.toArray()) {
						/* Draw a rectangle around each face. */
						Imgproc.rectangle(frame, rect.tl(), rect.br(),
								OBJ_RECT_COL);
					}

					/*
					 * Convert the mat to a buffered image that can be used by
					 * JavaFX. This is done by copying the raw byte array from
					 * the mat to the buffered images data byte array.
					 */
					frame.get(0, 0, imageDataBufferByte);

					numDetected.setText(
							new Integer(rects.toArray().length).toString());

					/*
					 * Copy the BufferedImage to the Swing ImageViewer using the
					 * JavaFX thread.
					 */
					Platform.runLater(() -> {
						imageViewer.imageProperty()
								.set(SwingFXUtils.toFXImage(image, null));
					});
				}
			};

			timer = Executors.newSingleThreadScheduledExecutor();
			timer.scheduleAtFixedRate(frameGrabber, 0,
					REFRESH_IMAGE_DELAY_MILIS, TimeUnit.MILLISECONDS);

			/* update the button content. */
			videoCaptureButton.setText(VID_CAP_BUT_STOP_MESSAGE);
		} else {
			/* the camera is not active at this point. */
			cameraActive = false;
			/* update again the button content. */
			videoCaptureButton.setText(VID_CAP_BUT_READY_MESSAGE);

			stopCamera(timer, capture);
		}
	}

	/**
	 * Stop the timer and close the video camera stream.
	 * 
	 * @param timer
	 *            The timer to stop.
	 * @param capture
	 *            The device to stop capturing from.
	 */
	private static void stopCamera(ScheduledExecutorService timer,
			VideoCapture capture) {
		if (timer != null && !timer.isShutdown()) {
			timer.shutdown();
			try {
				/* stop the timer. */
				timer.awaitTermination(REFRESH_IMAGE_DELAY_MILIS,
						TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				System.err.println(
						"Exception in stopping the frame capture, trying to release the camera now... "
								+ e);
			}
		}

		if (capture.isOpened()) {
			/* release the camera. */
			capture.release();
		}
	}

	/** Method Called when process image button is pressed. */
	@FXML
	protected void processImage() {
		/*
		 * These can use a lot of memory, make sure they are only created once.
		 */

		/** Stores the image data for processing by opencv. */
		final Mat image = new Mat();

		/** Used to find objects in an image. */
		final CascadeClassifier objectFinder = new CascadeClassifier();

		/** Stores the location of the objects in the image. */
		final MatOfRect signs = new MatOfRect();

		/*
		 * This has to be called occasionally, java does not correctly garbage
		 * collect some of the opencv objects which can lead to significant
		 * memory leaks.
		 */
		System.gc();

		if (!isProscessing) {
			isProscessing = true;

			/*
			 * Create a new, single-threaded executor every time the method is
			 * called. It is shutdown at the end of the method.
			 */
			detectorExecutor = Executors.newSingleThreadExecutor();

			/* Update the image button before starting the new thread. */
			processImageButton.setText(IMG_PROC_BUT_STOP_MESSAGE);

			/** Use a lambda expression to pass a Runnable to the executor. */
			processImageTask = detectorExecutor.submit(() -> {
				/*
				 * The maximum height and width of the image on the user
				 * interface.
				 */
				final int imgMaxWidth = 1600;
				final int imgMaxHeight = 700;

				/*
				 * The path to the image to process and the cascade file to
				 * process it with.
				 */
				String imageLoc = imgLoc.getText();
				String cascadeLoc = cascLoc.getText();

				/*
				 * The scale factor and minimum number of neighbors for object
				 * recognition.
				 */
				double scale = Double.parseDouble(detectScale.getText());
				int minNeighb = Integer.parseInt(detectMinNeighbor.getText());

				/* The number of times to shrink and grow the image. */
				int numShrinksInt = Integer.parseInt(numShrinks.getText());
				int numGrowsInt = Integer.parseInt(numGrows.getText());

				/* The height and width of the image on the user interface. */
				int imageWidth = 50;
				int imageHeight = 50;

				/* The buffered image object to send to javafx. */
				BufferedImage outImage;

				/* Read on image from the path the user entered. */
				Imgcodecs.imread(imageLoc).copyTo(image);

				/* Load the cascade file. */
				objectFinder.load(cascadeLoc);

				/* Shrink the image the given number of timer. */
				for (int i = 0; i < numShrinksInt; i++) {
					Imgproc.pyrDown(image, image);
				}

				/*
				 * This can use a lot of processor time and memory depending on
				 * the image.
				 */
				objectFinder.detectMultiScale(image, signs, scale, minNeighb, 0,
						new Size(), new Size());

				/* Draw a rectangle around each recognized object. */
				for (Rect rect : signs.toArray()) {
					Imgproc.rectangle(image, rect.tl(), rect.br(),
							OBJ_RECT_COL);
				}

				/*
				 * Grow the image. This makes it easier to see some of the
				 * smaller images.
				 */
				for (int i = 0; i < numGrowsInt; i++) {
					Imgproc.pyrUp(image, image);
				}

				outImage = new BufferedImage(image.cols(), image.rows(),
						BufferedImage.TYPE_3BYTE_BGR);

				/*
				 * Copy the image matrix to the byte array of the BufferedImage.
				 */
				image.get(0, 0,
						((DataBufferByte) outImage.getRaster().getDataBuffer())
								.getData());

				/* Get the height and width of the image. */
				imageWidth = outImage.getWidth();
				imageHeight = outImage.getHeight();

				/*
				 * Make sure the height and width do not stretch outside of
				 * their maximum values.
				 */
				imageWidth = imageWidth > imgMaxWidth
						? imgMaxWidth
						: imageWidth;
				imageHeight = imageHeight > imgMaxHeight
						? imgMaxHeight
						: imageHeight;

				imageViewer.setFitWidth(imageWidth);
				imageViewer.setFitHeight(imageHeight);

				/*
				 * Convert the buffered image which has the data from the image
				 * mat into an Image for javafx and show it on the image viewer.
				 */
				imageViewer.imageProperty()
						.set(SwingFXUtils.toFXImage(outImage, null));

				/* Use the javafx thread for setting the button text. */
				Platform.runLater(() -> {
					/* Update the number of detected objects. */
					numDetected.setText(
							new Integer(signs.toArray().length).toString());

					processImageButton.setText(IMG_PROC_BUT_READY_MESSAGE);
				});

				isProscessing = false;
			});

			/* Nothing else should be done in the thread. */
			detectorExecutor.shutdown();
		} else {
			/* Try to kill the process, usually unsuccesfully. */
			detectorExecutor.shutdownNow();

			/* Actually kill the process. */
			processImageTask.cancel(true);

			/*
			 * Reset the button text so the program knows it is ready to start
			 * again.
			 */
			processImageButton.setText(IMG_PROC_BUT_READY_MESSAGE);

			isProscessing = false;
		}
	}

	protected void setClosed() {
		stopCamera(timer, capture);
	}
}
