# Opencv Practice #
I created this repository to practice opencv.

# Running #
This project requires the opencv library. I need to learn how to make a good
ant build. I have a slightly modified version of the eclipse build file
included. If you are lucky, the following may work to run the program.

    ant build
    ant run

I modified to the ant build to include the location of the opencv libaries on
my system, so you will likely have to modify the library path in the run target
of the build file. You can also try running the program from the command line
with

    java -cp /usr/share/java/opencv.jar:./bin -Djava.library.path="/usr/share/opencv/java/"  application.Main

in the directory where it was built, Replacing the classpath with the path to
opencv.jar and the library path to the folder containing the opencv output
files, which should include a shared object (libopencv_java310.so) on linux or
a dll on windows.
