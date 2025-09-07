import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import javax.imageio.ImageIO;
import java.util.Map;
import javax.swing.JOptionPane;
import java.util.HashMap;
import java.util.Arrays;

public class Main2 {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Image Processing Tool");
            System.out.println("--------------------------------------------");
            
            while (true) {
                try {
                    System.out.print("\nEnter image name (or 'q' to quit): ");
                    // String userInput = scanner.nextLine().trim();
                    String userInput = "Filament";
                    
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
                    
                    // Convert to grayscale if needed
                    BufferedImage processedImage;
                    if (ImageProcessing2.isGrayscale(originalImage)) {
                        processedImage = originalImage;
                        System.out.println("Image is already grayscale.");
                    } else {
                        processedImage = ImageProcessing2.convertToGrayscale(originalImage);
                        System.out.println("Converted color image to grayscale.");
                    }
                    
                    // Main processing menu
                    boolean continueProcessing = true;
                    while (continueProcessing) {
                        System.out.println("\n=== Image Processing Menu ===");
                        System.out.println("1. Global Histogram Equalization");
                        System.out.println("2. Local Histogram Equalization");
                        System.out.println("3. Local Gamma Correction");
                        System.out.println("4. Show Image Statistics");
                        System.out.println("5. Exit");
                        System.out.print("Select option (1-5): ");
                        
                        String choice = scanner.nextLine();
                        
                        switch (choice) {
                            case "1": // Global Histogram Equalization
                                BufferedImage beforeEqualization = processedImage;
                                BufferedImage afterEqualization = ImageProcessing2.globalHistogramEqualization(processedImage);
                                
                                List<BufferedImage> resultImages = List.of(beforeEqualization, afterEqualization);
                                ImageDisplay2.displayImages(resultImages, "Before vs After Histogram Equalization");
                                break;

                            case "2": // Local Histogram Equalization with Optimization
                                try {
                                    // Find optimal parameters for each neighborhood size
                                    List<Map<String, Object>> optimalResults = ImageProcessing2.getAllOptimalLocalHistograms(processedImage);
                                    
                                    // Extract results for each neighborhood size
                                    Map<String, Object> result3x3 = optimalResults.get(0);
                                    Map<String, Object> result7x7 = optimalResults.get(1);
                                    Map<String, Object> result11x11 = optimalResults.get(2);
                                    
                                    // Extract optimal parameters
                                    double optimalK0_3x3 = (Double) result3x3.get("optimalK0");
                                    double optimalK1_3x3 = (Double) result3x3.get("optimalK1");
                                    double optimalK2_3x3 = (Double) result3x3.get("optimalK2");
                                    
                                    double optimalK0_7x7 = (Double) result7x7.get("optimalK0");
                                    double optimalK1_7x7 = (Double) result7x7.get("optimalK1");
                                    double optimalK2_7x7 = (Double) result7x7.get("optimalK2");
                                    
                                    double optimalK0_11x11 = (Double) result11x11.get("optimalK0");
                                    double optimalK1_11x11 = (Double) result11x11.get("optimalK1");
                                    double optimalK2_11x11 = (Double) result11x11.get("optimalK2");
                                    
                                    // Extract enhanced images
                                    BufferedImage enhanced3x3 = (BufferedImage) result3x3.get("optimalImage");
                                    BufferedImage enhanced7x7 = (BufferedImage) result7x7.get("optimalImage");
                                    BufferedImage enhanced11x11 = (BufferedImage) result11x11.get("optimalImage");
                                    
                                    // Create image list for display
                                    List<BufferedImage> allImages = new ArrayList<>();
                                    allImages.add(processedImage); // Original
                                    allImages.add(enhanced3x3);
                                    allImages.add(enhanced7x7);
                                    allImages.add(enhanced11x11);
                                    
                                    // Create informative title
                                    String title = String.format(
                                        "Local Histogram Equalization (Optimal) - " +
                                        "3x3(k0=%.2f,k1=%.3f,k2=%.2f), " +
                                        "7x7(k0=%.2f,k1=%.3f,k2=%.2f), " +
                                        "11x11(k0=%.2f,k1=%.3f,k2=%.2f)",
                                        optimalK0_3x3, optimalK1_3x3, optimalK2_3x3,
                                        optimalK0_7x7, optimalK1_7x7, optimalK2_7x7,
                                        optimalK0_11x11, optimalK1_11x11, optimalK2_11x11
                                    );
                                    
                                    ImageDisplay2.displayImages(allImages, title);
                                    
                                    // Print statistics
                                    System.out.println("\n=== OPTIMAL LOCAL HISTOGRAM EQUALIZATION STATISTICS ===");
                                    
                                    System.out.println("3x3 Neighborhood (k0=" + optimalK0_3x3 + ", k1=" + optimalK1_3x3 + ", k2=" + optimalK2_3x3 + "):");
                                    ImageProcessing2.printStatistics((Map<String, Object>) result3x3.get("optimalStats"));
                                    
                                    System.out.println("\n7x7 Neighborhood (k0=" + optimalK0_7x7 + ", k1=" + optimalK1_7x7 + ", k2=" + optimalK2_7x7 + "):");
                                    ImageProcessing2.printStatistics((Map<String, Object>) result7x7.get("optimalStats"));
                                    
                                    System.out.println("\n11x11 Neighborhood (k0=" + optimalK0_11x11 + ", k1=" + optimalK1_11x11 + ", k2=" + optimalK2_11x11 + "):");
                                    ImageProcessing2.printStatistics((Map<String, Object>) result11x11.get("optimalStats"));
                                    
                                } catch (Exception e) {
                                    System.out.println("Error in optimized local histogram equalization: " + e.getMessage());
                                    e.printStackTrace();
                                    
                                    // Fallback to original implementation if optimization fails
                                    double k0 = 0.09;
                                    double k1 = 0.02;
                                    double k2 = 0.3;
                                    
                                    List<BufferedImage> localResults = ImageProcessing2.getAllLocalHistogramEqualizations(processedImage, k0, k1, k2);
                                    List<BufferedImage> allImages = new ArrayList<>();
                                    allImages.add(processedImage);
                                    allImages.addAll(localResults);
                                    
                                    String title = String.format("Local Histogram Equalization (k0=%.2f, k1=%.3f, k2=%.2f) - Original, 3x3, 7x7, 11x11", 
                                                                k0, k1, k2);
                                    ImageDisplay2.displayImages(allImages, title);
                                }
                                break;

                            case "3": // Local Gamma Correction
                                // Define gamma values to test
                                double[] gammaValues = {0.5, 0.8, 1.0, 1.2, 1.5, 2.0, 2.5, 3.0};
                                
                                // Get optimal gamma for each neighborhood size
                                Map<String, Object> result5x5 = ImageProcessing2.findOptimalGamma(processedImage, 5, gammaValues);
                                Map<String, Object> result9x9 = ImageProcessing2.findOptimalGamma(processedImage, 9, gammaValues);
                                Map<String, Object> result15x15 = ImageProcessing2.findOptimalGamma(processedImage, 15, gammaValues);
                                
                                // Extract optimal gamma values and images
                                double optimalGamma5x5 = (Double) result5x5.get("optimalGamma");
                                double optimalGamma9x9 = (Double) result9x9.get("optimalGamma");
                                double optimalGamma15x15 = (Double) result15x15.get("optimalGamma");
                                
                                BufferedImage gamma5x5 = (BufferedImage) result5x5.get("optimalImage");
                                BufferedImage gamma9x9 = (BufferedImage) result9x9.get("optimalImage");
                                BufferedImage gamma15x15 = (BufferedImage) result15x15.get("optimalImage");
                                
                                List<BufferedImage> allImages2 = new ArrayList<>();
                                allImages2.add(processedImage); // Original
                                allImages2.add(gamma5x5);
                                allImages2.add(gamma9x9);
                                allImages2.add(gamma15x15);
                                
                                // Create informative title
                                String title2 = String.format("Local Gamma Correction - Optimal Values: 5x5(γ=%.2f), 9x9(γ=%.2f), 15x15(γ=%.2f)", 
                                                            optimalGamma5x5, optimalGamma9x9, optimalGamma15x15);
                                
                                ImageDisplay2.displayImages(allImages2, title2);

                                @SuppressWarnings("unchecked")
                                Map<String, Object> optimalStats = (Map<String, Object>) result15x15.get("optimalStats");
                                
                                // Print statistics for each result
                                System.out.println("\n=== LOCAL GAMMA CORRECTION STATISTICS ===");
                                System.out.println("5x5 Neighborhood (γ=" + optimalGamma5x5 + "):");
                                ImageProcessing2.printStatistics((Map<String, Object>) result5x5.get("optimalStats"));
                                
                                System.out.println("\n9x9 Neighborhood (γ=" + optimalGamma9x9 + "):");
                                ImageProcessing2.printStatistics((Map<String, Object>) result9x9.get("optimalStats"));
                                
                                System.out.println("\n15x15 Neighborhood (γ=" + optimalGamma15x15 + "):");
                                ImageProcessing2.printStatistics((Map<String, Object>) result15x15.get("optimalStats"));
                                break;
                                
                            case "4": // Show Image Statistics
                                Map<String, Object> stats = ImageProcessing2.getImageStatistics(processedImage);
                                ImageProcessing2.printStatistics(stats);
                                break;

                            case "5": // Exit
                                System.out.println("\nExiting program. Goodbye!");
                                System.exit(0);
                                break;
                                
                            default:
                                System.out.println("Invalid option! Please choose 1-4.");
                        }
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