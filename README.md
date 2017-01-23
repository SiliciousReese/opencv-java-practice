# Opencv Practice #
I created this repository to practice opencv.

# Running #
This project requires the opencv library. I need to learn how to make a good
ant build. Until then, I will include the eclipse generated buildfile, which
may be of help in trying to run this yourself.

I was able to run the program on my computer by running the build script and
then running java with the command 

    java -cp /usr/share/java/opencv.jar:./bin -Djava.library.path="/usr/share/opencv/java/"  application.Main

from the directory where it was built. 
