import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageProcessing {
    
    /**
     * Converts an image to grayscale using luminosity method
     * @param imageFile The image file to convert (must exist)
     * @return Grayscale BufferedImage
     */
    public static BufferedImage convertToGrayscale(BufferedImage imageFile) throws IOException {
        BufferedImage original = imageFile;
        if (original == null) {
            throw new IOException("Image file is null or could not be read.");
        }
        BufferedImage grayscale = new BufferedImage(
            original.getWidth(), 
            original.getHeight(), 
            BufferedImage.TYPE_INT_RGB);
        
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

public static BufferedImage zoom(BufferedImage img, int factor, String method) {
        int newWidth = img.getWidth() * factor;
        int newHeight = img.getHeight() * factor;
        BufferedImage zoomed = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        
        if ("replication".equals(method)) {
            for (int y = 0; y < newHeight; y++) {
                for (int x = 0; x < newWidth; x++) {
                    int origX = Math.min(x / factor, img.getWidth() - 1);
                    int origY = Math.min(y / factor, img.getHeight() - 1);
                    zoomed.setRGB(x, y, img.getRGB(origX, origY));
                }
            }
        } else { // bilinear
            bilinearInterpolation(img, zoomed);
        }
        return zoomed;
    }

    public static BufferedImage shrink(BufferedImage img, int factor, String method) {
        int newWidth = img.getWidth() / factor;
        int newHeight = img.getHeight() / factor;
        BufferedImage shrunk = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        
        if ("replication".equals(method)) {
            for (int y = 0; y < newHeight; y++) {
                for (int x = 0; x < newWidth; x++) {
                    int origX = Math.min(x * factor, img.getWidth() - 1);
                    int origY = Math.min(y * factor, img.getHeight() - 1);
                    shrunk.setRGB(x, y, img.getRGB(origX, origY));
                }
            }
        } else { // bilinear
            bilinearInterpolation(img, shrunk);
        }
        return shrunk;
    }

    private static int getMirroredPixel(BufferedImage img, int x, int y) {
        // Mirror coordinates if outside bounds
        x = (x < 0) ? -x : (x >= img.getWidth()) ? 2*img.getWidth() - x - 2 : x;
        y = (y < 0) ? -y : (y >= img.getHeight()) ? 2*img.getHeight() - y - 2 : y;
        return img.getRGB(x, y);
    }

    public static void bilinearInterpolation(BufferedImage src, BufferedImage dest) {
        for (int y = 0; y < dest.getHeight(); y++) {
            double yPos = (double) y / dest.getHeight() * (src.getHeight() - 1);
            int y0 = (int) Math.floor(yPos);
            double yFrac = yPos - y0;
            int y1 = y0 + 1;
            
            for (int x = 0; x < dest.getWidth(); x++) {
                double xPos = (double) x / dest.getWidth() * (src.getWidth() - 1);
                int x0 = (int) Math.floor(xPos);
                double xFrac = xPos - x0;
                int x1 = x0 + 1;
                
                // Get the four surrounding pixels
                int pixelA = getMirroredPixel(src, x0, y0);
                int pixelB = getMirroredPixel(src, x1, y0);
                int pixelC = getMirroredPixel(src, x0, y1);
                int pixelD = getMirroredPixel(src, x1, y1);
                
                // Extract color components for each pixel
                Color colorA = new Color(pixelA);
                Color colorB = new Color(pixelB);
                Color colorC = new Color(pixelC);
                Color colorD = new Color(pixelD);
                
                // Interpolate each color channel separately
                int red = (int) (colorA.getRed() * (1 - xFrac) * (1 - yFrac) + 
                          colorB.getRed() * xFrac * (1 - yFrac) + 
                          colorC.getRed() * (1 - xFrac) * yFrac + 
                          colorD.getRed() * xFrac * yFrac);
                
                int green = (int) (colorA.getGreen() * (1 - xFrac) * (1 - yFrac) + 
                            colorB.getGreen() * xFrac * (1 - yFrac) + 
                            colorC.getGreen() * (1 - xFrac) * yFrac + 
                            colorD.getGreen() * xFrac * yFrac);
                
                int blue = (int) (colorA.getBlue() * (1 - xFrac) * (1 - yFrac) + 
                           colorB.getBlue() * xFrac * (1 - yFrac) + 
                           colorC.getBlue() * (1 - xFrac) * yFrac + 
                           colorD.getBlue() * xFrac * yFrac);
                
                // Clamp values to 0-255 range
                red = Math.max(0, Math.min(255, red));
                green = Math.max(0, Math.min(255, green));
                blue = Math.max(0, Math.min(255, blue));
                
                // Set the interpolated color
                dest.setRGB(x, y, new Color(red, green, blue).getRGB());
            }
        }
    }

    public static boolean isGrayscale(BufferedImage inputImage) {
        return inputImage.getColorModel().getNumColorComponents() == 1;
    }

    public static BufferedImage powerLawTransform(BufferedImage inputImage, double c, double gamma) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isGrayscale(inputImage)) {
                    // Process grayscale image
                    int gray = inputImage.getRaster().getSample(x, y, 0);
                    int transformed = powerTransformPixel(gray, c, gamma);
                    outputImage.getRaster().setSample(x, y, 0, transformed);
                } else {
                    // Process color image (RGB)
                    int rgb = inputImage.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    
                    // Apply transformation to each channel
                    r = powerTransformPixel(r, c, gamma);
                    g = powerTransformPixel(g, c, gamma);
                    b = powerTransformPixel(b, c, gamma);
                    
                    // Set new RGB value
                    int newRgb = (r << 16) | (g << 8) | b;
                    outputImage.setRGB(x, y, newRgb);
                }
            }
        }
        
        return outputImage;
    }

    private static double normalizePixel(int pixel) {
        return pixel / 255.0;
    }
    
    private static int denormalizePixel(double transformedPixel) {
        int newPixel = (int) Math.round(transformedPixel * 255);
        return Math.max(0, Math.min(255, newPixel));  // Clamp to [0, 255]
    }

    private static double applyPowerTransform(double normalizedPixel, double c, double gamma) {
        return c * Math.pow(normalizedPixel, gamma);
    }
    
    private static int powerTransformPixel(int pixel, double c, double gamma) {
        double normalized = normalizePixel(pixel);               // Step 1: Normalize
        double transformed = applyPowerTransform(normalized, c, gamma); // Step 2: Transform
        return denormalizePixel(transformed);                    // Step 3: Denormalize + clamp
    }

    public static BufferedImage[] transformMultiple(BufferedImage inputImage, 
                                                  double[] cValues, double[] gammaValues) {
        int total = cValues.length * gammaValues.length;
        BufferedImage[] results = new BufferedImage[total];
        int index = 0;
        
        for (double c : cValues) {
            for (double gamma : gammaValues) {
                results[index++] = powerLawTransform(inputImage, c, gamma);
            }
        }
        
        return results;
    }
}