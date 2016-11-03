package hipi.examples.dumphib;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.UUID;	

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.CascadeClassifier;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
 
import org.apache.hadoop.util.ToolRunner;

import hipi.image.FloatImage;
import hipi.image.ImageHeader;
import hipi.image.io.JPEGImageUtil;
import hipi.image.io.PPMImageUtil;
import hipi.imagebundle.mapreduce.ImageBundleInputFormat;
import hipi.util.ByteUtils;

public class DumpHib extends Configured implements Tool {

	/**
	 * Mapper class for detecting face in the images available in the HIPI Bundle
	 */
	public static class DumpHibMapper extends Mapper<ImageHeader, FloatImage, IntWritable, Text> {
		private static Configuration conf;
		
		public void setup(Context context) throws IOException
		{
			conf = context.getConfiguration();
			System.loadLibrary("opencv_java249");
		}
		
		@Override
		public void map(ImageHeader key, FloatImage value, Context context)
		throws IOException, InterruptedException {
			if (value != null) {
				String localFilePath = conf.get("local.file.path");
				CascadeClassifier faceDetector = new CascadeClassifier(localFilePath.toString()+"haarcascade_frontalface_default.xml");
				String randomFile = UUID.randomUUID().toString();
				
				File file = new File(localFilePath.toString()+"imwf/"+randomFile+".jpg");
				OutputStream os = new FileOutputStream(file);
				JPEGImageUtil.getInstance().encodeImage(value, key, os);
				
				Mat matImage = Highgui.imread(file.getAbsolutePath());
				MatOfRect faceDetections = new MatOfRect();
				
				faceDetector.detectMultiScale(matImage,faceDetections);
				int totalFaces = faceDetections.toArray().length;
				
				if(totalFaces ==0){
					file.renameTo(new File(localFilePath.toString()+"imwof/" + file.getName()));
				}
				
				int imageWidth = value.getWidth();
				int imageHeight = value.getHeight();
				
				String hexHash = ByteUtils.asHex(ByteUtils.FloatArraytoByteArray(value.getData()));
				/// It will write the image information for reducer
				if(totalFaces > 0){ //(mageWidth > 540 && imageHeight > 300)
					String output = imageWidth + "x" + imageHeight + "\t(" + hexHash + ")\t	" + randomFile+".jpg";
					context.write(new IntWritable(1), new Text(output));	
				}else{
					String output = imageWidth + "x" + imageHeight + "\t(" + hexHash + ")\t	" + "No face detected found "+randomFile+ ".jpg";
					context.write(new IntWritable(1), new Text(output));
				}
			}
		}
	}
	
	/**
	 * Reducer class to combine the result of image dimenstions 
	 */
	public static class DumpHibReducer extends Reducer<IntWritable, Text, IntWritable, Text> {
		public void reduce(IntWritable key, Iterable<Text> values, Context context) 
		throws IOException, InterruptedException {
			for (Text value : values) {
				context.write(key, value);
			}
		}
	}

	/**
	 * Configure the setting for Job
	 */
	public int run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		if (args.length < 3) {
			System.out.println("Usage: dumphib <input hib> <facedetection> <outputdir>");
			System.exit(0);
		}
		String inputPath = args[0];
		String cascadeClassifier = args[1];
		
		String outputPath = args[2];
		conf.setStrings("cascade.classifier", cascadeClassifier);
		String localFilePath = "/home/user/JettyMavenHelloWorld/src/main/webapp/images/output/"; // Give path for your local system
		conf.setStrings("local.file.path",localFilePath);
		Job job = new Job(conf, "dumphib");
		job.setJarByClass(DumpHib.class);
		job.setMapperClass(DumpHibMapper.class);
		job.setReducerClass(DumpHibReducer.class);
		// Set formats
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		job.setInputFormatClass(ImageBundleInputFormat.class);
		File file = new File(localFilePath);
		delete(file);
		// Set out/in paths
		removeDir(outputPath, conf);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		FileInputFormat.setInputPaths(job, new Path(inputPath));	
		job.setNumReduceTasks(1);
		System.exit(job.waitForCompletion(true) ? 0 : 1);
		return 0;
	}

	/**
	 * Main function intiated from command line. Accepts the arguments passed to the jar
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		  Configuration conf = new Configuration();
		int exitCode = ToolRunner.run(conf,new DumpHib(), args);
		System.exit(exitCode);
	}

	/**
	 * Remove files in the hdfs system
	 * @param path
	 * @param conf
	 * @throws IOException
	 */
	public static void removeDir(String path, Configuration conf) throws IOException {
		Path output_path = new Path(path);
		FileSystem fs = FileSystem.get(conf);
		if (fs.exists(output_path)) {
			fs.delete(output_path, true);
		}
	}
	
	/**
	 * Delete the file in the directory
	 * @param file
	 * @throws IOException
	 */
	public static void delete(File file) 	throws IOException{
    	if(file.isDirectory()){
    		//directory is empty, then delete it
    		if(file.list().length!=0){
    		   //list all the directory contents
        	   String files[] = file.list();
        	   for (String temp : files) {
        	      //construct the file structure
        	      File fileDelete = new File(file, temp);
        	      //recursive delete
        	     delete(fileDelete);
        	   }
    		}
    	}else{
    		//if file, then delete it
    		file.delete();
    		System.out.println("File is deleted : " + file.getAbsolutePath());
    	}
    }
}
