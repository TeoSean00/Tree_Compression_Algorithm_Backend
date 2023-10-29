package com.example.demo.services;

import java.awt.Color;

import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*;

public class UtilityColourHuffman512 {

	public void Compress(int[][][] pixels, String outputFileName) throws IOException {
		Color[] px = ColorQuantization.getPx(pixels);
		// then compute desired 'box' size according to # of pixels in image & desired
		// dynamic range
		ColorQuantization.PX_PER_BOX = (int) Math.ceil((double) ColorQuantization.IMG_WIDTH
				* (double) ColorQuantization.IMG_HEIGHT / (double) ColorQuantization.NEW_DYN_RANGE);
		// prepare to compute palette
		ColorQuantization.COUNT = 0;
		ColorQuantization.NEW_PALETTE = new Color[ColorQuantization.NEW_DYN_RANGE];
		// compute palette using median cut
		System.out.println("Computing palette...");
		ColorQuantization.cut(px, 0, ColorQuantization.NEW_DYN_RANGE, 0, ColorQuantization.NEW_DYN_RANGE, 0,
				ColorQuantization.NEW_DYN_RANGE);

		if (ColorQuantization.DEBUG) {
			ColorQuantization.printPalette();
		}

		System.out.println("Preparing output file contents...");
		String txt_file_contents = ColorQuantization.getCompressedFileContents();
		String[] content = txt_file_contents.split(" ");
		int[] intArray = new int[content.length];

		for (int i = 0; i < content.length; i++) {
			intArray[i] = Integer.parseInt(content[i]);
		}

		Map<Integer, String> huffmanCodes = HuffmanCoding.buildHuffmanCodes(intArray);

		// Compress the input array into a binary string
		String compressedData = HuffmanCoding.compressData(intArray, huffmanCodes);

		byte[] byteArray = convertBinaryStringToByteArray(compressedData);

		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {
			// Write the Huffman tree and compressed data to the output file
			oos.writeObject(huffmanCodes);
			oos.writeObject(byteArray);
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

	public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
		Color[][] img = null;

		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
			// Read the Huffman tree from the input file
			Map<Integer, String> huffmanCodes = (Map<Integer, String>) ois.readObject();

			// Read the compressed data
			byte[] byteArray = (byte[]) ois.readObject();
			String binaryString = convertByteArrayToBinaryString(byteArray);
			int[] decompressedData = HuffmanCoding.decompressData(binaryString, huffmanCodes);
			img = ColorQuantization.readCompressedFile(decompressedData);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
		return ColorQuantization.savePNG(img, "decompr.png");
	}
}

class HuffmanNode {
	int value;
	int frequency;
	HuffmanNode left;
	HuffmanNode right;

	HuffmanNode(int value, int frequency) {
		this.value = value;
		this.frequency = frequency;
	}
}

class HuffmanCoding {
	public static void main(String[] args) {
		int[] input = { 1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3 }; // Example input data

		// Build the Huffman tree and generate Huffman codes
		Map<Integer, String> huffmanCodes = buildHuffmanCodes(input);

		// Compress the input array into a binary string
		String compressedData = compressData(input, huffmanCodes);

		// Decompress the binary string into the original data
		int[] decompressedData = decompressData(compressedData, huffmanCodes);

	}

	public static Map<Integer, String> buildHuffmanCodes(int[] data) {
		Map<Integer, Integer> frequencyTable = buildFrequencyTable(data);
		PriorityQueue<HuffmanNode> minHeap = new PriorityQueue<>(Comparator.comparingInt(node -> node.frequency));

		// Create Huffman nodes for each unique integer with their frequencies
		for (Map.Entry<Integer, Integer> entry : frequencyTable.entrySet()) {
			minHeap.add(new HuffmanNode(entry.getKey(), entry.getValue()));
		}

		// Build Huffman tree
		while (minHeap.size() > 1) {
			HuffmanNode left = minHeap.poll();
			HuffmanNode right = minHeap.poll();
			HuffmanNode mergedNode = new HuffmanNode(-1, left.frequency + right.frequency);
			mergedNode.left = left;
			mergedNode.right = right;
			minHeap.add(mergedNode);
		}

		// Generate Huffman codes
		HuffmanNode root = minHeap.poll();
		Map<Integer, String> huffmanCodes = new HashMap<>();
		generateHuffmanCodes(root, "", huffmanCodes);

		return huffmanCodes;
	}

	public static Map<Integer, Integer> buildFrequencyTable(int[] data) {
		Map<Integer, Integer> frequencyTable = new HashMap<>();
		for (int value : data) {
			frequencyTable.put(value, frequencyTable.getOrDefault(value, 0) + 1);
		}
		return frequencyTable;
	}

	public static void generateHuffmanCodes(HuffmanNode node, String code, Map<Integer, String> huffmanCodes) {
		if (node == null) {
			return;
		}
		if (node.value != -1) {
			huffmanCodes.put(node.value, code);
		}
		generateHuffmanCodes(node.left, code + "0", huffmanCodes);
		generateHuffmanCodes(node.right, code + "1", huffmanCodes);
	}

	public static String compressData(int[] data, Map<Integer, String> huffmanCodes) {
		StringBuilder compressedData = new StringBuilder();
		for (int value : data) {
			compressedData.append(huffmanCodes.get(value));
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
}

class ColorQuantization {
	// number of colors to be computed for palette
	public static final int NEW_DYN_RANGE = 512;
	// used in cut() base case
	public static int PX_PER_BOX;
	// image attributes
	public static int IMG_WIDTH, IMG_HEIGHT;
	// contains all colors in new palette: computed in cut()
	public static Color[] NEW_PALETTE;
	// init with uncompressed image's original pixels
	public static Color[] ORIG_PX;
	// for testing: counting number of colors
	public static int COUNT;
	// enable debugging
	public static boolean DEBUG = false;
	// enable blurring to treat colour banding
	public static boolean BLUR = false;
	// specifies format of compressed file
	public static final String format = "\"image_width image_height number_of_colors palette pixels_key_values\"\n(palette specifies rgb colors with 3 integers -- order implies corresponding pixel key)/";

	// prepares & returns compressed file content based on image/palette attributes
	// & palette
	public static String getCompressedFileContents() {
		// file begins with image & palette attributes
		String file_contents = IMG_WIDTH + " " + IMG_HEIGHT + " " + NEW_DYN_RANGE + " ";
		// followed by palette
		for (Color c : NEW_PALETTE) {
			file_contents += c.getRed() + " " + c.getGreen() + " " + c.getBlue() + " ";
		}
		// followed by all pixels with their new colors encoded with ints in range
		// [0,NEW_DYN_RANGE)
		file_contents += getEncodedPx();
		return file_contents;
	}

	// computes & returns encoded pixel colors as ints in range [0,NEW_DYN_RANGE)
	// with the computed palette & original pixel colors
	public static String getEncodedPx() {
		String pxStr = "";
		// for each pixel in image, assign it the closest color in palette (in terms of
		// Euclidean space)
		for (Color origCol : ORIG_PX) {
			// index of color in palette & current distance from original color
			int closestColIndex = 0;
			int diffR = Math.abs(origCol.getRed() - NEW_PALETTE[closestColIndex].getRed());
			int diffG = Math.abs(origCol.getGreen() - NEW_PALETTE[closestColIndex].getGreen());
			int diffB = Math.abs(origCol.getBlue() - NEW_PALETTE[closestColIndex].getBlue());
			int currDist = (int) (Math.pow((double) diffR, 2.0) + (int) Math.pow((double) diffG, 2.0)
					+ (int) Math.pow((double) diffB, 2.0));
			;

			// iterate over colors in palette to find closest to current pixel
			for (int i = 1; i < NEW_PALETTE.length; i++) {
				// compare distances by pairs to find closest
				int compR = Math.abs(origCol.getRed() - NEW_PALETTE[i].getRed());
				int compG = Math.abs(origCol.getGreen() - NEW_PALETTE[i].getGreen());
				int compB = Math.abs(origCol.getBlue() - NEW_PALETTE[i].getBlue());
				int compDist = (int) (Math.pow((double) compR, 2.0) + (int) Math.pow((double) compG, 2.0)
						+ (int) Math.pow((double) compB, 2.0));

				// if new color is closer, replace
				if (compDist < currDist) {
					currDist = compDist;
					closestColIndex = i;
				}
			}
			pxStr += closestColIndex + " ";
		}
		return pxStr;
	}

	// px : all pixels in current box. we keep 'slicing the box' until it reaches
	// the desired size
	public static void cut(Color[] px, int startR, int endR, int startG, int endG, int startB, int endB) {
		// base case
		if (px.length <= PX_PER_BOX) {
			try {
				NEW_PALETTE[COUNT++] = findMeanColor(px); // store box & return
			} catch (IndexOutOfBoundsException e) {
				System.out.println("More boxes computed than expected; color skipped");
			}

			// o.w. divide box into 2 disjoint boxes
		} else/* if(COUNT < NEW_DYN_RANGE) */ {
			// compute ranges of each color ("length of each side of the current cube")
			int difR = endR - startR, difG = endG - startG, difB = endB - startB;

			// find longest side & sort with respect to the color
			if (difR >= difG && difR >= difB) {
				// cut along red
				px = sortByRed(px);

				// slice
				Color[] px1 = Arrays.copyOfRange(px, 0, px.length / 2);
				Color[] px2 = Arrays.copyOfRange(px, px.length / 2, px.length);

				// find red intensity of median
				int midR = px[px.length / 2].getRed();

				cut(px1, startR, midR, startG, endG, startB, endB);
				cut(px2, midR, endR, startG, endG, startB, endB);

			} else if (difG >= difR && difG >= difB) {
				// cut along green
				px = sortByGreen(px);

				// slice
				Color[] px1 = Arrays.copyOfRange(px, 0, px.length / 2);
				Color[] px2 = Arrays.copyOfRange(px, px.length / 2, px.length);

				// find green intensity of median
				int midG = px[px.length / 2].getGreen();

				cut(px1, startR, endR, startG, midG, startB, endB);
				cut(px2, startR, endR, midG, endG, startB, endB);

			} else {
				// cut along blue
				px = sortByBlue(px);

				// slice
				Color[] px1 = Arrays.copyOfRange(px, 0, px.length / 2);
				Color[] px2 = Arrays.copyOfRange(px, px.length / 2, px.length);

				// find blue intensity of median
				int midB = px[px.length / 2].getBlue();

				cut(px1, startR, endR, startG, endG, startB, midB);
				cut(px2, startR, endR, startG, endG, midB, endB);
			}
		}
	}

	// transfers colors in buckets to a Color array
	public static Color[] bucketsToArr(ArrayList<LinkedList<Color>> buckets, int len) {
		// System.out.println(buckets);
		Color[] sorted = new Color[len];
		int index = 0;
		// transfer colors in buckets to an array
		for (int i = 0; i < NEW_DYN_RANGE; i++) {
			LinkedList<Color> colors = buckets.get(i);
			while (!colors.isEmpty()) {
				sorted[index++] = colors.poll();
			}
		}
		return sorted;
	}

	// runs bucket sort on px, ordering by red values
	public static Color[] sortByRed(Color[] px) {
		// buckets numbered 0-->255
		ArrayList<LinkedList<Color>> buckets = new ArrayList<LinkedList<Color>>(NEW_DYN_RANGE);
		// go through array list and create lists
		for (int i = 0; i < NEW_DYN_RANGE; i++) {
			buckets.add(new LinkedList<Color>());
		}

		// place in buckets
		for (Color c : px) {
			// bucket number corresponds to red channel intensity of color
			LinkedList<Color> list = buckets.get(c.getRed());
			// place in bucket
			list.add(c);
		}
		Color[] result = bucketsToArr(buckets, px.length);
		return result;
	}

	// runs bucket sort on px, ordering by green values
	public static Color[] sortByGreen(Color[] px) {
		// buckets numbered 0-->255
		ArrayList<LinkedList<Color>> buckets = new ArrayList<LinkedList<Color>>(NEW_DYN_RANGE);
		// go through array list and create lists
		for (int i = 0; i < NEW_DYN_RANGE; i++) {
			buckets.add(new LinkedList<Color>());
		}

		// place in buckets
		for (Color c : px) {
			// bucket number corresponds to red channel intensity of color
			LinkedList<Color> list = buckets.get(c.getGreen());
			// place in bucket
			list.add(c);
		}
		Color[] result = bucketsToArr(buckets, px.length);
		return result;
	}

	// runs bucket sort on px, ordering by blue values
	public static Color[] sortByBlue(Color[] px) {
		// buckets numbered 0-->255
		ArrayList<LinkedList<Color>> buckets = new ArrayList<LinkedList<Color>>();
		// go through array list and create lists
		for (int i = 0; i < NEW_DYN_RANGE; i++) {
			buckets.add(new LinkedList<Color>());
		}

		// place in buckets
		for (Color c : px) {
			// bucket number corresponds to red channel intensity of color
			LinkedList<Color> list = buckets.get(c.getBlue());
			// place in bucket
			list.add(c);
		}
		Color[] result = bucketsToArr(buckets, px.length);
		return result;
	}

	// find average color of given array
	public static Color findMeanColor(Color[] px) {
		int sumR = 0, sumG = 0, sumB = 0;
		// sum intensities per color channel
		for (Color c : px) {
			sumR += c.getRed();
			sumG += c.getGreen();
			sumB += c.getBlue();
		}
		// find ave intensity per color channel
		int meanR = (int) (((double) sumR / (double) px.length) + 0.5),
				meanG = (int) (((double) sumG / (double) px.length) + 0.5),
				meanB = (int) (((double) sumB / (double) px.length) + 0.5);
		// clamp
		meanR = (meanR > 255 ? 255 : meanR);
		meanG = (meanG > 255 ? 255 : meanG);
		meanB = (meanB > 255 ? 255 : meanB);
		return (new Color(meanR, meanG, meanB));
	}

	// prints palette to see colors
	public static void printPalette() {
		Color[][] squarePalette = new Color[16][16];
		int ctr = 0;
		for (int i = 0; i < squarePalette.length; i++) {
			for (int j = 0; j < squarePalette[0].length; j++) {
				try {
					squarePalette[i][j] = NEW_PALETTE[ctr++];
				} catch (IndexOutOfBoundsException e) {
					System.out.println(e);
					System.exit(-1);
				}
			}
		}
		savePNG(squarePalette, "palette.png");
	}

	public static void dump2DArr(int[][] arr) {
		for (int[] row : arr) {
			for (int num : row) {
				System.out.print(num + " ");
			}
			System.out.println();
		}
	}

	// print colors in array
	public static void dumpCol(Color[] arr) {
		for (Color c : arr) {
			System.out.print("r: " + c.getRed() + " g: " + c.getGreen() + " b: " + c.getBlue() + ", ");
		}
	}

	// decodes Color value of each pixel from supplied key values and corresponding
	// palette
	public static Color[][] decompressAndBlur(Color[] palette, int[][] px) {
		Color[][] img = new Color[px.length][px[0].length];

		int[][] sumDiff = new int[px.length][px[0].length];
		int minNotZero = 255 * 3, totalSumDiff = 0, numDiff = 0;
		boolean checkUp = false;
		boolean checkLeft, checkRight;
		// for each pixel: find & store color associated with key value provided
		// can check above pixel from the second row til the end
		for (int i = 0; i < px.length; i++, checkUp = true) {
			checkLeft = false; // can check left pixel from second pixel on row til end
			checkRight = true; // can check right pixel from first pixel on row til 2nd to last
			for (int j = 0; j < px[0].length; j++, checkLeft = true) {
				img[i][j] = palette[px[i][j]]; // palette[key value] = Color instance

				// cannot check upRight pixel if at rightmost pixel on row
				if (j == px[0].length - 1) {
					checkRight = false;
				}

				int currRed = img[i][j].getRed(), currGreen = img[i][j].getGreen(), currBlue = img[i][j].getBlue();
				int diff;
				if (checkUp) {
					int upRed = img[i - 1][j].getRed(), upGreen = img[i - 1][j].getGreen(), upBlue = img[i - 1][j].getBlue();
					diff = Math.abs(currRed - upRed) + Math.abs(currGreen - upGreen) + Math.abs(currRed - upBlue);
					sumDiff[i][j] += diff;
					sumDiff[i - 1][j] += diff;

					numDiff++;
					totalSumDiff += diff;
					if (diff < minNotZero && diff != 0) {
						minNotZero = diff;
					}
				}
				if (checkLeft) {
					int leftRed = img[i][j - 1].getRed(), leftGreen = img[i][j - 1].getGreen(),
							leftBlue = img[i][j - 1].getBlue();
					diff = Math.abs(currRed - leftRed) + Math.abs(currGreen - leftGreen) + Math.abs(currRed - leftBlue);
					sumDiff[i][j] += diff;
					sumDiff[i][j - 1] += diff;

					numDiff++;
					totalSumDiff += diff;
					if (diff < minNotZero && diff != 0) {
						minNotZero = diff;
					}
				}
				if (checkLeft && checkUp) {
					int upLeftRed = img[i - 1][j - 1].getRed(), upLeftGreen = img[i - 1][j - 1].getGreen(),
							upLeftBlue = img[i - 1][j - 1].getBlue();
					diff = Math.abs(currRed - upLeftRed) + Math.abs(currGreen - upLeftGreen) + Math.abs(currRed - upLeftBlue);
					sumDiff[i][j] += diff;
					sumDiff[i - 1][j - 1] += diff;

					numDiff++;
					totalSumDiff += diff;
					if (diff < minNotZero && diff != 0) {
						minNotZero = diff;
					}
				}
				if (checkRight && checkUp) {
					int upRightRed = img[i - 1][j + 1].getRed(), upRightGreen = img[i - 1][j + 1].getGreen(),
							upRightBlue = img[i - 1][j + 1].getBlue();
					diff = Math.abs(currRed - upRightRed) + Math.abs(currGreen - upRightGreen) + Math.abs(currRed - upRightBlue);
					sumDiff[i][j] += diff;
					sumDiff[i - 1][j + 1] += diff;

					numDiff++;
					totalSumDiff += diff;
					if (diff < minNotZero && diff != 0) {
						minNotZero = diff;
					}
				}
			}
		}
		int aveDiff = totalSumDiff / (numDiff);
		return blur(minNotZero, aveDiff, img, sumDiff);
	}

	public static Color[][] blur(int minDiffNotZero, int aveDiff, Color[][] img, int[][] sumDiff) {
		double factor = 1.0 / 5.0;
		int radius = 3;
		return applyBlur(minDiffNotZero, aveDiff / 5, sumDiff, img, factor, 1);
	}

	// applies a NxN filter to a grayscale image, where N ~ odd
	public static Color[][] applyBlur(int lb, int ub, int[][] sumDiff, Color[][] img, double factor, int radius) {
		Color[][] newImage = new Color[img.length][img[0].length];
		// for each pixel in the image
		for (int i = 0; i < img.length; i++) {
			for (int j = 0; j < img[0].length; j++) {

				// if pixel colour differs from neighboring colours by small range, blur (colour
				// banding)
				if (lb <= sumDiff[i][j] && sumDiff[i][j] <= ub) {
					double newRed = 0, newGreen = 0, newBlue = 0;
					double lastRed = img[i][j].getRed(), lastGreen = img[i][j].getGreen(), lastBlue = img[i][j].getBlue();
					// apply filter to pixel: nested loops iterate through filter & image region
					for (int col = i - radius; col <= i + radius; col++) {
						for (int row = j - radius; row <= j + radius; row++) {
							// if not in range, set equal to color at i,j (current px colour)
							int diff = 0;
							if (col < 0 || img.length - 1 < col || row < 0 || img[0].length - 1 < row) {
								newRed += (factor * (double) img[i][j].getRed());
								newGreen += (factor * (double) img[i][j].getGreen());
								newBlue += (factor * (double) img[i][j].getBlue());
							} else {
								diff += Math.abs(img[col][row].getRed() - img[i][j].getRed());
								diff += Math.abs(img[col][row].getGreen() - img[i][j].getGreen());
								diff += Math.abs(img[col][row].getBlue() - img[i][j].getBlue());

								if (lb < diff && diff < ub) {
									newRed += (factor * (double) img[col][row].getRed());
									newGreen += (factor * (double) img[col][row].getGreen());
									newBlue += (factor * (double) img[col][row].getBlue());
								} else {
									newRed = lastRed;
									newGreen = lastGreen;
									newBlue = lastBlue;
								}
							}
							lastRed = newRed;
							lastGreen = newGreen;
							lastBlue = newBlue;
						}
					}
					// clamp
					if (255.0 < newRed) {
						newRed = 255.0;
					}
					if (255.0 < newGreen) {
						newGreen = 255.0;
					}
					if (255.0 < newBlue) {
						newBlue = 255.0;
					}
					// save pixel
					newImage[i][j] = new Color((int) newRed, (int) newGreen, (int) newBlue);
				} else {
					newImage[i][j] = img[i][j];
				}
			}
		}
		return newImage;
	}

	public static Color[][] decompress(Color[] palette, int[][] px) {
		if (BLUR) {
			return decompressAndBlur(palette, px);
		}

		Color[][] img = new Color[px.length][px[0].length];
		// for each pixel: find & store color associated with key value provided
		// can check above pixel from the second row til the end
		for (int i = 0; i < px.length; i++) {
			for (int j = 0; j < px[0].length; j++) {

				img[i][j] = palette[px[i][j]]; // palette[key value] = Color instance
			}
		}
		return img;
	}

	// reads formatted txt file to acquire image data; returns 2D Color array rep of
	// image
	public static Color[][] readCompressedFile(int[] compressed) throws IOException, ClassNotFoundException {
		Color[][] image = null;

		int imgWidth = compressed[0];
		int imgHeight = compressed[1];
		int numColors = compressed[2];

		Color[] palette = new Color[numColors];
		// read in all colors in palette
		int count = 3;
		for (int i = 0; i < numColors; i++) {
			// System.out.println("r: "+compressed[count]+" g: "+compressed[count+1]+" b:
			// "+compressed[count+2]);
			palette[i] = new Color(compressed[count++], compressed[count++], compressed[count++]);
		}
		// System.out.println("palette: "+Arrays.toString(palette));

		int[][] px = new int[imgWidth][imgHeight];
		// collects key values for all pixels
		for (int i = 0; i < imgWidth; i++) {
			for (int j = 0; j < imgHeight; j++) {
				px[i][j] = compressed[count++];
			}
		}
		// decompresses info into 2D Color array (the image!)
		image = decompress(palette, px);

		return image;
	}

	public static Color[] getPx(int[][][] pixels) {
		IMG_WIDTH = pixels[0].length;
		IMG_HEIGHT = pixels.length;

		Color[] imgPx = new Color[IMG_WIDTH * IMG_HEIGHT];
		ORIG_PX = new Color[IMG_WIDTH * IMG_HEIGHT];

		int ctr = 0;
		for (int i = 0; i < IMG_WIDTH; i++) {
			for (int j = 0; j < IMG_HEIGHT; j++) {
				int red = pixels[j][i][0];
				int green = pixels[j][i][1];
				int blue = pixels[j][i][2];
				int rgb = (red << 16) | (green << 8) | blue;
				imgPx[ctr] = new Color(rgb);
				ORIG_PX[ctr++] = new Color(rgb);
			}
		}

		System.err.printf("Read a %d by %d image\n", IMG_WIDTH, IMG_HEIGHT);
		return imgPx;
	}

	public static int[][][] savePNG(Color[][] imagePixels, String image_filename) {
		int width = imagePixels.length;
		int height = imagePixels[0].length;
		int[][][] pixelData = new int[height][width][3];
		BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				int color = imagePixels[x][y].getRGB();
				int red = (color >> 16) & 0xFF;
				int green = (color >> 8) & 0xFF;
				int blue = color & 0xFF;
				pixelData[y][x][0] = red;
				pixelData[y][x][1] = green;
				pixelData[y][x][2] = blue;
			}
		return pixelData;
	}
}