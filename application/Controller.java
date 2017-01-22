package application;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

	private static final double FACE_RECOG_SCALE_FACTOR = 1.3;
	private static final int FACE_RECOG_MIN_NEIGHBOR = 3;

	/** Path to the cascade for a face. */
	private static final String FACE_CASCADE_PATH = "/usr/share/opencv/haarcascades/haarcascade_frontalface_default.xml";

	/** The color of the rectangle to draw around each face in RGB format. */
	private static final Scalar FACE_RECT_COL = new Scalar(255, 255, 255);

	/** the id of the camera to be used */
	private static final int CAMERA_ID = 0;

	/** Button to start and stop capturing video from video device. */
	@FXML
	private Button captureButton;

	/** Button to start training the algorithm. */
	@FXML
	private Button trainButton;

	/** Viewer for images from the video device. */
	@FXML
	private ImageView imageViewer;

	/** A timer for getting the video stream. */
	private ScheduledExecutorService timer;

	/** the OpenCV object that realizes the video capture */
	private VideoCapture capture = new VideoCapture();

	/** a flag to change the button behavior */
	private boolean cameraActive = false;

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
				private BufferedImage image;
				private byte[] imageDataBufferByte;

				private boolean firstRun = true;

				@Override
				public void run() {
					/* read the current frame. */
					capture.read(frame);

					/*
					 * Convert to a known colorscheme. Gray is used to reduce
					 * memory usage and hopefully speed up object detection.
					 */

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
					}

					/* Use a face cascade to search the image. */
					new CascadeClassifier(FACE_CASCADE_PATH).detectMultiScale(
							frame, rects, FACE_RECOG_SCALE_FACTOR,
							FACE_RECOG_MIN_NEIGHBOR, 0, new Size(), new Size());

					/*
					 * Here is the code that handles face recognition. To simply
					 * detect whether or not any faces are on the screen, just
					 * put the code you want to run before the loop.
					 */
					for (Rect rect : rects.toArray()) {
						/* Draw a rectangle around each face. */
						Imgproc.rectangle(frame, rect.tl(), rect.br(),
								FACE_RECT_COL);
					}

					/*
					 * Convert the mat to a buffered image that can be used by
					 * JavaFX. This is done by copying the raw byte array from
					 * the mat to the buffered images data byte array.
					 */
					frame.get(0, 0, imageDataBufferByte);

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
			captureButton.setText("Stop Camera");
		} else {
			/* the camera is not active at this point. */
			cameraActive = false;
			/* update again the button content. */
			captureButton.setText("Start Camera");

			stopCamera(timer, capture);
		}
	}

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

	/** Method Called when train button is pressed. */
	@FXML
	protected void train() {

	}

	protected void setClosed() {
		stopCamera(timer, capture);
	}
}
