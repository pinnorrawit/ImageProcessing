import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Scanner;

public class Main3 {
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
                        System.out.println("Supported formats: .jpg, .png, .jpeg, .JPG, .PNG, .JPEG");
                        continue;
                    }

                    // Load the image file
                    System.out.println("\nLoading file: " + fileName);
                    BufferedImage originalImage = ImageIO.read(new File(fileName));
                    int originalWidth = originalImage.getWidth();
                    int originalHeight = originalImage.getHeight();
                    System.out.println("Image dimensions: " + originalWidth + "x" + originalHeight);

                    // Main processing menu
                    boolean continueProcessing = true;
                    while (continueProcessing) {
                        // Add to the menu system
                        System.out.println("1. Notch Low-pass filter");
                        System.out.println("2. Notch High-pass filter");
                        System.out.println("3. Gaussian Low-pass filter");
                        System.out.println("4. Gaussian High-pass filter");
                        System.out.println("5. Remove Periodic Noise");
                        System.out.println("6. Back to main menu");
                        System.out.print("Select option (1-6): ");
                        
                        String choice = scanner.nextLine();
                        
                        switch (choice) {
                            case "1": // Notch Low-pass filter
                                ImageProcessing3.Complex[][] preProcLP = ImageProcessing3.preProcessComplexs(originalImage);
                                ImageProcessing3.Complex[][] lowpass10 = ImageProcessing3.applyNotchLowPassFilter(preProcLP, 10);
                                ImageProcessing3.Complex[][] lowpass50 = ImageProcessing3.applyNotchLowPassFilter(preProcLP, 50);
                                ImageProcessing3.Complex[][] lowpass100 = ImageProcessing3.applyNotchLowPassFilter(preProcLP, 100);

                                BufferedImage resultImageLP10 = ImageProcessing3.postProcessImage(lowpass10, originalWidth, originalHeight);
                                BufferedImage resultImageLP50 = ImageProcessing3.postProcessImage(lowpass50, originalWidth, originalHeight);
                                BufferedImage resultImageLP100 = ImageProcessing3.postProcessImage(lowpass100, originalWidth, originalHeight);

                                List<BufferedImage> lowPassResults = List.of(
                                    originalImage, resultImageLP10, resultImageLP50, resultImageLP100);
                                ImageDisplay3.display2x2Grid(
                                    lowPassResults.get(0),  // Original
                                    lowPassResults.get(1),  // Radius 10
                                    lowPassResults.get(2),  // Radius 50
                                    lowPassResults.get(3),  // Radius 100
                                    "Original Image",
                                    "Notch Low-pass (r=10)",
                                    "Notch Low-pass (r=50)", 
                                    "Notch Low-pass (r=100)",
                                    "Notch Low-pass Filter Results");
                                break;

                            case "2": // Notch High-pass filter
                                ImageProcessing3.Complex[][] preProcHP = ImageProcessing3.preProcessComplexs(originalImage);
                                ImageProcessing3.Complex[][] highpass10 = ImageProcessing3.applyNotchHighPassFilter(preProcHP, 10);
                                ImageProcessing3.Complex[][] highpass50 = ImageProcessing3.applyNotchHighPassFilter(preProcHP, 50);
                                ImageProcessing3.Complex[][] highpass100 = ImageProcessing3.applyNotchHighPassFilter(preProcHP, 100);

                                BufferedImage resultImageHP10 = ImageProcessing3.postProcessImage(highpass10, originalWidth, originalHeight);
                                BufferedImage resultImageHP50 = ImageProcessing3.postProcessImage(highpass50, originalWidth, originalHeight);
                                BufferedImage resultImageHP100 = ImageProcessing3.postProcessImage(highpass100, originalWidth, originalHeight);

                                List<BufferedImage> highPassResults = List.of(
                                    originalImage, resultImageHP10, resultImageHP50, resultImageHP100);
                                ImageDisplay3.display2x2Grid(
                                    highPassResults.get(0),  // Original
                                    highPassResults.get(1),  // Radius 10
                                    highPassResults.get(2),  // Radius 50
                                    highPassResults.get(3),  // Radius 100
                                    "Original Image",
                                    "Notch High-pass (r=10)",
                                    "Notch High-pass (r=50)", 
                                    "Notch High-pass (r=100)",
                                    "Notch High-pass Filter Results");
                                break;

                            case "3": // Gaussian Low-pass filter
                                ImageProcessing3.Complex[][] preProcGLP = ImageProcessing3.preProcessComplexs(originalImage);
                                ImageProcessing3.Complex[][] gaussianLow10 = ImageProcessing3.applyGaussianLowPassFilter(preProcGLP, 10);
                                ImageProcessing3.Complex[][] gaussianLow50 = ImageProcessing3.applyGaussianLowPassFilter(preProcGLP, 50);
                                ImageProcessing3.Complex[][] gaussianLow100 = ImageProcessing3.applyGaussianLowPassFilter(preProcGLP, 100);

                                BufferedImage resultImageGLP10 = ImageProcessing3.postProcessImage(gaussianLow10, originalWidth, originalHeight);
                                BufferedImage resultImageGLP50 = ImageProcessing3.postProcessImage(gaussianLow50, originalWidth, originalHeight);
                                BufferedImage resultImageGLP100 = ImageProcessing3.postProcessImage(gaussianLow100, originalWidth, originalHeight);

                                List<BufferedImage> gaussianLowPassResults = List.of(
                                    originalImage, resultImageGLP10, resultImageGLP50, resultImageGLP100);
                                ImageDisplay3.display2x2Grid(
                                    gaussianLowPassResults.get(0),  // Original
                                    gaussianLowPassResults.get(1),  // D₀ = 10
                                    gaussianLowPassResults.get(2),  // D₀ = 50
                                    gaussianLowPassResults.get(3),  // D₀ = 100
                                    "Original Image",
                                    "Gaussian Low-pass (D₀=10)",
                                    "Gaussian Low-pass (D₀=50)", 
                                    "Gaussian Low-pass (D₀=100)",
                                    "Gaussian Low-pass Filter Results");
                                break;
                                
                            case "4": // Gaussian High-pass filter
                                ImageProcessing3.Complex[][] preProcGHP = ImageProcessing3.preProcessComplexs(originalImage);
                                ImageProcessing3.Complex[][] gaussianHigh10 = ImageProcessing3.applyGaussianHighPassFilter(preProcGHP, 10);
                                ImageProcessing3.Complex[][] gaussianHigh50 = ImageProcessing3.applyGaussianHighPassFilter(preProcGHP, 50);
                                ImageProcessing3.Complex[][] gaussianHigh100 = ImageProcessing3.applyGaussianHighPassFilter(preProcGHP, 100);

                                BufferedImage resultImageGHP10 = ImageProcessing3.postProcessImage(gaussianHigh10, originalWidth, originalHeight);
                                BufferedImage resultImageGHP50 = ImageProcessing3.postProcessImage(gaussianHigh50, originalWidth, originalHeight);
                                BufferedImage resultImageGHP100 = ImageProcessing3.postProcessImage(gaussianHigh100, originalWidth, originalHeight);

                                List<BufferedImage> gaussianHighPassResults = List.of(
                                    originalImage, resultImageGHP10, resultImageGHP50, resultImageGHP100);
                                ImageDisplay3.display2x2Grid(
                                    gaussianHighPassResults.get(0),  // Original
                                    gaussianHighPassResults.get(1),  // D₀ = 10
                                    gaussianHighPassResults.get(2),  // D₀ = 50
                                    gaussianHighPassResults.get(3),  // D₀ = 100
                                    "Original Image",
                                    "Gaussian High-pass (D₀=10)",
                                    "Gaussian High-pass (D₀=50)", 
                                    "Gaussian High-pass (D₀=100)",
                                    "Gaussian High-pass Filter Results");
                                break;


                            case "5": // Remove Periodic Noise using Original Reference
                                BufferedImage oriSpec = ImageProcessing3.createSpectrum(originalImage);
                                BufferedImage processedImage = ImageProcessing3.convertToGrayscale(originalImage);
                                BufferedImage powerOfTwoImage = ImageProcessing3.resizeToPowerOfTwo(processedImage);
                                double[][] imageArray = ImageProcessing3.imageTo2DArray(powerOfTwoImage);
                                double[][] centeredArray = ImageProcessing3.centerForFFT(imageArray);
                                ImageProcessing3.Complex[][] fftArray = ImageProcessing3.fft2d(centeredArray);

                                BufferedImage denoised = ImageProcessing3.denoiseSpectrumWithLaplacian(oriSpec, 0.1);

                                ImageProcessing3.Complex[][] inverse = ImageProcessing3.spectrumToComplex(denoised, fftArray);
                                BufferedImage denoisedImage = ImageProcessing3.postProcessImage(inverse, originalWidth, originalHeight);
                            

                                ImageDisplay3.display2x2Grid(
                                    originalImage, oriSpec, denoised, denoisedImage,
                                    "Noised Image", "Spectrum of Noised Image", 
                                    "Spectrum of Denoised Image", "Denoised Image",
                                    "Periodic Noise Removal - Noisy Images");

                                break;
                            case "6": // Back to main menu
                                continueProcessing = false;
                                break;
                                
                            default:
                                System.out.println("Invalid option! Please choose 1-5.");
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