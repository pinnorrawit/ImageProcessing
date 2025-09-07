import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class ImageDisplay2 {
    
    private static final int MAX_COLUMNS = 4;
    private static final int MAX_ROWS = 2;
    private static final int BORDER = 10;
    
    public static void displayImage(BufferedImage image, String title) {
        displayImages(List.of(image), title);
    }
    
    public static void displayImages(List<BufferedImage> images, String title) {
        if (images == null || images.isEmpty()) {
            System.out.println("No images to display");
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                // Limit the number of images to display
                int numImages = Math.min(images.size(), MAX_COLUMNS * MAX_ROWS);
                
                // Calculate grid layout
                int columns = Math.min(numImages, MAX_COLUMNS);
                int rows = (int) Math.ceil((double) numImages / columns);
                rows = Math.min(rows, MAX_ROWS);
                
                // Create main panel with grid layout
                JPanel mainPanel = new JPanel(new GridLayout(rows, columns, BORDER, BORDER));
                mainPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
                mainPanel.setBackground(Color.DARK_GRAY);
                
                // Add images at original size
                for (int i = 0; i < numImages; i++) {
                    BufferedImage image = images.get(i);
                    if (image != null) {
                        JLabel imageLabel = new JLabel(new ImageIcon(image));
                        imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
                        
                        // Add tooltip with image dimensions
                        String tooltip = String.format("Original Size: %dx%d pixels",
                            image.getWidth(), image.getHeight());
                        imageLabel.setToolTipText(tooltip);
                        
                        mainPanel.add(imageLabel);
                    }
                }
                
                // Fill remaining slots with empty panels if needed
                for (int i = numImages; i < rows * columns; i++) {
                    mainPanel.add(new JPanel());
                }
                
                // Create and show the frame
                JFrame frame = new JFrame(title);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                
                JScrollPane scrollPane = new JScrollPane(mainPanel);
                scrollPane.setBorder(BorderFactory.createEmptyBorder());
                
                frame.add(scrollPane);
                frame.pack();
                frame.setLocationRelativeTo(null); // Center on screen
                
                // Set maximum size to prevent window from being too large
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                frame.setMaximumSize(new Dimension(screenSize.width, screenSize.height));
                
                frame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Error displaying images: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}