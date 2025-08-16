import java.awt.image.BufferedImage;
import java.io.File;
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
                        ImageDisplay.closeAll();
                        return;
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
                    File imageFile = new File(fileName);
                    BufferedImage originalImage = ImageIO.read(imageFile);
                    
                    // Process image
                    System.out.println("Convert image to grayscale? (yes/no)");
                    String grayscaleChoice = scanner.nextLine().toLowerCase();
                    BufferedImage processedImage = originalImage;

                    if (grayscaleChoice.equals("yes")) {
                        System.out.println("Converting to grayscale...");
                        processedImage = ImageProcessing.convertToGrayscale(originalImage);
                        System.out.println("Conversion successful!");
                    }

                    // Resize options
                    System.out.println("\nResize the image? (yes/no):");
                    String resizeChoice = scanner.nextLine().toLowerCase();

                    if (resizeChoice.equals("yes")) {
                        System.out.println("Zoom (enlarge) or shrink the image? (zoom/shrink):");
                        String resizeType = scanner.nextLine().toLowerCase();
                        
                        System.out.println("Enter the factor (e.g., 2 for 2x):");
                        int factor = Integer.parseInt(scanner.nextLine());
                        
                        System.out.println("Select interpolation method (replication/bilinear):");
                        String method = scanner.nextLine().toLowerCase();
                        
                        if (resizeType.equals("zoom")) {
                            processedImage = ImageProcessing.zoom(processedImage, factor, method);
                        } else {
                            processedImage = ImageProcessing.shrink(processedImage, factor, method);
                        }
                    } else {
                        System.out.println("No resizing will be done.");
                    }

                    // Display final image
                    String title = (grayscaleChoice.equals("yes") ? "Grayscale" : "Original") + 
                                 (resizeChoice.equals("yes") ? " + Resized" : "") + 
                                 ": " + fileName;
                    ImageDisplay.showImage(processedImage, title);
                    
                    System.out.println("\nThe processed image is now displayed.");
                    System.out.println("You may close the image window to continue.");
                    
                } catch (NumberFormatException e) {
                    System.err.println("Error: Please enter a valid number for the factor.");
                } catch (Exception e) {
                    System.err.println("\nError: " + e.getMessage());
                    System.err.println("Please try again.");
                }
            }
        }
    }
}