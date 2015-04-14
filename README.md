# HIPI-Hadoop-opencv

Bulk image processing made simple. Face detection using HIPI and OpenCV

# What we used?

HIPI is an image processing library designed to be used with the ([Apache Hadoop MapReduce](http://hadoop.apache.org/)).

OpenCV (Open Source Computer Vision Library) is an open source computer vision and machine learning software library. OpenCV was built to provide a common infrastructure for computer vision applications and to accelerate the use of machine perception in the commercial products.

The library has more than 2500 optimized algorithms to solve real time problems image processing.

# How we done?

We have used HIPI example program (downloader and dumphib) and used OpenCV jar to process the face detection problem.

# Prerequisite

-ANT
-Hadoop ecosystem

# Quickstart

Build the two map reduce function manually by giving following command

* Run 'ant downloader' and 'ant dumphib'

The above comment will generate two jar files.

Move the list.txt file to hdfs "/user/hduser/hipi-hadoop/list.txt"

* Run the runDownloader.sh <%nodes%> parameter

if you are running in a single hadoop system %nodes% = 1

The first downloader will download all the image in the list file and merge it to single HIPI image bundle.

The second dumpHIB will use the OpenCV jar to detect the faces in the image and store the images in local / file server
