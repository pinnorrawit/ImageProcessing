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
                    
                    // Convert to grayscale option
                    System.out.print("Convert to grayscale? (y/n): ");
                    if (scanner.nextLine().equalsIgnoreCase("y")) {
                        processedImage = ImageProcessing.convertToGrayscale(originalImage);
                        imagesToDisplay.add(processedImage);
                    } else {
                        processedImage = originalImage;
                        imagesToDisplay.add(originalImage);
                    }

                    // Resize options
                    System.out.print("Resize the image? (y/n): ");
                    if (scanner.nextLine().equalsIgnoreCase("y")) {
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
                        imagesToDisplay.add(processedImage);
                    }

                    // Power-law transformation option
                    System.out.print("\nApply power-law transformation? (y/n): ");
                    if (scanner.nextLine().equalsIgnoreCase("y")) {
                        System.out.println("\nPower-law Options:");
                        System.out.println("1. Single transformation");
                        System.out.println("2. Multiple transformations (test different parameters)");
                        System.out.print("Enter choice (1-2): ");
                        int powerLawChoice = Integer.parseInt(scanner.nextLine());
                        
                        if (powerLawChoice == 1) {
                            // Single transformation
                            System.out.print("Enter c value (e.g., 0.4, 1.0, 1.6): ");
                            double c = Double.parseDouble(scanner.nextLine());
                            System.out.print("Enter gamma value (e.g., 0.3, 2.4): ");
                            double gamma = Double.parseDouble(scanner.nextLine());
                            
                            processedImage = ImageProcessing.powerLawTransform(processedImage, c, gamma);
                            imagesToDisplay.add(processedImage);
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
                            
                            // Add all transformed images to display list
                            Collections.addAll(imagesToDisplay, powerLawResults);
                        }
                    }
                    
                    // Display all processed images
                    if (imagesToDisplay.size() > 1) {
                        System.out.println("\nDisplaying " + imagesToDisplay.size() + " image variants...");
                        ImageDisplay.showImagesGrid(imagesToDisplay, "Image Processing Results");
                    } else {
                        ImageDisplay.showImage(processedImage, "Processed Image");
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
