import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Image Processing Tool");
            System.out.println("--------------------------------------------");
            
            while (true) {
                try {
                    System.out.print("\nEnter image name (or 'q' to quit): ");
                    String userInput = scanner.nextLine().trim();
                    
                    if (userInput.equalsIgnoreCase("q")) {
                        System.out.println("\nExiting program. Goodbye!");
                        System.exit(0); 
                    }
                    
                    if (userInput.isEmpty()) {
                        System.out.println("Error: Please enter a filename.");
                        continue;
                    }
                    
                    String fileName = ImageFileFinder.getValidFilename(userInput);
                    if (fileName == null) {
                        System.out.println("Error: No matching image file found!");
                        System.out.println("Supported formats: .jpg, .png, .jpeg");
                        continue;
                    }
                    
                    // Load the image file
                    System.out.println("\nLoading file: " + fileName);
                    BufferedImage originalImage = ImageIO.read(new File(fileName));
                    BufferedImage processedImage = originalImage;
                    List<BufferedImage> imagesToDisplay = new ArrayList<>();
                    
                    // Store the original image for comparison in custom grayscale
                    BufferedImage beforeCustomGrayscale = null;
                    
                    // Convert to grayscale option
                    System.out.print("Convert to grayscale? (y/n): ");
                    boolean convertToGrayscale = scanner.nextLine().equalsIgnoreCase("y");
                    if (convertToGrayscale) {
                        processedImage = ImageProcessing.convertToGrayscale(originalImage);
                        // Grayscale: Show only the grayscale image
                        imagesToDisplay.clear();
                        imagesToDisplay.add(processedImage);
                    } else {
                        processedImage = originalImage;
                    }

                    // Resize options
                    System.out.print("Resize the image? (y/n): ");
                    boolean resizeImage = scanner.nextLine().equalsIgnoreCase("y");
                    if (resizeImage) {
                        System.out.println("Choose resize option:");
                        System.out.println("1. Zoom in (enlarge)");
                        System.out.println("2. Zoom out (shrink)");
                        System.out.print("Enter choice (1 or 2): ");
                        int choice = Integer.parseInt(scanner.nextLine());
                        while (choice < 1 || choice > 2) {
                            System.out.print("Invalid choice. Enter 1 (zoom in) or 2 (zoom out): ");
                            choice = Integer.parseInt(scanner.nextLine());
                        }
                        
                        System.out.print("Enter zoom factor (e.g., 2 for 2x): ");
                        int factor = Integer.parseInt(scanner.nextLine());
                        while (factor <= 0) {
                            System.out.print("Factor must be positive. Enter again: ");
                            factor = Integer.parseInt(scanner.nextLine());
                        }
                        
                        System.out.print("Use bilinear interpolation? (y/n): ");
                        String method = scanner.nextLine().equalsIgnoreCase("y") ? "bilinear" : "replication";
                        
                        processedImage = (choice == 1) 
                            ? ImageProcessing.zoom(processedImage, factor, method)
                            : ImageProcessing.shrink(processedImage, factor, method);
                        
                        // Resize: Show only the resized image
                        System.out.println("\nDisplaying resized image...");
                        System.out.println("New dimensions: " + processedImage.getWidth() + "x" + processedImage.getHeight());
                        ImageDisplay.showImageTrueSize(processedImage, "Resized Image");
                        
                        // Ask if user wants to continue with other operations
                        System.out.print("\nContinue with other operations on this resized image? (y/n): ");
                        boolean continueProcessing = scanner.nextLine().equalsIgnoreCase("y");
                        if (!continueProcessing) {
                            continue; // Skip to next iteration
                        }
                    }

                    // CUSTOM GRAYSCALE ENHANCEMENT OPTION
                    System.out.print("\nApply custom 8-bit grayscale enhancement? (y/n): ");
                    boolean applyCustomGrayscale = scanner.nextLine().equalsIgnoreCase("y");
                    if (applyCustomGrayscale) {
                        if (!ImageProcessing.isGrayscale(processedImage)) {
                            System.out.println("Warning: Custom grayscale enhancement requires a grayscale image.");
                            System.out.println("Skipping this transformation...");
                        } else {
                            beforeCustomGrayscale = processedImage;
                            processedImage = ImageProcessing.customGrayscaleTransform(processedImage);
                            
                            // Custom Grayscale: Show before and after images
                            List<BufferedImage> customGrayscaleImages = new ArrayList<>();
                            customGrayscaleImages.add(beforeCustomGrayscale);
                            customGrayscaleImages.add(processedImage);
                            
                            System.out.println("Custom grayscale enhancement applied!");
                            System.out.println("Displaying before and after comparison...");
                            ImageDisplay.showImagesGrid(customGrayscaleImages, "Custom Grayscale: Before vs After");
                            System.out.println("Transformation: [0,0.25]∪[0.75,1]→0.8333, (0.25,0.75)→linear");
                        }
                    }

                    // Power-law transformation option
                    System.out.print("\nApply power-law transformation? (y/n): ");
                    boolean applyPowerLaw = scanner.nextLine().equalsIgnoreCase("y");
                    if (applyPowerLaw) {
                        System.out.println("\nPower-law Options:");
                        System.out.println("1. Single transformation");
                        System.out.println("2. Multiple transformations (test different parameters)");
                        System.out.print("Enter choice (1-2): ");
                        int powerLawChoice = Integer.parseInt(scanner.nextLine());
                        
                        List<BufferedImage> powerLawImages = new ArrayList<>();
                        
                        if (powerLawChoice == 1) {
                            // Single transformation
                            System.out.print("Enter c value (e.g., 0.4, 1.0, 1.6): ");
                            double c = Double.parseDouble(scanner.nextLine());
                            System.out.print("Enter gamma value (e.g., 0.3, 2.4): ");
                            double gamma = Double.parseDouble(scanner.nextLine());
                            
                            BufferedImage powerLawResult = ImageProcessing.powerLawTransform(processedImage, c, gamma);
                            
                            // Power-law single: Show original + transformed
                            powerLawImages.add(processedImage); // Original
                            powerLawImages.add(powerLawResult); // Transformed
                            
                            processedImage = powerLawResult;
                        } 
                        else if (powerLawChoice == 2) {
                            // Multiple transformations
                            double[] cValues = {0.4, 1.0, 1.6};
                            double[] gammaValues = {0.3, 2.4};
                            
                            System.out.println("\nTesting all combinations of:");
                            System.out.println("c values: " + Arrays.toString(cValues));
                            System.out.println("gamma values: " + Arrays.toString(gammaValues));
                            
                            BufferedImage[] powerLawResults = ImageProcessing.transformMultiple(
                                processedImage, cValues, gammaValues);
                            
                            // Power-law multiple: Show original + all transformed images
                            powerLawImages.add(processedImage); // Original
                            Collections.addAll(powerLawImages, powerLawResults);
                        }
                        
                        // Display power-law results
                        System.out.println("Displaying power-law transformation results...");
                        if (powerLawImages.size() > 1) {
                            ImageDisplay.showImagesGrid(powerLawImages, "Power-law Transformation Results");
                        }
                        
                        // Display power-law results
                        System.out.println("Displaying power-law transformation results...");
                        if (powerLawImages.size() > 1) {
                            ImageDisplay.showImagesGrid(powerLawImages, "Power-law Transformation Results");
                        }
                        
                        // Update the processed image to the last transformation
                        if (!powerLawImages.isEmpty()) {
                            processedImage = powerLawImages.get(powerLawImages.size() - 1);
                        }
                    }
                    
                    // Final display if no specific display was shown for the last operation
                    if (!applyCustomGrayscale && !applyPowerLaw && !resizeImage && !convertToGrayscale) {
                        // If no operations were performed, show the original image
                        ImageDisplay.showImage(originalImage, "Original Image");
                    }
                    
                } catch (NumberFormatException e) {
                    System.err.println("Error: Please enter a valid number.");
                } catch (Exception e) {
                    System.err.println("\nError: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}