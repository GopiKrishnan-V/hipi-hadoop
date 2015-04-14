#!/bin/bash
#ant -f ../build.xml downloader
hadoop jar downloader.jar /user/hduser/fidelity/list.txt /user/hduser/fidelity/output.hib $1
echo "Finished building HIPI Image bundle"
hadoop jar dumphib.jar -Dmapred.child.env=”JAVA_LIBRARY_PATH=YOUR_JAVA_LIB” -Djava.library.path=YOUR_JAVA_LIB -libjars PATH_TO_opencv-249.jar /user/hduser/fidelity/output.hib /user/hduser/fidelity/haarcascade_frontalface_default.xml /user/hduser/fidelity/dump/
