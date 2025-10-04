import java.io.File;

public class ImageFileFinder {
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".png", ".jpeg", ".JPG", ".PNG", ".JPEG"};
    
    public static String findImageFile(String userInput) {
        // Case 1: User already included an extension
        if (hasImageExtension(userInput)) {
            return new File(userInput).exists() ? userInput : null;
        }
        
        // Case 2: User provided base name (no extension)
        for (String ext : IMAGE_EXTENSIONS) {
            String filename = userInput + ext;
            if (new File(filename).exists()) {
                return filename;
            }
        }
        return null;
    }
    
    private static boolean hasImageExtension(String filename) {
        String lower = filename.toLowerCase();
        for (String ext : IMAGE_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static String getValidFilename(String userInput) {
            String foundFile = ImageFileFinder.findImageFile(userInput);
            if (foundFile != null) {
                return foundFile;
            } else {
                return null;  
            }
        }
    }

