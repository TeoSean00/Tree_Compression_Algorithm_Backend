package com.example.demo.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.example.demo.services.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class AlgorithmController {
  private HashMap<String, Class<? extends Utility>> algorithmMap = new HashMap<>();

  // Default constructor to initialise the algorithm map
  public AlgorithmController() {
    algorithmMap.put("Original Bad Algorithm", UtilityOriginalBad.class);
    algorithmMap.put("Colour Quant and Huffman Algorithm - Quant 512", UtilityColourHuffman512.class);
    algorithmMap.put("Image Dithering and Huffman Algorithm", UtilityDitheringHuffman.class);
    algorithmMap.put("Colour Quant and Huffman Algorithm - Quant 256", UtilityColourHuffman256.class);
    // algorithmMap.put("Colour Quant and Huffman Algorithm - String Array", UtilityColourHuffmanString.class);
    algorithmMap.put("Huffman Algorithm", UtilityHuffmanOnly.class);
  }


  // Endpoint for backend health check
  @GetMapping("/healthcheck")
  public String healthCheck(
    @RequestParam(name = "algo1", required = true) String algo1,
    @RequestParam(name = "algo2", required = true) String algo2
  ) {
    System.out.println("algo1: " + algo1 + ", algo2: " + algo2);
    return String.format("algo1: %s, algo2: %s sent to the backend", algo1, algo2);
  }


  // Endpoint to run the original provided algorithm and get its results
  @GetMapping("/original-algorithm")
  public HashMap<String, ArrayList<String>> getTestAlgorithm() {

    HashMap<String, ArrayList<String>> imageResults = new HashMap<String, ArrayList<String>>();

    try {
      System.out.println("test original algorithm now");

      UtilityOriginalBad Utility = new UtilityOriginalBad();

      String ImageDirectory = "src/main/java/com/example/demo/images/";
      File directory = new File(ImageDirectory);
      File[] files = directory.listFiles();

      if (files != null) {
        System.out.println("files not null");
        for (File file : files) {
          if (file.isFile()) {
            System.out.println("file is of type file");
            ArrayList<String> imageInfo = new ArrayList<>();
            String imageName = file.getName();

            // Converting image to pixels
            ImagetoPixelConverter ImagetoPixelConverter = new ImagetoPixelConverter(ImageDirectory + imageName);

            // Converting the image to pixels
            int[][][] pixelData = ImagetoPixelConverter.getPixelData();
            int width = ImagetoPixelConverter.getWidth();
            int height = ImagetoPixelConverter.getHeight();

            for (int x = 0; x < width; x++) {
              for (int y = 0; y < height; y++) {
                int red = pixelData[x][y][0];
                int green = pixelData[x][y][1];
                int blue = pixelData[x][y][2];
              }
            }

            // Compression
            // Define location and name for the compressed file to be created
            String compressed_file_name = "src/main/java/com/example/demo/compressed/"
                + imageName.substring(0, imageName.lastIndexOf('.')) + ".bin";

            // start compress timer
            long compressStartTime = System.currentTimeMillis();

            // call compress function
            Utility.Compress(pixelData, compressed_file_name);

            // end timer for compress and record the total time passed
            long compressEndTime = System.currentTimeMillis();
            long compressExecutionTime = compressEndTime - compressStartTime;
            System.out
                .println("Compress Execution Time for " + imageName + " : " + compressExecutionTime + " milliseconds");
            String compressTime = ("Compress Execution Time for " + imageName + " : " + compressExecutionTime
                + " milliseconds");
            imageInfo.add(compressTime);

            // Check the original file size
            File originalFile = new File(ImageDirectory + imageName);
            long originalFileSize = originalFile.length();
            System.out.println("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            String originalSize = ("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            imageInfo.add(originalSize);

            // Check size of the compressed file
            File compressedFile = new File(compressed_file_name);
            long compressedFileSize = compressedFile.length();
            System.out.println("Size of the compressed file for " + imageName + ": " + compressedFileSize + " bytes");
            String compressedSize = ("Size of the compressed file for " + imageName + ": " + compressedFileSize
                + " bytes");
            imageInfo.add(compressedSize);

            // Find the Difference
            long differenceInFileSize = originalFileSize - compressedFileSize;
            System.out.println("Bytes saved from compression of " + imageName + ": " + differenceInFileSize + " bytes");
            String bytesSaved = ("Bytes saved from compression of " + imageName + ": " + differenceInFileSize
                + " bytes");
            imageInfo.add(bytesSaved);

            // Decompression
            // start decompress timer
            long decompressStartTime = System.currentTimeMillis();

            // call decompress function
            int[][][] newPixelData = Utility.Decompress(compressed_file_name);

            // end timer for decompress and record the total time passed
            long decompressEndTime = System.currentTimeMillis();
            long decompressExecutionTime = decompressEndTime - decompressStartTime;
            System.out.println(
                "Decompress Execution Time for " + imageName + " : " + decompressExecutionTime + " milliseconds");
            String decompressTime = ("Decompress Execution Time for " + imageName + " : " + decompressExecutionTime
                + " milliseconds");
            imageInfo.add(decompressTime);

            // convert back to image for visualisation
            PixeltoImageConverter PixeltoImageConverter = new PixeltoImageConverter(newPixelData);
            PixeltoImageConverter.saveImage("src/main/java/com/example/demo/decompressed/" + imageName, "png");

            // Get the two bufferedimages for calculations
            BufferedImage originalimage = ImageIO.read(new File(ImageDirectory + imageName));
            BufferedImage decompressedimage = ImageIO
                .read(new File("src/main/java/com/example/demo/decompressed/" + imageName));

            // calculate MAE
            double MAE = MAECalculator.calculateMAE(originalimage, decompressedimage);
            System.out.println("Mean Absolute Error of :" + imageName + " is " + MAE);
            String MAEString = ("Mean Absolute Error of :" + imageName + " is " + MAE);
            imageInfo.add(MAEString);

            // calculate MSE
            double MSE = MSECalculator.calculateMSE(originalimage, decompressedimage);
            System.out.println("Mean Squared Error of :" + imageName + " is " + MSE);
            String MSEString = ("Mean Squared Error of :" + imageName + " is " + MSE);
            imageInfo.add(MSEString);

            // calculate PSNR
            double PSNR = PSNRCalculator.calculatePSNR(originalimage, decompressedimage);
            System.out.println("PSNR of :" + imageName + " is " + PSNR);
            String PSNRString = ("PSNR of :" + imageName + " is " + PSNR);
            imageInfo.add(PSNRString);

            imageResults.put(imageName, imageInfo);
          }
        }
      }
    } catch (Exception e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
    return imageResults;
  }


  // Helper function to run the UtilityColourHuffman - 512 algorithm
  private HashMap<String, ArrayList<String>> runColourHuffman512(String CompressDirectory, String DecompressDirectory) {

    HashMap<String, ArrayList<String>> imageResults = new HashMap<>();

    try {
      UtilityColourHuffman512 Utility = new UtilityColourHuffman512();

      String ImageDirectory = "src/main/java/com/example/demo/images/";
      File directory = new File(ImageDirectory);
      File[] files = directory.listFiles();

      if (files != null) {
        System.out.println("1");
        for (File file : files) {
          if (file.isFile()) {
            System.out.println("2");
            ArrayList<String> imageInfo = new ArrayList<>();
            String imageName = file.getName();

            // Converting image to pixels
            ImagetoPixelConverter ImagetoPixelConverter = new ImagetoPixelConverter(ImageDirectory + imageName);

            // Converting the image to pixels
            int[][][] pixelData = ImagetoPixelConverter.getPixelData();
            int width = ImagetoPixelConverter.getWidth();
            int height = ImagetoPixelConverter.getHeight();

            for (int x = 0; x < width; x++) {
              for (int y = 0; y < height; y++) {
                int red = pixelData[x][y][0];
                int green = pixelData[x][y][1];
                int blue = pixelData[x][y][2];
              }
            }

            // Compression
            // Define location and name for the compressed file to be created
            String compressed_file_name = CompressDirectory + "/"
                + imageName.substring(0, imageName.lastIndexOf('.')) + ".bin";

            // start compress timer
            long compressStartTime = System.currentTimeMillis();

            // call compress function
            Utility.Compress(pixelData, compressed_file_name);

            // end timer for compress and record the total time passed
            long compressEndTime = System.currentTimeMillis();
            long compressExecutionTime = compressEndTime - compressStartTime;
            System.out
                .println("Compress Execution Time for " + imageName + " : " + compressExecutionTime + " milliseconds");
            String compressTime = ("Compress Execution Time for " + imageName + " : " + compressExecutionTime
                + " milliseconds");
            imageInfo.add(compressTime);

            // Check the original file size
            File originalFile = new File(ImageDirectory + imageName);
            long originalFileSize = originalFile.length();
            System.out.println("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            String originalSize = ("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            imageInfo.add(originalSize);

            // Check size of the compressed file
            File compressedFile = new File(compressed_file_name);
            long compressedFileSize = compressedFile.length();
            System.out.println("Size of the compressed file for " + imageName + ": " + compressedFileSize + " bytes");
            String compressedSize = ("Size of the compressed file for " + imageName + ": " + compressedFileSize
                + " bytes");
            imageInfo.add(compressedSize);

            // Find the Difference
            long differenceInFileSize = originalFileSize - compressedFileSize;
            System.out.println("Bytes saved from compression of " + imageName + ": " + differenceInFileSize + " bytes");
            String bytesSaved = ("Bytes saved from compression of " + imageName + ": " + differenceInFileSize
                + " bytes");
            imageInfo.add(bytesSaved);

            // Decompression
            // start decompress timer
            long decompressStartTime = System.currentTimeMillis();

            // call decompress function
            int[][][] newPixelData = Utility.Decompress(compressed_file_name);

            // end timer for decompress and record the total time passed
            long decompressEndTime = System.currentTimeMillis();
            long decompressExecutionTime = decompressEndTime - decompressStartTime;
            System.out.println(
                "Decompress Execution Time for " + imageName + " : " + decompressExecutionTime + " milliseconds");
            String decompressTime = ("Decompress Execution Time for " + imageName + " : " + decompressExecutionTime
                + " milliseconds");
            imageInfo.add(decompressTime);

            // convert back to image for visualisation
            PixeltoImageConverter PixeltoImageConverter = new PixeltoImageConverter(newPixelData);
            PixeltoImageConverter.saveImage(DecompressDirectory + "/" + imageName, "png");

            // Get the two bufferedimages for calculations
            BufferedImage originalimage = ImageIO.read(new File(ImageDirectory + imageName));
            BufferedImage decompressedimage = ImageIO
                .read(new File(DecompressDirectory + "/" + imageName));

            // calculate MAE
            double MAE = MAECalculator.calculateMAE(originalimage, decompressedimage);
            System.out.println("Mean Absolute Error of :" + imageName + " is " + MAE);
            String MAEString = ("Mean Absolute Error of :" + imageName + " is " + MAE);
            imageInfo.add(MAEString);

            // calculate MSE
            double MSE = MSECalculator.calculateMSE(originalimage, decompressedimage);
            System.out.println("Mean Squared Error of :" + imageName + " is " + MSE);
            String MSEString = ("Mean Squared Error of :" + imageName + " is " + MSE);
            imageInfo.add(MSEString);

            // calculate PSNR
            double PSNR = PSNRCalculator.calculatePSNR(originalimage, decompressedimage);
            System.out.println("PSNR of :" + imageName + " is " + PSNR);
            String PSNRString = ("PSNR of :" + imageName + " is " + PSNR);
            imageInfo.add(PSNRString);

            imageResults.put(imageName, imageInfo);

          }
        }
      }
    } catch (Exception e) {
      System.out.println("An error occurred");
      e.printStackTrace();
    }
    return imageResults;
  }

  // Helper function to run the UtilityColourHuffman - 256 algorithm
  private HashMap<String, ArrayList<String>> runColourHuffman256(String CompressDirectory, String DecompressDirectory) {

    HashMap<String, ArrayList<String>> imageResults = new HashMap<>();

    try {
      UtilityColourHuffman256 Utility = new UtilityColourHuffman256();

      String ImageDirectory = "src/main/java/com/example/demo/images/";
      File directory = new File(ImageDirectory);
      File[] files = directory.listFiles();

      if (files != null) {
        System.out.println("1");
        for (File file : files) {
          if (file.isFile()) {
            System.out.println("2");
            ArrayList<String> imageInfo = new ArrayList<>();
            String imageName = file.getName();

            // Converting image to pixels
            ImagetoPixelConverter ImagetoPixelConverter = new ImagetoPixelConverter(ImageDirectory + imageName);

            // Converting the image to pixels
            int[][][] pixelData = ImagetoPixelConverter.getPixelData();
            int width = ImagetoPixelConverter.getWidth();
            int height = ImagetoPixelConverter.getHeight();

            for (int x = 0; x < width; x++) {
              for (int y = 0; y < height; y++) {
                int red = pixelData[x][y][0];
                int green = pixelData[x][y][1];
                int blue = pixelData[x][y][2];
              }
            }

            // Compression
            // Define location and name for the compressed file to be created
            String compressed_file_name = CompressDirectory + "/"
                + imageName.substring(0, imageName.lastIndexOf('.')) + ".bin";

            // start compress timer
            long compressStartTime = System.currentTimeMillis();

            // call compress function
            Utility.Compress(pixelData, compressed_file_name);

            // end timer for compress and record the total time passed
            long compressEndTime = System.currentTimeMillis();
            long compressExecutionTime = compressEndTime - compressStartTime;
            System.out
                .println("Compress Execution Time for " + imageName + " : " + compressExecutionTime + " milliseconds");
            String compressTime = ("Compress Execution Time for " + imageName + " : " + compressExecutionTime
                + " milliseconds");
            imageInfo.add(compressTime);

            // Check the original file size
            File originalFile = new File(ImageDirectory + imageName);
            long originalFileSize = originalFile.length();
            System.out.println("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            String originalSize = ("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            imageInfo.add(originalSize);

            // Check size of the compressed file
            File compressedFile = new File(compressed_file_name);
            long compressedFileSize = compressedFile.length();
            System.out.println("Size of the compressed file for " + imageName + ": " + compressedFileSize + " bytes");
            String compressedSize = ("Size of the compressed file for " + imageName + ": " + compressedFileSize
                + " bytes");
            imageInfo.add(compressedSize);

            // Find the Difference
            long differenceInFileSize = originalFileSize - compressedFileSize;
            System.out.println("Bytes saved from compression of " + imageName + ": " + differenceInFileSize + " bytes");
            String bytesSaved = ("Bytes saved from compression of " + imageName + ": " + differenceInFileSize
                + " bytes");
            imageInfo.add(bytesSaved);

            // Decompression
            // start decompress timer
            long decompressStartTime = System.currentTimeMillis();

            // call decompress function
            int[][][] newPixelData = Utility.Decompress(compressed_file_name);

            // end timer for decompress and record the total time passed
            long decompressEndTime = System.currentTimeMillis();
            long decompressExecutionTime = decompressEndTime - decompressStartTime;
            System.out.println(
                "Decompress Execution Time for " + imageName + " : " + decompressExecutionTime + " milliseconds");
            String decompressTime = ("Decompress Execution Time for " + imageName + " : " + decompressExecutionTime
                + " milliseconds");
            imageInfo.add(decompressTime);

            // convert back to image for visualisation
            PixeltoImageConverter PixeltoImageConverter = new PixeltoImageConverter(newPixelData);
            PixeltoImageConverter.saveImage(DecompressDirectory + "/" + imageName, "png");

            // Get the two bufferedimages for calculations
            BufferedImage originalimage = ImageIO.read(new File(ImageDirectory + imageName));
            BufferedImage decompressedimage = ImageIO
                .read(new File(DecompressDirectory + "/" + imageName));

            // calculate MAE
            double MAE = MAECalculator.calculateMAE(originalimage, decompressedimage);
            System.out.println("Mean Absolute Error of :" + imageName + " is " + MAE);
            String MAEString = ("Mean Absolute Error of :" + imageName + " is " + MAE);
            imageInfo.add(MAEString);

            // calculate MSE
            double MSE = MSECalculator.calculateMSE(originalimage, decompressedimage);
            System.out.println("Mean Squared Error of :" + imageName + " is " + MSE);
            String MSEString = ("Mean Squared Error of :" + imageName + " is " + MSE);
            imageInfo.add(MSEString);

            // calculate PSNR
            double PSNR = PSNRCalculator.calculatePSNR(originalimage, decompressedimage);
            System.out.println("PSNR of :" + imageName + " is " + PSNR);
            String PSNRString = ("PSNR of :" + imageName + " is " + PSNR);
            imageInfo.add(PSNRString);

            imageResults.put(imageName, imageInfo);

          }
        }
      }
    } catch (Exception e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
    return imageResults;
  }

  // Helper function to run the UtilityColourHuffman - String algorithm
  private HashMap<String, ArrayList<String>> runColourHuffmanString(String CompressDirectory, String DecompressDirectory) {

    HashMap<String, ArrayList<String>> imageResults = new HashMap<>();

    try {
      UtilityColourHuffmanString Utility = new UtilityColourHuffmanString();

      String ImageDirectory = "src/main/java/com/example/demo/images/";
      File directory = new File(ImageDirectory);
      File[] files = directory.listFiles();

      if (files != null) {
        System.out.println("1");
        for (File file : files) {
          if (file.isFile()) {
            System.out.println("2");
            ArrayList<String> imageInfo = new ArrayList<>();
            String imageName = file.getName();

            // Converting image to pixels
            ImagetoPixelConverter ImagetoPixelConverter = new ImagetoPixelConverter(ImageDirectory + imageName);

            // Converting the image to pixels
            int[][][] pixelData = ImagetoPixelConverter.getPixelData();
            int width = ImagetoPixelConverter.getWidth();
            int height = ImagetoPixelConverter.getHeight();

            for (int x = 0; x < width; x++) {
              for (int y = 0; y < height; y++) {
                int red = pixelData[x][y][0];
                int green = pixelData[x][y][1];
                int blue = pixelData[x][y][2];
              }
            }

            // Compression
            // Define location and name for the compressed file to be created
            String compressed_file_name = CompressDirectory +"/"
                + imageName.substring(0, imageName.lastIndexOf('.')) + ".bin";

            // start compress timer
            long compressStartTime = System.currentTimeMillis();

            // call compress function
            Utility.Compress(pixelData, compressed_file_name);

            // end timer for compress and record the total time passed
            long compressEndTime = System.currentTimeMillis();
            long compressExecutionTime = compressEndTime - compressStartTime;
            System.out
                .println("Compress Execution Time for " + imageName + " : " + compressExecutionTime + " milliseconds");
            String compressTime = ("Compress Execution Time for " + imageName + " : " + compressExecutionTime
                + " milliseconds");
            imageInfo.add(compressTime);

            // Check the original file size
            File originalFile = new File(ImageDirectory + imageName);
            long originalFileSize = originalFile.length();
            System.out.println("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            String originalSize = ("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            imageInfo.add(originalSize);

            // Check size of the compressed file
            File compressedFile = new File(compressed_file_name);
            long compressedFileSize = compressedFile.length();
            System.out.println("Size of the compressed file for " + imageName + ": " + compressedFileSize + " bytes");
            String compressedSize = ("Size of the compressed file for " + imageName + ": " + compressedFileSize
                + " bytes");
            imageInfo.add(compressedSize);

            // Find the Difference
            long differenceInFileSize = originalFileSize - compressedFileSize;
            System.out.println("Bytes saved from compression of " + imageName + ": " + differenceInFileSize + " bytes");
            String bytesSaved = ("Bytes saved from compression of " + imageName + ": " + differenceInFileSize
                + " bytes");
            imageInfo.add(bytesSaved);

            // Decompression
            // start decompress timer
            long decompressStartTime = System.currentTimeMillis();

            // call decompress function
            int[][][] newPixelData = Utility.Decompress(compressed_file_name);

            // end timer for decompress and record the total time passed
            long decompressEndTime = System.currentTimeMillis();
            long decompressExecutionTime = decompressEndTime - decompressStartTime;
            System.out.println(
                "Decompress Execution Time for " + imageName + " : " + decompressExecutionTime + " milliseconds");
            String decompressTime = ("Decompress Execution Time for " + imageName + " : " + decompressExecutionTime
                + " milliseconds");
            imageInfo.add(decompressTime);

            // convert back to image for visualisation
            PixeltoImageConverter PixeltoImageConverter = new PixeltoImageConverter(newPixelData);
            PixeltoImageConverter.saveImage(DecompressDirectory + "/" + imageName, "png");

            // Get the two bufferedimages for calculations
            BufferedImage originalimage = ImageIO.read(new File(ImageDirectory + imageName));
            BufferedImage decompressedimage = ImageIO
                .read(new File(DecompressDirectory + "/" + imageName));

            // calculate MAE
            double MAE = MAECalculator.calculateMAE(originalimage, decompressedimage);
            System.out.println("Mean Absolute Error of :" + imageName + " is " + MAE);
            String MAEString = ("Mean Absolute Error of :" + imageName + " is " + MAE);
            imageInfo.add(MAEString);

            // calculate MSE
            double MSE = MSECalculator.calculateMSE(originalimage, decompressedimage);
            System.out.println("Mean Squared Error of :" + imageName + " is " + MSE);
            String MSEString = ("Mean Squared Error of :" + imageName + " is " + MSE);
            imageInfo.add(MSEString);

            // calculate PSNR
            double PSNR = PSNRCalculator.calculatePSNR(originalimage, decompressedimage);
            System.out.println("PSNR of :" + imageName + " is " + PSNR);
            String PSNRString = ("PSNR of :" + imageName + " is " + PSNR);
            imageInfo.add(PSNRString);

            imageResults.put(imageName, imageInfo);

          }
        }
      }
    } catch (Exception e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
    return imageResults;
  }

  // Helper function to run the UtilityDitheringHuffman algorithm
  private HashMap<String, ArrayList<String>> runDitheringHuffman(String CompressDirectory, String DecompressDirectory) {

    HashMap<String, ArrayList<String>> imageResults = new HashMap<>();

    try {
      UtilityDitheringHuffman Utility = new UtilityDitheringHuffman();

      String ImageDirectory = "src/main/java/com/example/demo/images/";
      File directory = new File(ImageDirectory);
      File[] files = directory.listFiles();

      if (files != null) {
        System.out.println("1");
        for (File file : files) {
          if (file.isFile()) {
            System.out.println("2");
            ArrayList<String> imageInfo = new ArrayList<>();
            String imageName = file.getName();

            // Converting image to pixels
            ImagetoPixelConverter ImagetoPixelConverter = new ImagetoPixelConverter(ImageDirectory + imageName);

            // Converting the image to pixels
            int[][][] pixelData = ImagetoPixelConverter.getPixelData();
            int width = ImagetoPixelConverter.getWidth();
            int height = ImagetoPixelConverter.getHeight();

            for (int x = 0; x < width; x++) {
              for (int y = 0; y < height; y++) {
                int red = pixelData[x][y][0];
                int green = pixelData[x][y][1];
                int blue = pixelData[x][y][2];
              }
            }

            // Compression
            // Define location and name for the compressed file to be created
            String compressed_file_name = CompressDirectory + "/"
                + imageName.substring(0, imageName.lastIndexOf('.')) + ".bin";

            // start compress timer
            long compressStartTime = System.currentTimeMillis();

            // call compress function
            Utility.Compress(pixelData, compressed_file_name);

            // end timer for compress and record the total time passed
            long compressEndTime = System.currentTimeMillis();
            long compressExecutionTime = compressEndTime - compressStartTime;
            System.out
                .println("Compress Execution Time for " + imageName + " : " + compressExecutionTime + " milliseconds");
            String compressTime = ("Compress Execution Time for " + imageName + " : " + compressExecutionTime
                + " milliseconds");
            imageInfo.add(compressTime);

            // Check the original file size
            File originalFile = new File(ImageDirectory + imageName);
            long originalFileSize = originalFile.length();
            System.out.println("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            String originalSize = ("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            imageInfo.add(originalSize);

            // Check size of the compressed file
            File compressedFile = new File(compressed_file_name);
            long compressedFileSize = compressedFile.length();
            System.out.println("Size of the compressed file for " + imageName + ": " + compressedFileSize + " bytes");
            String compressedSize = ("Size of the compressed file for " + imageName + ": " + compressedFileSize
                + " bytes");
            imageInfo.add(compressedSize);

            // Find the Difference
            long differenceInFileSize = originalFileSize - compressedFileSize;
            System.out.println("Bytes saved from compression of " + imageName + ": " + differenceInFileSize + " bytes");
            String bytesSaved = ("Bytes saved from compression of " + imageName + ": " + differenceInFileSize
                + " bytes");
            imageInfo.add(bytesSaved);

            // Decompression
            // start decompress timer
            long decompressStartTime = System.currentTimeMillis();

            // call decompress function
            int[][][] newPixelData = Utility.Decompress(compressed_file_name);

            // end timer for decompress and record the total time passed
            long decompressEndTime = System.currentTimeMillis();
            long decompressExecutionTime = decompressEndTime - decompressStartTime;
            System.out.println(
                "Decompress Execution Time for " + imageName + " : " + decompressExecutionTime + " milliseconds");
            String decompressTime = ("Decompress Execution Time for " + imageName + " : " + decompressExecutionTime
                + " milliseconds");
            imageInfo.add(decompressTime);

            // convert back to image for visualisation
            PixeltoImageConverter PixeltoImageConverter = new PixeltoImageConverter(newPixelData);
            PixeltoImageConverter.saveImage(DecompressDirectory + "/" + imageName, "png");

            // Get the two bufferedimages for calculations
            BufferedImage originalimage = ImageIO.read(new File(ImageDirectory + imageName));
            BufferedImage decompressedimage = ImageIO
                .read(new File(DecompressDirectory + "/" + imageName));

            // calculate MAE
            double MAE = MAECalculator.calculateMAE(originalimage, decompressedimage);
            System.out.println("Mean Absolute Error of :" + imageName + " is " + MAE);
            String MAEString = ("Mean Absolute Error of :" + imageName + " is " + MAE);
            imageInfo.add(MAEString);

            // calculate MSE
            double MSE = MSECalculator.calculateMSE(originalimage, decompressedimage);
            System.out.println("Mean Squared Error of :" + imageName + " is " + MSE);
            String MSEString = ("Mean Squared Error of :" + imageName + " is " + MSE);
            imageInfo.add(MSEString);

            // calculate PSNR
            double PSNR = PSNRCalculator.calculatePSNR(originalimage, decompressedimage);
            System.out.println("PSNR of :" + imageName + " is " + PSNR);
            String PSNRString = ("PSNR of :" + imageName + " is " + PSNR);
            imageInfo.add(PSNRString);

            imageResults.put(imageName, imageInfo);

          }
        }
      }
    } catch (Exception e) {
      System.out.println("An error occurred");
      e.printStackTrace();
    }
    return imageResults;
  }

  // Helper function to run the UtilityHuffmanOnly algorithm
  private HashMap<String, ArrayList<String>> runHuffmanOnly(String CompressDirectory, String DecompressDirectory) {

    HashMap<String, ArrayList<String>> imageResults = new HashMap<>();

    try {
      UtilityHuffmanOnly Utility = new UtilityHuffmanOnly();

      String ImageDirectory = "src/main/java/com/example/demo/images/";
      File directory = new File(ImageDirectory);
      File[] files = directory.listFiles();

      if (files != null) {
        System.out.println("1");
        for (File file : files) {
          if (file.isFile()) {
            System.out.println("2");
            ArrayList<String> imageInfo = new ArrayList<>();
            String imageName = file.getName();

            // Converting image to pixels
            ImagetoPixelConverter ImagetoPixelConverter = new ImagetoPixelConverter(ImageDirectory + imageName);

            // Converting the image to pixels
            int[][][] pixelData = ImagetoPixelConverter.getPixelData();
            int width = ImagetoPixelConverter.getWidth();
            int height = ImagetoPixelConverter.getHeight();

            for (int x = 0; x < width; x++) {
              for (int y = 0; y < height; y++) {
                int red = pixelData[x][y][0];
                int green = pixelData[x][y][1];
                int blue = pixelData[x][y][2];
              }
            }

            // Compression
            // Define location and name for the compressed file to be created
            String compressed_file_name = CompressDirectory + "/"
                + imageName.substring(0, imageName.lastIndexOf('.')) + ".bin";

            // start compress timer
            long compressStartTime = System.currentTimeMillis();

            // call compress function
            Utility.Compress(pixelData, compressed_file_name);

            // end timer for compress and record the total time passed
            long compressEndTime = System.currentTimeMillis();
            long compressExecutionTime = compressEndTime - compressStartTime;
            System.out
                .println("Compress Execution Time for " + imageName + " : " + compressExecutionTime + " milliseconds");
            String compressTime = ("Compress Execution Time for " + imageName + " : " + compressExecutionTime
                + " milliseconds");
            imageInfo.add(compressTime);

            // Check the original file size
            File originalFile = new File(ImageDirectory + imageName);
            long originalFileSize = originalFile.length();
            System.out.println("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            String originalSize = ("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            imageInfo.add(originalSize);

            // Check size of the compressed file
            File compressedFile = new File(compressed_file_name);
            long compressedFileSize = compressedFile.length();
            System.out.println("Size of the compressed file for " + imageName + ": " + compressedFileSize + " bytes");
            String compressedSize = ("Size of the compressed file for " + imageName + ": " + compressedFileSize
                + " bytes");
            imageInfo.add(compressedSize);

            // Find the Difference
            long differenceInFileSize = originalFileSize - compressedFileSize;
            System.out.println("Bytes saved from compression of " + imageName + ": " + differenceInFileSize + " bytes");
            String bytesSaved = ("Bytes saved from compression of " + imageName + ": " + differenceInFileSize
                + " bytes");
            imageInfo.add(bytesSaved);

            // Decompression
            // start decompress timer
            long decompressStartTime = System.currentTimeMillis();

            // call decompress function
            int[][][] newPixelData = Utility.Decompress(compressed_file_name);

            // end timer for decompress and record the total time passed
            long decompressEndTime = System.currentTimeMillis();
            long decompressExecutionTime = decompressEndTime - decompressStartTime;
            System.out.println(
                "Decompress Execution Time for " + imageName + " : " + decompressExecutionTime + " milliseconds");
            String decompressTime = ("Decompress Execution Time for " + imageName + " : " + decompressExecutionTime
                + " milliseconds");
            imageInfo.add(decompressTime);

            // convert back to image for visualisation
            PixeltoImageConverter PixeltoImageConverter = new PixeltoImageConverter(newPixelData);
            PixeltoImageConverter.saveImage(DecompressDirectory + "/" + imageName, "png");

            // Get the two bufferedimages for calculations
            BufferedImage originalimage = ImageIO.read(new File(ImageDirectory + imageName));
            BufferedImage decompressedimage = ImageIO
                .read(new File(DecompressDirectory + "/" + imageName));

            // calculate MAE
            double MAE = MAECalculator.calculateMAE(originalimage, decompressedimage);
            System.out.println("Mean Absolute Error of :" + imageName + " is " + MAE);
            String MAEString = ("Mean Absolute Error of :" + imageName + " is " + MAE);
            imageInfo.add(MAEString);

            // calculate MSE
            double MSE = MSECalculator.calculateMSE(originalimage, decompressedimage);
            System.out.println("Mean Squared Error of :" + imageName + " is " + MSE);
            String MSEString = ("Mean Squared Error of :" + imageName + " is " + MSE);
            imageInfo.add(MSEString);

            // calculate PSNR
            double PSNR = PSNRCalculator.calculatePSNR(originalimage, decompressedimage);
            System.out.println("PSNR of :" + imageName + " is " + PSNR);
            String PSNRString = ("PSNR of :" + imageName + " is " + PSNR);
            imageInfo.add(PSNRString);

            imageResults.put(imageName, imageInfo);

          }
        }
      }
    } catch (Exception e) {
      System.out.println("An error occurred");
      e.printStackTrace();
    }
    return imageResults;
  }

  // Helper function to run the UtilityOriginalBad algorithm
  private HashMap<String, ArrayList<String>> runOriginalBad(String CompressDirectory, String DecompressDirectory) {

    HashMap<String, ArrayList<String>> imageResults = new HashMap<>();

    try {
      UtilityOriginalBad Utility = new UtilityOriginalBad();

      String ImageDirectory = "src/main/java/com/example/demo/images/";
      File directory = new File(ImageDirectory);
      File[] files = directory.listFiles();

      if (files != null) {
        System.out.println("1");
        for (File file : files) {
          if (file.isFile()) {
            System.out.println("2");
            ArrayList<String> imageInfo = new ArrayList<>();
            String imageName = file.getName();

            // Converting image to pixels
            ImagetoPixelConverter ImagetoPixelConverter = new ImagetoPixelConverter(ImageDirectory + imageName);

            // Converting the image to pixels
            int[][][] pixelData = ImagetoPixelConverter.getPixelData();
            int width = ImagetoPixelConverter.getWidth();
            int height = ImagetoPixelConverter.getHeight();

            for (int x = 0; x < width; x++) {
              for (int y = 0; y < height; y++) {
                int red = pixelData[x][y][0];
                int green = pixelData[x][y][1];
                int blue = pixelData[x][y][2];
              }
            }

            // Compression
            // Define location and name for the compressed file to be created
            String compressed_file_name = CompressDirectory + "/"
                + imageName.substring(0, imageName.lastIndexOf('.')) + ".bin";

            // start compress timer
            long compressStartTime = System.currentTimeMillis();

            // call compress function
            Utility.Compress(pixelData, compressed_file_name);

            // end timer for compress and record the total time passed
            long compressEndTime = System.currentTimeMillis();
            long compressExecutionTime = compressEndTime - compressStartTime;
            System.out
                .println("Compress Execution Time for " + imageName + " : " + compressExecutionTime + " milliseconds");
            String compressTime = ("Compress Execution Time for " + imageName + " : " + compressExecutionTime
                + " milliseconds");
            imageInfo.add(compressTime);

            // Check the original file size
            File originalFile = new File(ImageDirectory + imageName);
            long originalFileSize = originalFile.length();
            System.out.println("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            String originalSize = ("Size of the original file for " + imageName + ": " + originalFileSize + " bytes");
            imageInfo.add(originalSize);

            // Check size of the compressed file
            File compressedFile = new File(compressed_file_name);
            long compressedFileSize = compressedFile.length();
            System.out.println("Size of the compressed file for " + imageName + ": " + compressedFileSize + " bytes");
            String compressedSize = ("Size of the compressed file for " + imageName + ": " + compressedFileSize
                + " bytes");
            imageInfo.add(compressedSize);

            // Find the Difference
            long differenceInFileSize = originalFileSize - compressedFileSize;
            System.out.println("Bytes saved from compression of " + imageName + ": " + differenceInFileSize + " bytes");
            String bytesSaved = ("Bytes saved from compression of " + imageName + ": " + differenceInFileSize
                + " bytes");
            imageInfo.add(bytesSaved);

            // Decompression
            // start decompress timer
            long decompressStartTime = System.currentTimeMillis();

            // call decompress function
            int[][][] newPixelData = Utility.Decompress(compressed_file_name);

            // end timer for decompress and record the total time passed
            long decompressEndTime = System.currentTimeMillis();
            long decompressExecutionTime = decompressEndTime - decompressStartTime;
            System.out.println(
                "Decompress Execution Time for " + imageName + " : " + decompressExecutionTime + " milliseconds");
            String decompressTime = ("Decompress Execution Time for " + imageName + " : " + decompressExecutionTime
                + " milliseconds");
            imageInfo.add(decompressTime);

            // convert back to image for visualisation
            PixeltoImageConverter PixeltoImageConverter = new PixeltoImageConverter(newPixelData);
            PixeltoImageConverter.saveImage(DecompressDirectory + "/" + imageName, "png");

            // Get the two bufferedimages for calculations
            BufferedImage originalimage = ImageIO.read(new File(ImageDirectory + imageName));
            BufferedImage decompressedimage = ImageIO
                .read(new File(DecompressDirectory + "/" + imageName));

            // calculate MAE
            double MAE = MAECalculator.calculateMAE(originalimage, decompressedimage);
            System.out.println("Mean Absolute Error of :" + imageName + " is " + MAE);
            String MAEString = ("Mean Absolute Error of :" + imageName + " is " + MAE);
            imageInfo.add(MAEString);

            // calculate MSE
            double MSE = MSECalculator.calculateMSE(originalimage, decompressedimage);
            System.out.println("Mean Squared Error of :" + imageName + " is " + MSE);
            String MSEString = ("Mean Squared Error of :" + imageName + " is " + MSE);
            imageInfo.add(MSEString);

            // calculate PSNR
            double PSNR = PSNRCalculator.calculatePSNR(originalimage, decompressedimage);
            System.out.println("PSNR of :" + imageName + " is " + PSNR);
            String PSNRString = ("PSNR of :" + imageName + " is " + PSNR);
            imageInfo.add(PSNRString);

            imageResults.put(imageName, imageInfo);

          }
        }
      }
    } catch (Exception e) {
      System.out.println("An error occurred");
      e.printStackTrace();
    }
    return imageResults;
  }

  // Helper function to run an algorithm based on the provided algorithm name
  private HashMap<String, HashMap<String, ArrayList<String>>> runChosenAlgorithm(String algorithm, String CompressDirectory, String DecompressDirectory) {

    HashMap<String, HashMap<String, ArrayList<String>>> algoResult = new HashMap<>();

    try {
        // Run the first algorithm concurrently
      if (algorithm.equals("Original Bad Algorithm")) {
        HashMap<String, ArrayList<String>> result = runOriginalBad(CompressDirectory, DecompressDirectory);
        algoResult.put(algorithm, result);
      }
      else if (algorithm.equals("Huffman Algorithm")) {
        HashMap<String, ArrayList<String>> result = runHuffmanOnly(CompressDirectory, DecompressDirectory);
        algoResult.put(algorithm, result);
      }
      else if (algorithm.equals("Colour Quant and Huffman Algorithm - Quant 256")) {
        HashMap<String, ArrayList<String>> result = runColourHuffman256(CompressDirectory, DecompressDirectory);
        algoResult.put(algorithm, result);
      }
      else if (algorithm.equals("Colour Quant and Huffman Algorithm - Quant 512")) {
        HashMap<String, ArrayList<String>> result = runColourHuffman512(CompressDirectory, DecompressDirectory);
        algoResult.put(algorithm, result);
      }
      else if (algorithm.equals("Colour Quant and Huffman Algorithm - String Array")) {
        HashMap<String, ArrayList<String>> result = runColourHuffmanString(CompressDirectory, DecompressDirectory);
        algoResult.put(algorithm, result);
      }
      else if (algorithm.equals("Image Dithering and Huffman Algorithm")) {
        HashMap<String, ArrayList<String>> result = runDitheringHuffman(CompressDirectory, DecompressDirectory);
        algoResult.put(algorithm, result);
      }
      else {
        throw new Exception("Invalid algorithm name for algo 1");
      }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return algoResult;
  }


  // Endpoint to run the 2 selected algorithms concurrently
  @GetMapping("/algorithms")
  public HashMap<String, HashMap<String, ArrayList<String>>> getAlgorithms(
    @RequestParam(name = "algo1", required = true) String algo1,
    @RequestParam(name = "algo2", required = true) String algo2
  ) {

    if (algo1.equals(algo2)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chosen algorithms cannot be the same");
    }

    HashMap<String, HashMap<String, ArrayList<String>>> overallResults = new HashMap<String, HashMap<String, ArrayList<String>>>();

    // Create an executor service with a thread pool of size 2
    ExecutorService executor = Executors.newFixedThreadPool(2);

    // Create tasks for running algo1 and algo2 concurrently
    List<Callable<HashMap<String, HashMap<String, ArrayList<String>>>>> tasks = new ArrayList<>();
    tasks.add(() -> runChosenAlgorithm(algo1, "src/main/java/com/example/demo/compressed", "src/main/java/com/example/demo/decompressed"));
    tasks.add(() -> runChosenAlgorithm(algo2, "src/main/java/com/example/demo/compressed2", "src/main/java/com/example/demo/decompressed2"));

    try {
      System.out.println("Running the algorithms concurrently");

      // Run the tasks concurrently
      List<Future<HashMap<String, HashMap<String, ArrayList<String>>>>> concurrentResults = executor.invokeAll(tasks);

      // Get the results from the concurrent tasks
      for (Future<HashMap<String, HashMap<String, ArrayList<String>>>> concurrentResult : concurrentResults) {
        HashMap<String, HashMap<String, ArrayList<String>>> result = concurrentResult.get();
        overallResults.putAll(result);
      }

      System.out.println("Concurrent algorithms completed");
    }
    catch (ResponseStatusException e) {
      System.out.println("Algorithms validation exception: " + e.getMessage());
      e.printStackTrace();
    }
    catch (Exception e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    } finally {
      executor.shutdown();
    }

    return overallResults;
  }
}