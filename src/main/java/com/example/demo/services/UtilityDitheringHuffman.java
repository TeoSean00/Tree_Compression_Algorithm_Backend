package com.example.demo.services;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

class HuffmanNode implements Serializable {
    public int value; // The pixel value
    public int frequency; // Frequency of the pixel value
    public HuffmanNode left; // Left child
    public HuffmanNode right; // Right child

    public HuffmanNode(int value, int frequency) {
        this.value = value;
        this.frequency = frequency;
        this.left = null;
        this.right = null;
    }

    @Override
    public String toString() {
        return "Value: " + value + ", Frequency: " + frequency;
    }
}

public class UtilityDitheringHuffman {
    // HuffmanNode root;
    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        int quantizationLevels = 128;

        // Quantize the image
        int[][][] quantizedPixels = quantizeImageDithering(pixels, quantizationLevels);

        HashMap<Integer, Integer> frequencyTable = buildFrequencyTable(quantizedPixels);
        // Build a Huffman tree from the frequency table
        HuffmanNode root = buildHuffmanTree(frequencyTable);

        // Create a mapping of pixel values to Huffman codes
        HashMap<Integer, String> huffmanCodes = buildHuffmanCodes(root, "");

        // Encode the RLE-compressed data using Huffman codes
        String compressedString = encodeWithHuffman(quantizedPixels, huffmanCodes);
        byte[] compressedData = convertBinaryStringToByteArray(compressedString);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {
            // Write the Huffman tree and compressed data to the output file
            oos.writeObject(root);
            oos.writeObject(compressedData);
        }
    }

    public static byte[] convertBinaryStringToByteArray(String binaryString) {
        int length = binaryString.length();
        int byteLength = (length + 7) / 8; // Calculate the number of bytes required

        byte[] byteArray = new byte[byteLength];
        for (int i = 0; i < length; i++) {
            if (binaryString.charAt(i) == '1') {
                int byteIndex = i / 8;
                int bitIndex = 7 - (i % 8);
                byteArray[byteIndex] |= (1 << bitIndex);
            }
        }

        return byteArray;
    }

    public static String convertByteArrayToBinaryString(byte[] byteArray) {
        StringBuilder binaryStringBuilder = new StringBuilder();

        for (byte b : byteArray) {
            for (int i = 7; i >= 0; i--) {
                int bit = (b >> i) & 1;
                binaryStringBuilder.append(bit);
            }
        }

        return binaryStringBuilder.toString();
    }

    // Helper function to view array
    public static void print3DArray(int[][][] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                for (int k = 0; k < array[i][j].length; k++) {
                    System.out.print(array[i][j][k] + " ");
                }
                System.out.println(); // Move to the next row
            }
            System.out.println(); // Add a blank line to separate 2D slices
        }
    }

    private int[][][] quantizeImageDithering(int[][][] pixels, int levels) {
        int width = pixels.length;
        int height = pixels[0].length;
        int depth = pixels[0][0].length;
        int[][][] quantizedPixels = new int[width][height][depth];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < depth; k++) {
                    int oldPixel = pixels[i][j][k];
                    int newPixel = (int) ((oldPixel * (levels - 1)) / 255.0);

                    quantizedPixels[i][j][k] = newPixel;
                    int quantizationError = oldPixel - newPixel;

                    // Distribute the error to neighboring pixels
                    if (i + 1 < width) {
                        quantizedPixels[i + 1][j][k] += quantizationError * 7 / 16;
                    }
                    if (i - 1 >= 0 && j + 1 < height) {
                        quantizedPixels[i - 1][j + 1][k] += quantizationError * 3 / 16;
                    }
                    if (j + 1 < height) {
                        quantizedPixels[i][j + 1][k] += quantizationError * 5 / 16;
                    }
                    if (i + 1 < width && j + 1 < height) {
                        quantizedPixels[i + 1][j + 1][k] += quantizationError * 1 / 16;
                    }
                }
            }
        }

        return quantizedPixels;
    }

    private HashMap<Integer, Integer> buildFrequencyTable(int[][][] pixels) {
        HashMap<Integer, Integer> frequencyTable = new HashMap<>();

        for (int[][] pixelRow : pixels) {
            for (int[] pixel : pixelRow) {
                for (int value : pixel) {
                    // Count the frequency of each pixel value
                    frequencyTable.put(value, frequencyTable.getOrDefault(value, 0) + 1);
                }
            }
        }
        return frequencyTable;
    }

    private HuffmanNode buildHuffmanTree(HashMap<Integer, Integer> frequencyTable) {
        PriorityQueue<HuffmanNode> minHeap = new PriorityQueue<>(
                (a, b) -> a.frequency - b.frequency);

        // Create a leaf node for each pixel value and add them to the min-heap
        for (int value : frequencyTable.keySet()) {
            HuffmanNode node = new HuffmanNode(value, frequencyTable.get(value));
            minHeap.offer(node);
        }
        // Build the Huffman tree by merging nodes until only one node (the root)
        while (minHeap.size() > 1) {
            HuffmanNode left = minHeap.poll();
            HuffmanNode right = minHeap.poll();
            HuffmanNode parent = new HuffmanNode(-1, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;
            minHeap.offer(parent);
        }

        return minHeap.poll(); // The root of the Huffman tree
    }

    private HashMap<Integer, String> buildHuffmanCodes(HuffmanNode root, String currentCode) {
        HashMap<Integer, String> huffmanCodes = new HashMap<>();

        if (root == null) {
            return huffmanCodes;
        }

        if (root.left == null && root.right == null) {
            // This is a leaf node (pixel value), add it to the codes map
            huffmanCodes.put(root.value, currentCode);
        }

        // Recursively build codes for left and right subtrees
        huffmanCodes.putAll(buildHuffmanCodes(root.left, currentCode + "0"));
        huffmanCodes.putAll(buildHuffmanCodes(root.right, currentCode + "1"));
        return huffmanCodes;
    }

    private String encodeWithHuffman(int[][][] pixels, HashMap<Integer, String> huffmanCodes) {
        StringBuilder compressedData = new StringBuilder();

        for (int[][] pixelRow : pixels) {
            for (int[] pixel : pixelRow) {
                for (int value : pixel) {
                    String huffmanCode = huffmanCodes.get(value);
                    compressedData.append(huffmanCode); // Append the Huffman code to the StringBuilder
                }
            }
        }

        return compressedData.toString();
    }

    public static int[] decompressData(String compressedData, Map<Integer, String> huffmanCodes) {
        List<Integer> decompressedData = new ArrayList<>();
        StringBuilder currentCode = new StringBuilder();

        for (char bit : compressedData.toCharArray()) {
            currentCode.append(bit);
            for (Map.Entry<Integer, String> entry : huffmanCodes.entrySet()) {
                if (entry.getValue().equals(currentCode.toString())) {
                    decompressedData.add(entry.getKey());
                    currentCode.setLength(0); // Reset the current code
                    break;
                }
            }
        }

        int[] result = new int[decompressedData.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = decompressedData.get(i);
        }
        return result;
    }

    private int[][][] decodeWithHuffman(String compressedData, HashMap<Integer, String> huffmanMappings) {
        // Create a reverse mapping of Huffman codes to values for efficient decoding
        HashMap<String, Integer> reverseHuffmanMappings = new HashMap<>();
        for (Map.Entry<Integer, String> entry : huffmanMappings.entrySet()) {
            reverseHuffmanMappings.put(entry.getValue(), entry.getKey());
        }

        List<Integer> pixelValues = new ArrayList<>();
        StringBuilder currentCode = new StringBuilder();

        for (char bit : compressedData.toCharArray()) {
            currentCode.append(bit);
            Integer value = reverseHuffmanMappings.get(currentCode.toString());
            if (value != null) {
                pixelValues.add(value);
                currentCode.setLength(0); // Reset the currentCode
            }
        }

        int width = 500; // Set the correct width from your header data;
        int height = 375; // Set the correct height from your header data;
        int depth = 3; // Set the correct depth from your header data;
        int[][][] pixels = new int[width][height][depth];
        int pixelIndex = 0;

        for (int h = 0; h < width; h++) {
            for (int w = 0; w < height; w++) {
                for (int c = 0; c < depth; c++) {
                    pixels[h][w][c] = pixelValues.get(pixelIndex);
                    pixelIndex++;
                }
            }
        }

        return pixels;
    }

    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
            // Read the Huffman tree from the input file
            HuffmanNode root = (HuffmanNode) ois.readObject();

            // Read the compressed data
            byte[] compressedData = (byte[]) ois.readObject();
            String binaryString = convertByteArrayToBinaryString(compressedData);

            // Decode the compressed data using the Huffman tree
            HashMap<Integer, String> huffmanCodes = buildHuffmanCodes(root, "");

            int[][][] pixels = decodeWithHuffman(binaryString, huffmanCodes);
            return pixels;
        }
    }

}