import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import java.util.Collection;

public class ImageProcessing3 {

    public static BufferedImage createSpectrum(BufferedImage inputImage) {
        // Convert to grayscale if needed
        BufferedImage processedImage = convertToGrayscale(inputImage);
        
        // Resize to power of 2
        BufferedImage powerOfTwoImage = resizeToPowerOfTwo(processedImage);
        
        // Convert to 2D array
        double[][] imageArray = imageTo2DArray(powerOfTwoImage);
        
        // Center for FFT
        double[][] centeredArray = centerForFFT(imageArray);
        
        // Apply FFT
        Complex[][] fftArray = fft2d(centeredArray);
        
        // Create magnitude spectrum
        return createMagnitudeImage(fftArray);
    }

    public static Complex[][] preProcessComplexs(BufferedImage inputImage) {
        // Convert to grayscale if needed
        BufferedImage processedImage = convertToGrayscale(inputImage);
        
        // Resize to power of 2
        BufferedImage powerOfTwoImage = resizeToPowerOfTwo(processedImage);
        
        // Convert to 2D array
        double[][] imageArray = imageTo2DArray(powerOfTwoImage);
        
        // Center for FFT
        double[][] centeredArray = centerForFFT(imageArray);
        
        // Apply FFT
        return fft2d(centeredArray);
    }

    public static BufferedImage postProcessImage(Complex[][] inputFFT, int targetWidth, int targetHeigh) {
        // Apply inverse FFT
        double[][] inverseArray = ifft2d(inputFFT);
        
        // Re-center
        double[][] reCentered = centerForFFT(inverseArray);

        BufferedImage postImage = arrayToImage(reCentered);
        
        // Convert back to image - note: this returns the FFT-processed size
        return resizeToDimensions(postImage, targetWidth, targetHeigh);
    }
   public static Complex[][] spectrumToComplex(BufferedImage spectrum, Complex[][] originalFFT) {
        int height = spectrum.getHeight();
        int width = spectrum.getWidth();
        Complex[][] complexArray = new Complex[height][width];
        
        // Verify dimensions match
        if (height != originalFFT.length || width != originalFFT[0].length) {
            throw new IllegalArgumentException("Spectrum dimensions must match original FFT dimensions");
        }
        
        for (int u = 0; u < height; u++) {
            for (int v = 0; v < width; v++) {
                // Get intensity from spectrum image
                int intensity = new Color(spectrum.getRGB(v, u)).getRed();
                
                // Convert intensity back to magnitude (approximate reverse of log scaling)
                double magnitude = intensityToMagnitude(intensity);
                
                // Use original phase information - THIS IS CRUCIAL!
                double phase = originalFFT[u][v].getPhase();
                
                complexArray[u][v] = fromPolar(magnitude, phase);
            }
        }
        
        return complexArray;
    }

private static double intensityToMagnitude(int intensity) {
    // Reverse the log scaling: magnitude = e^(intensity * maxLog / 255) - 1
    // Since we don't have the original maxLog, we'll use a reasonable approximation
    double normalizedIntensity = intensity / 255.0;
    double logMagnitude = normalizedIntensity * 10.0; // Approximate max log magnitude
    return Math.exp(logMagnitude) - 1;
}

    // Static nested Complex class
    public static class Complex {
        private final double real;
        private final double imag;
        
        public Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }
        
        public double getReal() {
            return real;
        }
        
        public double getImag() {
            return imag;
        }
        
        public Complex add(Complex other) {
            return new Complex(real + other.real, imag + other.imag);
        }
        
        public Complex subtract(Complex other) {
            return new Complex(real - other.real, imag - other.imag);
        }
        
        public Complex multiply(double scalar) {
            return new Complex(real * scalar, imag * scalar);
        }
        
        public Complex multiply(Complex other) {
            double newReal = real * other.real - imag * other.imag;
            double newImag = real * other.imag + imag * other.real;
            return new Complex(newReal, newImag);
        }
        
        public Complex conjugate() {
            return new Complex(real, -imag);
        }
        
        public double magnitude() {
            return Math.sqrt(real * real + imag * imag);
        }
        
        public double getPhase() {
            return Math.atan2(imag, real);
        }
        
        @Override
        public String toString() {
            return String.format("%.3f + %.3fi", real, imag);
        }
    }

    public static double getMagnitude(Complex complex) {
        return Math.sqrt(complex.getReal() * complex.getReal() + complex.getImag() * complex.getImag());
    }

    public static double getPhase(Complex complex) {
        return Math.atan2(complex.getImag(), complex.getReal());
    }

    public static Complex fromPolar(double magnitude, double phase) {
        double real = magnitude * Math.cos(phase);
        double imaginary = magnitude * Math.sin(phase);
        return new Complex(real, imaginary);
    }

    // Direct 1D FFT implementation (Cooley-Tukey algorithm)
    public static Complex[] fft1d(Complex[] x) {
        int n = x.length;
        
        // Base case
        if (n == 1) {
            return new Complex[]{x[0]};
        }
        
        // Check if n is a power of 2
        if ((n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Array length must be a power of 2");
        }
        
        // Split into even and odd indices
        Complex[] even = new Complex[n/2];
        Complex[] odd = new Complex[n/2];
        for (int k = 0; k < n/2; k++) {
            even[k] = x[2*k];
            odd[k] = x[2*k + 1];
        }
        
        // Recursive FFT
        Complex[] evenFFT = fft1d(even);
        Complex[] oddFFT = fft1d(odd);
        
        // Combine results
        Complex[] result = new Complex[n];
        for (int k = 0; k < n/2; k++) {
            double angle = -2 * Math.PI * k / n;
            Complex twiddle = new Complex(Math.cos(angle), Math.sin(angle));
            Complex term = oddFFT[k].multiply(twiddle);
            result[k] = evenFFT[k].add(term);
            result[k + n/2] = evenFFT[k].subtract(term);
        }
        
        return result;
    }
    
    // Inverse 1D FFT
    public static Complex[] ifft1d(Complex[] x) {
        int n = x.length;
        Complex[] conjugated = new Complex[n];
        
        // Conjugate the input
        for (int i = 0; i < n; i++) {
            conjugated[i] = x[i].conjugate();
        }
        
        // Compute FFT of conjugated
        Complex[] fftResult = fft1d(conjugated);
        
        // Conjugate again and scale
        Complex[] result = new Complex[n];
        for (int i = 0; i < n; i++) {
            result[i] = fftResult[i].conjugate().multiply(1.0 / n);
        }
        
        return result;
    }
    
    // 2D FFT using direct implementation
    public static Complex[][] fft2d(double[][] input) {
        int M = input.length;
        int N = input[0].length;
        
        // Check if dimensions are powers of 2
        if (((M & (M - 1)) != 0) || ((N & (N - 1)) != 0)) {
            throw new IllegalArgumentException("Image dimensions must be powers of 2");
        }
        
        // Convert input to Complex[][]
        Complex[][] complexInput = new Complex[M][N];
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                complexInput[i][j] = new Complex(input[i][j], 0);
            }
        }
        
        // Apply FFT to each row
        Complex[][] rowFFT = new Complex[M][N];
        for (int i = 0; i < M; i++) {
            rowFFT[i] = fft1d(complexInput[i]);
        }
        
        // Apply FFT to each column
        Complex[][] result = new Complex[M][N];
        for (int j = 0; j < N; j++) {
            Complex[] column = new Complex[M];
            for (int i = 0; i < M; i++) {
                column[i] = rowFFT[i][j];
            }
            Complex[] columnFFT = fft1d(column);
            for (int i = 0; i < M; i++) {
                result[i][j] = columnFFT[i];
            }
        }
        
        return result;
    }
    
    // 2D Inverse FFT using direct implementation
    public static double[][] ifft2d(Complex[][] input) {
        int M = input.length;
        int N = input[0].length;
        
        // Apply inverse FFT to each column first
        Complex[][] columnIFFT = new Complex[M][N];
        for (int j = 0; j < N; j++) {
            Complex[] column = new Complex[M];
            for (int i = 0; i < M; i++) {
                column[i] = input[i][j];
            }
            Complex[] columnResult = ifft1d(column);
            for (int i = 0; i < M; i++) {
                columnIFFT[i][j] = columnResult[i];
            }
        }
        
        // Apply inverse FFT to each row
        Complex[][] complexResult = new Complex[M][N];
        for (int i = 0; i < M; i++) {
            complexResult[i] = ifft1d(columnIFFT[i]);
        }
        
        // Extract real parts (imaginary parts should be near zero)
        double[][] result = new double[M][N];
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                result[i][j] = complexResult[i][j].getReal();
            }
        }
        
        return result;
    }

    // Improved resize method with better interpolation
    public static BufferedImage resizeToDimensions(BufferedImage image, int targetWidth, int targetHeight) {
        if (image.getWidth() == targetWidth && image.getHeight() == targetHeight) {
            return image;
        }
        
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, image.getType());
        java.awt.Graphics2D g = resized.createGraphics();
        
        // Use high-quality rendering hints for better resizing
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                        java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        
        System.out.println("Resized image from " + image.getWidth() + "x" + image.getHeight() + 
                        " to " + targetWidth + "x" + targetHeight);
        
        return resized;
    }

    // Utility method to ensure image dimensions are powers of 2
    public static BufferedImage resizeToPowerOfTwo(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        int newWidth = nextPowerOfTwo(width);
        int newHeight = nextPowerOfTwo(height);
        
        if (width == newWidth && height == newHeight) {
            return image;
        }
        
        BufferedImage resized = new BufferedImage(newWidth, newHeight, image.getType());
        java.awt.Graphics2D g = resized.createGraphics();
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        return resized;
    }
    
    private static int nextPowerOfTwo(int n) {
        int power = 1;
        while (power < n) {
            power *= 2;
        }
        return power;
    }

    public static BufferedImage convertToGrayscale(BufferedImage imageFile) {
        BufferedImage original = imageFile;
        BufferedImage grayscale = new BufferedImage(
            original.getWidth(), 
            original.getHeight(), 
            BufferedImage.TYPE_BYTE_GRAY);
        
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

    public static double[][] imageTo2DArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] resultArray = new double[height][width];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int intensity = (pixel >> 16) & 0xFF;
                resultArray[y][x] = intensity;
            }
        }
        
        return resultArray;
    }

    public static double[][] centerForFFT(double[][] input) {
        int M = input.length;
        int N = input[0].length;
        double[][] centered = new double[M][N];
        
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                // Center by multiplying by (-1)^(i+j)
                centered[i][j] = input[i][j] * (((i + j) % 2 == 0) ? 1 : -1);
            }
        }
        
        return centered;
    }

    public static BufferedImage createMagnitudeImage(Complex[][] fftResult) {
        int M = fftResult.length;
        int N = fftResult[0].length;
        BufferedImage image = new BufferedImage(N, M, BufferedImage.TYPE_BYTE_GRAY);
        
        // Find maximum magnitude for normalization
        double maxMagnitude = 0.0;
        for (int u = 0; u < M; u++) {
            for (int v = 0; v < N; v++) {
                double mag = fftResult[u][v].magnitude();
                if (mag > maxMagnitude) maxMagnitude = mag;
            }
        }
        
        // Create grayscale image with log scale for better visualization
        for (int u = 0; u < M; u++) {
            for (int v = 0; v < N; v++) {
                double magnitude = fftResult[u][v].magnitude();
                double logMagnitude = Math.log(1 + magnitude);
                double maxLog = Math.log(1 + maxMagnitude);
                int grayValue = (int) (255 * logMagnitude / maxLog);
                grayValue = Math.min(255, Math.max(0, grayValue));
                
                int rgb = (grayValue << 16) | (grayValue << 8) | grayValue;
                image.setRGB(v, u, rgb);
            }
        }
        
        return image;
    }

    // Convert 2D double array to BufferedImage
    public static BufferedImage arrayToImage(double[][] array) {
        int height = array.length;
        int width = array[0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        // Find min and max values for normalization
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (array[i][j] < min) min = array[i][j];
                if (array[i][j] > max) max = array[i][j];
            }
        }
        
        // Handle case where all values are the same
        if (max == min) {
            max = min + 1; // Avoid division by zero
        }
        
        // Normalize and convert to image
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Normalize to 0-255 range
                int value = (int) (255 * (array[i][j] - min) / (max - min));
                value = Math.min(255, Math.max(0, value)); // Clamp to valid range
                
                // Create grayscale RGB value
                int rgb = (value << 16) | (value << 8) | value;
                image.setRGB(j, i, rgb);
            }
        }
        return image;
    }

    // Apply notch low-pass filter to FFT result
    public static Complex[][] applyNotchLowPassFilter(Complex[][] fftInput, int radius) {
        int M = fftInput.length;
        int N = fftInput[0].length;
        Complex[][] filtered = new Complex[M][N];
        
        // Calculate center coordinates
        int centerU = M / 2;
        int centerV = N / 2;
        
        for (int u = 0; u < M; u++) {
            for (int v = 0; v < N; v++) {
                // Calculate distance from center (considering FFT is centered)
                double distance = Math.sqrt(Math.pow(u - centerU, 2) + Math.pow(v - centerV, 2));
                
                // Apply the notch filter
                if (distance <= radius) {
                    // Keep frequencies inside the circle (low frequencies)
                    filtered[u][v] = fftInput[u][v];
                } else {
                    // Remove frequencies outside the circle (high frequencies)
                    filtered[u][v] = new Complex(0, 0);
                }
            }
        }
        
        return filtered;
    }

    // Apply notch high-pass filter to FFT result
    public static Complex[][] applyNotchHighPassFilter(Complex[][] fftInput, int radius) {
        int M = fftInput.length;
        int N = fftInput[0].length;
        Complex[][] filtered = new Complex[M][N];
        
        // Calculate center coordinates
        int centerU = M / 2;
        int centerV = N / 2;
        
        for (int u = 0; u < M; u++) {
            for (int v = 0; v < N; v++) {
                // Calculate distance from center (considering FFT is centered)
                double distance = Math.sqrt(Math.pow(u - centerU, 2) + Math.pow(v - centerV, 2));
                
                // Apply the notch filter
                if (distance > radius) {
                    // Keep frequencies outside the circle (high frequencies)
                    filtered[u][v] = fftInput[u][v];
                } else {
                    // Remove frequencies inside the circle (low frequencies)
                    filtered[u][v] = new Complex(0, 0);
                }
            }
        }
        return filtered;
    }

    // Gaussian Low-pass filter
    public static Complex[][] applyGaussianLowPassFilter(Complex[][] fftInput, double cutoff) {
        int M = fftInput.length;
        int N = fftInput[0].length;
        Complex[][] filtered = new Complex[M][N];
        
        // Calculate center coordinates
        int centerU = M / 2;
        int centerV = N / 2;
        
        for (int u = 0; u < M; u++) {
            for (int v = 0; v < N; v++) {
                // Calculate distance from center
                double distance = Math.sqrt(Math.pow(u - centerU, 2) + Math.pow(v - centerV, 2));
                
                // Apply Gaussian low-pass filter: H(u,v) = e^(-D²(u,v)/2D₀²)
                double exponent = - (distance * distance) / (2 * cutoff * cutoff);
                double filterValue = Math.exp(exponent);
                
                // Apply the filter
                filtered[u][v] = fftInput[u][v].multiply(filterValue);
            }
        }
        
        return filtered;
    }

    // Gaussian High-pass filter
    public static Complex[][] applyGaussianHighPassFilter(Complex[][] fftInput, double cutoff) {
        int M = fftInput.length;
        int N = fftInput[0].length;
        Complex[][] filtered = new Complex[M][N];
        
        // Calculate center coordinates
        int centerU = M / 2;
        int centerV = N / 2;
        
        for (int u = 0; u < M; u++) {
            for (int v = 0; v < N; v++) {
                // Calculate distance from center
                double distance = Math.sqrt(Math.pow(u - centerU, 2) + Math.pow(v - centerV, 2));
                
                // Apply Gaussian high-pass filter: H(u,v) = 1 - e^(-D²(u,v)/2D₀²)
                double exponent = - (distance * distance) / (2 * cutoff * cutoff);
                double filterValue = 1 - Math.exp(exponent);
                
                // Apply the filter
                filtered[u][v] = fftInput[u][v].multiply(filterValue);
            }
        }
        
        return filtered;
    }

    public static BufferedImage denoiseSpectrumWithLaplacian(BufferedImage spectrum, double subtractionStrength) {
        // Ensure spectrum is grayscale
        BufferedImage graySpectrum = isGrayscale(spectrum) ? spectrum : convertToGrayscale(spectrum);
        
        // Step 1: Apply Laplacian to detect edges (noise patterns in spectrum)
        BufferedImage laplacianEdges = applyLaplacianToSpectrum(graySpectrum);
        
        // Step 2: Subtract the edges from original spectrum to reduce noise
        BufferedImage denoised = subtractEdgesFromSpectrum(graySpectrum, laplacianEdges, subtractionStrength);
        
        return denoised;
    }

    private static BufferedImage applyLaplacianToSpectrum(BufferedImage spectrum) {
        int width = spectrum.getWidth();
        int height = spectrum.getHeight();
        BufferedImage laplacian = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        // Use 8-connected Laplacian kernel for better edge detection
        double[][] laplacianKernel = {
            {-1, -1, -1},
            {-1,  8, -1},
            {-1, -1, -1}
        };
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double sum = 0;
                
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixelX = x + kx;
                        int pixelY = y + ky;
                        int intensity = new Color(spectrum.getRGB(pixelX, pixelY)).getRed();
                        sum += intensity * laplacianKernel[ky + 1][kx + 1];
                    }
                }
                
                // Take absolute value and normalize
                int edgeValue = (int) Math.abs(sum);
                edgeValue = Math.min(255, edgeValue);
                
                Color gray = new Color(edgeValue, edgeValue, edgeValue);
                laplacian.setRGB(x, y, gray.getRGB());
            }
        }
        
        return laplacian;
    }

    private static BufferedImage subtractEdgesFromSpectrum(BufferedImage spectrum, BufferedImage edges, double strength) {
        int width = spectrum.getWidth();
        int height = spectrum.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int spectrumIntensity = new Color(spectrum.getRGB(x, y)).getRed();
                int edgeIntensity = new Color(edges.getRGB(x, y)).getRed();
                
                // Subtract edges from spectrum: spectrum - strength × edges
                // This removes high-frequency noise patterns detected by Laplacian
                int denoisedValue = (int) (spectrumIntensity - strength * edgeIntensity);
                denoisedValue = Math.min(255, Math.max(0, denoisedValue));
                
                Color gray = new Color(denoisedValue, denoisedValue, denoisedValue);
                result.setRGB(x, y, gray.getRGB());
            }
        }
        
        return result;
    }
}