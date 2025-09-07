
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ImageProcessing2 {
    
    public static BufferedImage convertToGrayscale(BufferedImage imageFile) throws IOException {
        BufferedImage original = imageFile;
        if (original == null) {
            throw new IOException("Image file is null or could not be read.");
        }
        BufferedImage grayscale = new BufferedImage(
            original.getWidth(), 
            original.getHeight(), 
            BufferedImage.TYPE_BYTE_GRAY);
        
        // Convert image to grayscale with luminosity formula
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                Color color = new Color(original.getRGB(x, y));
                int gray = (int)(0.299 * color.getRed() + 
                                0.587 * color.getGreen() + 
                                0.114 * color.getBlue());
                grayscale.setRGB(x, y, new Color(gray, gray, gray).getRGB());
            }
        }
        return grayscale;
    }

    public static boolean isGrayscale(BufferedImage image) {
        return image.getType() == BufferedImage.TYPE_BYTE_GRAY;
    }


    public static BufferedImage globalHistogramEqualization(BufferedImage inputImage) {
        if (!isGrayscale(inputImage)) {
            throw new IllegalArgumentException("Input image must be grayscale");
        }
        
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        // Step 1: Compute histogram of pixel intensities
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = inputImage.getRaster().getSample(x, y, 0);
                histogram[gray]++;
            }
        }
        
        // Step 2: Compute cumulative distribution function (CDF)
        int totalPixels = width * height;
        int[] cdf = new int[256];
        cdf[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }
        
        // Step 3: Find minimum non-zero CDF value
        int cdfMin = 0;
        for (int i = 0; i < 256; i++) {
            if (cdf[i] > 0) {
                cdfMin = cdf[i];
                break;
            }
        }
        
        // Step 4: Apply histogram equalization transformation
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = inputImage.getRaster().getSample(x, y, 0);
                int newGray = (int) (((cdf[gray] - cdfMin) * 255.0) / (totalPixels - cdfMin));
                newGray = Math.max(0, Math.min(255, newGray)); // Clamp to valid range
                outputImage.getRaster().setSample(x, y, 0, newGray);
            }
        }
        
        return outputImage;
    }

    public static BufferedImage localHistogramEqualization(BufferedImage grayscaleImage, int neighborhoodSize, double k0, double k1, double k2) {
        if (!isGrayscale(grayscaleImage)) {
            throw new IllegalArgumentException("Image must be grayscale for local histogram equalization");
        }
        
        int width = grayscaleImage.getWidth();
        int height = grayscaleImage.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        int halfSize = neighborhoodSize / 2;
        
        // Pre-calculate global statistics
        double[] globalStats = calculateGlobalStatistics(grayscaleImage);
        double globalMean = globalStats[0];
        double globalStdDev = globalStats[1];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = grayscaleImage.getRaster().getSample(x, y, 0);
                
                // Calculate local statistics for the neighborhood
                double[] localStats = calculateLocalStatistics(grayscaleImage, x, y, halfSize);
                double localMean = localStats[0];
                double localStdDev = localStats[1];
                
                // Apply contrast enhancement based on the conditions
                int enhancedGray;
                if (localMean <= k0 * globalMean && 
                    localStdDev >= k1 * globalStdDev && 
                    localStdDev <= k2 * globalStdDev) {
                    // Apply histogram equalization to this pixel
                    enhancedGray = applyLocalEqualization(grayscaleImage, x, y, halfSize);
                } else {
                    // Keep original pixel value
                    enhancedGray = gray;
                }
                
                result.getRaster().setSample(x, y, 0, enhancedGray);
            }
        }
        
        return result;
    }
    
    private static double[] calculateGlobalStatistics(BufferedImage grayscaleImage) {
        int width = grayscaleImage.getWidth();
        int height = grayscaleImage.getHeight();
        double sum = 0;
        double sumSquared = 0;
        int totalPixels = width * height;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = grayscaleImage.getRaster().getSample(x, y, 0);
                sum += gray;
                sumSquared += gray * gray;
            }
        }
        
        double mean = sum / totalPixels;
        double variance = (sumSquared / totalPixels) - (mean * mean);
        double stdDev = Math.sqrt(Math.max(0, variance));
        
        return new double[]{mean, stdDev};
    }
    
    private static double[] calculateLocalStatistics(BufferedImage grayscaleImage, int centerX, int centerY, int halfSize) {
        int width = grayscaleImage.getWidth();
        int height = grayscaleImage.getHeight();
        double sum = 0;
        double sumSquared = 0;
        int count = 0;
        
        for (int dy = -halfSize; dy <= halfSize; dy++) {
            for (int dx = -halfSize; dx <= halfSize; dx++) {
                int nx = centerX + dx;
                int ny = centerY + dy;
                
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    int gray = grayscaleImage.getRaster().getSample(nx, ny, 0);
                    sum += gray;
                    sumSquared += gray * gray;
                    count++;
                }
            }
        }
        
        if (count == 0) {
            return new double[]{0, 0};
        }
        
        double mean = sum / count;
        double variance = (sumSquared / count) - (mean * mean);
        double stdDev = Math.sqrt(Math.max(0, variance));
        
        return new double[]{mean, stdDev};
    }
    
    private static int applyLocalEqualization(BufferedImage grayscaleImage, int centerX, int centerY, int halfSize) {
        int width = grayscaleImage.getWidth();
        int height = grayscaleImage.getHeight();
        
        // Calculate local histogram
        int[] histogram = new int[256];
        int count = 0;
        
        for (int dy = -halfSize; dy <= halfSize; dy++) {
            for (int dx = -halfSize; dx <= halfSize; dx++) {
                int nx = centerX + dx;
                int ny = centerY + dy;
                
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    int gray = grayscaleImage.getRaster().getSample(nx, ny, 0);
                    histogram[gray]++;
                    count++;
                }
            }
        }
        
        if (count == 0) {
            return grayscaleImage.getRaster().getSample(centerX, centerY, 0);
        }
        
        // Calculate cumulative distribution function (CDF)
        int[] cdf = new int[256];
        cdf[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }
        
        // Find minimum non-zero CDF value
        int cdfMin = 0;
        for (int i = 0; i < 256; i++) {
            if (cdf[i] > 0) {
                cdfMin = cdf[i];
                break;
            }
        }
        
        // Apply histogram equalization
        int centerGray = grayscaleImage.getRaster().getSample(centerX, centerY, 0);
        int enhancedGray = (int) (((cdf[centerGray] - cdfMin) * 255.0) / (count - cdfMin));
        return Math.max(0, Math.min(255, enhancedGray));
    }
    
    // Helper method to get all local histogram equalization results
    public static List<BufferedImage> getAllLocalHistogramEqualizations(BufferedImage grayscaleImage, double k0, double k1, double k2) {
        if (!isGrayscale(grayscaleImage)) {
            throw new IllegalArgumentException("Image must be grayscale");
        }
        
        List<BufferedImage> results = new ArrayList<>();
        
        results.add(localHistogramEqualization(grayscaleImage, 3, k0, k1, k2));
        results.add(localHistogramEqualization(grayscaleImage, 7, k0, k1, k2));
        results.add(localHistogramEqualization(grayscaleImage, 11, k0, k1, k2));
        
        return results;
    }

    public static Map<String, Object> getImageStatistics(BufferedImage image) {
        Map<String, Object> stats = new HashMap<>();
        
        if (!isGrayscale(image)) {
            throw new IllegalArgumentException("Image must be grayscale for statistics");
        }
        
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;
        
        // Basic image info
        stats.put("width", width);
        stats.put("height", height);
        stats.put("totalPixels", totalPixels);
        stats.put("aspectRatio", (double) width / height);
        
        // Calculate histogram and basic statistics
        int[] histogram = new int[256];
        double sum = 0;
        double sumSquared = 0;
        int min = 255;
        int max = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = image.getRaster().getSample(x, y, 0);
                histogram[gray]++;
                sum += gray;
                sumSquared += gray * gray;
                min = Math.min(min, gray);
                max = Math.max(max, gray);
            }
        }
        
        // Basic statistics
        double mean = sum / totalPixels;
        double variance = (sumSquared / totalPixels) - (mean * mean);
        double stdDev = Math.sqrt(Math.max(0, variance));
        
        stats.put("histogram", histogram);
        stats.put("minIntensity", min);
        stats.put("maxIntensity", max);
        stats.put("meanIntensity", mean);
        stats.put("variance", variance);
        stats.put("stdDev", stdDev);
        stats.put("dynamicRange", max - min);
        
        // Calculate additional statistics
        stats.put("medianIntensity", calculateMedian(histogram, totalPixels));
        stats.put("modeIntensity", calculateMode(histogram));
        stats.put("entropy", calculateEntropy(histogram, totalPixels));
        stats.put("contrast", calculateContrast(histogram));
        
        // Image quality metrics
        stats.put("snr", calculateSNR(mean, stdDev)); // Signal-to-Noise Ratio
        stats.put("psnr", calculatePSNR(max)); // Peak Signal-to-Noise Ratio (theoretical max)
        
        return stats;
    }
    
    private static int calculateMedian(int[] histogram, int totalPixels) {
        int count = 0;
        int median = 0;
        for (int i = 0; i < 256; i++) {
            count += histogram[i];
            if (count >= totalPixels / 2) {
                median = i;
                break;
            }
        }
        return median;
    }
    
    private static int calculateMode(int[] histogram) {
        int mode = 0;
        int maxCount = 0;
        for (int i = 0; i < 256; i++) {
            if (histogram[i] > maxCount) {
                maxCount = histogram[i];
                mode = i;
            }
        }
        return mode;
    }
    
    private static double calculateEntropy(int[] histogram, int totalPixels) {
        double entropy = 0;
        for (int i = 0; i < 256; i++) {
            if (histogram[i] > 0) {
                double probability = (double) histogram[i] / totalPixels;
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        return entropy;
    }
    
    private static double calculateContrast(int[] histogram) {
        // Simple contrast measure based on histogram spread
        double contrast = 0;
        for (int i = 0; i < 256; i++) {
            contrast += histogram[i] * i * i;
        }
        return Math.sqrt(contrast / getSum(histogram)) / 255.0;
    }
    
    private static double calculateSNR(double mean, double stdDev) {
        return (stdDev > 0) ? mean / stdDev : Double.MAX_VALUE;
    }
    
    private static double calculatePSNR(int maxValue) {
        return 20 * Math.log10(maxValue); // Theoretical maximum PSNR
    }
    
    private static int getSum(int[] array) {
        int sum = 0;
        for (int value : array) {
            sum += value;
        }
        return sum;
    }
    
    // Method to print statistics in a readable format
    public static void printStatistics(Map<String, Object> stats) {
        System.out.println("\n=== IMAGE STATISTICS ===");
        System.out.println("Dimensions: " + stats.get("width") + " x " + stats.get("height"));
        System.out.println("Total Pixels: " + stats.get("totalPixels"));
        System.out.println("Aspect Ratio: " + String.format("%.2f", stats.get("aspectRatio")));
        System.out.println("\n--- Intensity Statistics ---");
        System.out.println("Min Intensity: " + stats.get("minIntensity"));
        System.out.println("Max Intensity: " + stats.get("maxIntensity"));
        System.out.println("Mean Intensity: " + String.format("%.2f", stats.get("meanIntensity")));
        System.out.println("Median Intensity: " + stats.get("medianIntensity"));
        System.out.println("Mode Intensity: " + stats.get("modeIntensity"));
        System.out.println("Standard Deviation: " + String.format("%.2f", stats.get("stdDev")));
        System.out.println("Variance: " + String.format("%.2f", stats.get("variance")));
        System.out.println("Dynamic Range: " + stats.get("dynamicRange"));
        System.out.println("\n--- Image Quality Metrics ---");
        System.out.println("Entropy: " + String.format("%.4f", stats.get("entropy")));
        System.out.println("Contrast: " + String.format("%.4f", stats.get("contrast")));
        System.out.println("SNR: " + String.format("%.2f", stats.get("snr")));
        System.out.println("Theoretical Max PSNR: " + String.format("%.2f dB", stats.get("psnr")));
    }

    public static BufferedImage localGammaCorrection(BufferedImage grayscaleImage, int neighborhoodSize, double gamma) {
        if (!isGrayscale(grayscaleImage)) {
            throw new IllegalArgumentException("Image must be grayscale for local gamma correction");
        }
        
        if (neighborhoodSize % 2 == 0) {
            throw new IllegalArgumentException("Neighborhood size must be an odd number");
        }
        
        int width = grayscaleImage.getWidth();
        int height = grayscaleImage.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        int halfSize = neighborhoodSize / 2;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Calculate local mean intensity in the neighborhood
                double localMean = calculateLocalMean(grayscaleImage, x, y, halfSize);
                
                // Apply gamma correction based on local mean
                int originalGray = grayscaleImage.getRaster().getSample(x, y, 0);
                int correctedGray = applyGammaCorrection(originalGray, localMean, gamma);
                
                result.getRaster().setSample(x, y, 0, correctedGray);
            }
        }
        
        return result;
    }

    /**
     * Calculates the local mean intensity around a pixel
     */
    private static double calculateLocalMean(BufferedImage image, int centerX, int centerY, int halfSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        double sum = 0;
        int count = 0;
        
        for (int dy = -halfSize; dy <= halfSize; dy++) {
            for (int dx = -halfSize; dx <= halfSize; dx++) {
                int nx = centerX + dx;
                int ny = centerY + dy;
                
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    int gray = image.getRaster().getSample(nx, ny, 0);
                    sum += gray;
                    count++;
                }
            }
        }
        
        return count > 0 ? sum / count : 0;
    }

    /**
     * Applies gamma correction based on local mean intensity
     */
    private static int applyGammaCorrection(int originalGray, double localMean, double gamma) {
        // Normalize to [0, 1] range
        double normalizedGray = originalGray / 255.0;
        
        // Apply gamma correction
        double correctedValue = Math.pow(normalizedGray, 1.0 / gamma);
        
        // Convert back to [0, 255] range and clamp
        return (int) Math.max(0, Math.min(255, correctedValue * 255));
    }

    /**
     * Applies local gamma correction with multiple neighborhood sizes and returns all results
     */
    public static List<BufferedImage> getAllLocalGammaCorrections(BufferedImage grayscaleImage, double gamma) {
        if (!isGrayscale(grayscaleImage)) {
            throw new IllegalArgumentException("Image must be grayscale");
        }
        
        List<BufferedImage> results = new ArrayList<>();
        
        // Apply gamma correction with different neighborhood sizes
        results.add(localGammaCorrection(grayscaleImage, 5, gamma));
        results.add(localGammaCorrection(grayscaleImage, 9, gamma));
        results.add(localGammaCorrection(grayscaleImage, 15, gamma));
        
        return results;
    }

    /**
     * Finds the optimal gamma value by testing multiple values and comparing results
     */
    public static Map<String, Object> findOptimalGamma(BufferedImage grayscaleImage, int neighborhoodSize, double[] gammaValues) {
        Map<String, Object> results = new HashMap<>();
        double bestGamma = 1.0;
        double bestScore = -1;
        BufferedImage bestImage = null;
        Map<String, Object> bestStats = null;
        
        for (double gamma : gammaValues) {
            BufferedImage correctedImage = localGammaCorrection(grayscaleImage, neighborhoodSize, gamma);
            Map<String, Object> stats = getImageStatistics(correctedImage);
            
            // Calculate a quality score
            double score = calculateGammaQualityScore(stats);
            
            if (score > bestScore) {
                bestScore = score;
                bestGamma = gamma;
                bestImage = correctedImage;
                bestStats = stats;
            }
            
            System.out.println("Gamma: " + gamma + " | Score: " + String.format("%.4f", score));
        }
        
        results.put("optimalGamma", bestGamma);
        results.put("optimalScore", bestScore);
        results.put("optimalImage", bestImage);
        results.put("optimalStats", bestStats);
        
        return results;
    }

    /**
     * Calculates a quality score for gamma-corrected images
     */
    private static double calculateGammaQualityScore(Map<String, Object> stats) {
        double entropy = (Double) stats.get("entropy");
        double contrast = (Double) stats.get("contrast");
        double stdDev = (Double) stats.get("stdDev");
        
        // Weighted combination of metrics - higher values generally indicate better quality
        return (entropy * 0.4) + (contrast * 0.3) + (stdDev * 0.3);
    }
    public static Map<String, Object> findOptimalLocalHistogramParams(BufferedImage grayscaleImage, int neighborhoodSize, 
                                                                 double[] k0Values, double[] k1Values, double[] k2Values) {
    Map<String, Object> results = new HashMap<>();
    double bestK0 = 0.4;
    double bestK1 = 0.02;
    double bestK2 = 0.4;
    double bestScore = -1;
    BufferedImage bestImage = null;
    Map<String, Object> bestStats = null;
    
    System.out.println("Testing parameters for " + neighborhoodSize + "x" + neighborhoodSize + " neighborhood:");
    
    // Test all combinations of parameters
    for (double k0 : k0Values) {
        for (double k1 : k1Values) {
            for (double k2 : k2Values) {
                if (k2 > k1) { // k2 should be greater than k1
                    try {
                        BufferedImage enhancedImage = localHistogramEqualization(grayscaleImage, neighborhoodSize, k0, k1, k2);
                        Map<String, Object> stats = getImageStatistics(enhancedImage);
                        
                        // Calculate quality score
                        double score = calculateHistogramQualityScore(stats);
                        
                        if (score > bestScore) {
                            bestScore = score;
                            bestK0 = k0;
                            bestK1 = k1;
                            bestK2 = k2;
                            bestImage = enhancedImage;
                            bestStats = stats;
                        }
                        
                        System.out.println(String.format("k0=%.2f, k1=%.3f, k2=%.2f -> Score: %.4f", 
                                k0, k1, k2, score));
                        
                    } catch (Exception e) {
                        System.out.println("Error with k0=" + k0 + ", k1=" + k1 + ", k2=" + k2 + ": " + e.getMessage());
                    }
                }
            }
        }
    }
    
    results.put("optimalK0", bestK0);
    results.put("optimalK1", bestK1);
    results.put("optimalK2", bestK2);
    results.put("optimalScore", bestScore);
    results.put("optimalImage", bestImage);
    results.put("optimalStats", bestStats);
    results.put("neighborhoodSize", neighborhoodSize);
    
    return results;
}

    /**
     * Calculates quality score for histogram enhanced images
     */
    private static double calculateHistogramQualityScore(Map<String, Object> stats) {
        double entropy = (Double) stats.get("entropy");
        double contrast = (Double) stats.get("contrast");
        double stdDev = (Double) stats.get("stdDev");
        
        // Higher entropy and contrast are better for histogram equalization
        // Moderate standard deviation is preferred (too high might indicate over-enhancement)
        double stdDevScore = 1.0 - Math.abs(stdDev - 80) / 80.0; // Target ~80 std dev
        
        return (entropy * 0.4) + (contrast * 0.3) + (stdDevScore * 0.3);
    }

    /**
     * Gets all local histogram equalizations with optimal parameters for each neighborhood size
     */
    public static List<Map<String, Object>> getAllOptimalLocalHistograms(BufferedImage grayscaleImage) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Parameter ranges to test
        double[] k0Values = {0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        double[] k1Values = {0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1, 0.2, 0.3};
        double[] k2Values = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        
        int[] neighborhoodSizes = {3, 7, 11};
        
        for (int size : neighborhoodSizes) {
            Map<String, Object> result = findOptimalLocalHistogramParams(grayscaleImage, size, k0Values, k1Values, k2Values);
            results.add(result);
        }
        
        return results;
    }
}


