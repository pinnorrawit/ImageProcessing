import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageDisplay3 {
    
    public static void displayImage(BufferedImage image, String title) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            
            JLabel label = new JLabel(new ImageIcon(image));
            frame.getContentPane().add(label, BorderLayout.CENTER);
            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
    
    public static void displayComparison(BufferedImage original, BufferedImage processed, 
                                       String originalTitle, String processedTitle) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Image Comparison");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new GridLayout(1, 2));
            
            JLabel originalLabel = new JLabel(originalTitle, new ImageIcon(original), JLabel.CENTER);
            originalLabel.setVerticalTextPosition(JLabel.BOTTOM);
            originalLabel.setHorizontalTextPosition(JLabel.CENTER);
            
            JLabel processedLabel = new JLabel(processedTitle, new ImageIcon(processed), JLabel.CENTER);
            processedLabel.setVerticalTextPosition(JLabel.BOTTOM);
            processedLabel.setHorizontalTextPosition(JLabel.CENTER);
            
            frame.add(originalLabel);
            frame.add(processedLabel);
            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
    
    /**
     * Displays 4 images in a 2x2 grid layout
     * @param topLeft Image for top-left position
     * @param topRight Image for top-right position
     * @param bottomLeft Image for bottom-left position
     * @param bottomRight Image for bottom-right position
     * @param topLeftTitle Title for top-left image
     * @param topRightTitle Title for top-right image
     * @param bottomLeftTitle Title for bottom-left image
     * @param bottomRightTitle Title for bottom-right image
     * @param frameTitle Title for the entire frame
     */
    public static void display2x2Grid(BufferedImage topLeft, BufferedImage topRight,
                                    BufferedImage bottomLeft, BufferedImage bottomRight,
                                    String topLeftTitle, String topRightTitle,
                                    String bottomLeftTitle, String bottomRightTitle,
                                    String frameTitle) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(frameTitle);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new GridLayout(2, 2, 5, 5)); // 2 rows, 2 columns, with 5px gaps
            
            // Create labeled images for each position
            JLabel topLeftLabel = createLabeledImage(topLeft, topLeftTitle);
            JLabel topRightLabel = createLabeledImage(topRight, topRightTitle);
            JLabel bottomLeftLabel = createLabeledImage(bottomLeft, bottomLeftTitle);
            JLabel bottomRightLabel = createLabeledImage(bottomRight, bottomRightTitle);
            
            // Add to frame in correct order (row-major)
            frame.add(topLeftLabel);
            frame.add(topRightLabel);
            frame.add(bottomLeftLabel);
            frame.add(bottomRightLabel);
            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
    
    /**
     * Helper method to create a JLabel with image and title
     */
    private static JLabel createLabeledImage(BufferedImage image, String title) {
        JLabel label = new JLabel(title, new ImageIcon(image), JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.BOTTOM);
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return label;
    }
    
    /**
     * Overloaded version with default titles
     */
    public static void display2x2Grid(BufferedImage topLeft, BufferedImage topRight,
                                    BufferedImage bottomLeft, BufferedImage bottomRight,
                                    String frameTitle) {
        display2x2Grid(topLeft, topRight, bottomLeft, bottomRight,
                      "Top Left", "Top Right", "Bottom Left", "Bottom Right", 
                      frameTitle);
    }
    
    /**
     * Simple comparison display with titled borders
     */
    public static void displaySimpleComparison(BufferedImage image1, BufferedImage image2, String title) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new GridLayout(1, 2));
            
            JLabel label1 = new JLabel(new ImageIcon(image1));
            JLabel label2 = new JLabel(new ImageIcon(image2));
            
            JPanel panel1 = new JPanel(new BorderLayout());
            panel1.setBorder(BorderFactory.createTitledBorder("Original"));
            panel1.add(label1, BorderLayout.CENTER);
            
            JPanel panel2 = new JPanel(new BorderLayout());
            panel2.setBorder(BorderFactory.createTitledBorder("DFT Magnitude"));
            panel2.add(label2, BorderLayout.CENTER);
            
            frame.add(panel1);
            frame.add(panel2);
            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
    public static void display3ImagesHorizontal(BufferedImage img1, BufferedImage img2, BufferedImage img3,
                                          String title1, String title2, String title3,
                                          String windowTitle) {
    
        // Get dimensions of each image
        int width1 = img1.getWidth();
        int height1 = img1.getHeight();
        int width2 = img2.getWidth();
        int height2 = img2.getHeight();
        int width3 = img3.getWidth();
        int height3 = img3.getHeight();
        
        // Use the maximum height for consistent display
        int maxHeight = Math.max(height1, Math.max(height2, height3));
        int padding = 20;
        int totalWidth = width1 + width2 + width3 + 4 * padding;
        
        // Create combined image
        BufferedImage combined = new BufferedImage(totalWidth, maxHeight + 80, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = combined.createGraphics();
        
        // Set white background
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, totalWidth, maxHeight + 80);
        
        // Draw each image centered vertically
        int y1 = 40 + (maxHeight - height1) / 2;
        int y2 = 40 + (maxHeight - height2) / 2;
        int y3 = 40 + (maxHeight - height3) / 2;
        
        g.drawImage(img1, padding, y1, width1, height1, null);
        g.drawImage(img2, width1 + 2 * padding, y2, width2, height2, null);
        g.drawImage(img3, width1 + width2 + 3 * padding, y3, width3, height3, null);
        
        // Draw titles
        g.setColor(java.awt.Color.BLACK);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        
        // Center titles under each image
        int titleY = maxHeight + 60;
        g.drawString(title1 + " (" + width1 + "x" + height1 + ")", 
                    padding + width1/2 - (title1.length() * 3), titleY);
        g.drawString(title2 + " (" + width2 + "x" + height2 + ")", 
                    width1 + 2 * padding + width2/2 - (title2.length() * 3), titleY);
        g.drawString(title3 + " (" + width3 + "x" + height3 + ")", 
                    width1 + width2 + 3 * padding + width3/2 - (title3.length() * 3), titleY);
        
        // Draw window title at top
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        int titleWidth = g.getFontMetrics().stringWidth(windowTitle);
        g.drawString(windowTitle, (totalWidth - titleWidth) / 2, 25);
        
        g.dispose();
        
        // Display the combined image
        displayImage(combined, windowTitle);
    }
}