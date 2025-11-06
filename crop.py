import cv2
import matplotlib.pyplot as plt
import numpy as np

def autocrop(gray_image, kernel_size=15, target_ratio=3.0/2.0):
    # Create binary image
    _, binary = cv2.threshold(gray_image, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    
    # Find lung regions (holes in contours)
    contours, hierarchy = cv2.findContours(binary, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    lung_mask = np.zeros_like(binary)
    
    if hierarchy is not None:
        for i, contour in enumerate(contours):
            if hierarchy[0][i][3] != -1:  # Lung region
                cv2.drawContours(lung_mask, [contour], -1, 255, -1)
    
    # Connect lung regions and find largest contour
    kernel = np.ones((kernel_size, kernel_size), np.uint8)
    connected_lungs = cv2.dilate(lung_mask, kernel, iterations=1)
    contours, _ = cv2.findContours(connected_lungs, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    if not contours:
        return None, None
    
    largest_contour = max(contours, key=cv2.contourArea)
    x, y, w, h = cv2.boundingRect(largest_contour)
    img_h, img_w = gray_image.shape
    
    # Calculate padding for target aspect ratio
    padding_x = int(w * 0.25)
    padding_y = int(h * 0.25)
    
    # Adjust padding to achieve 3:2 ratio
    bbox_w = w + 2 * padding_x
    bbox_h = h + 2 * padding_y
    current_ratio = bbox_w / bbox_h
    
    if current_ratio > target_ratio:
        target_h = bbox_w / target_ratio
        padding_y = max(padding_y, int((target_h - h) / 2))
    else:
        target_w = bbox_h * target_ratio
        padding_x = max(padding_x, int((target_w - w) / 2))
    
    # Apply padding with bounds checking
    x1, y1 = max(0, x - padding_x), max(0, y - padding_y)
    x2, y2 = min(img_w, x + w + padding_x), min(img_h, y + h + padding_y)
    
    # Final ratio adjustment
    final_w, final_h = x2 - x1, y2 - y1
    final_ratio = final_w / final_h
    
    if abs(final_ratio - target_ratio) > 0.1:
        if final_ratio < target_ratio:
            needed_w = int(final_h * target_ratio)
            w_diff = needed_w - final_w
            x1, x2 = max(0, x1 - w_diff//2), min(img_w, x2 + w_diff//2)
        else:
            needed_h = int(final_w / target_ratio)
            h_diff = needed_h - final_h
            y1, y2 = max(0, y1 - h_diff//2), min(img_h, y2 + h_diff//2)
    
    cropped = gray_image[y1:y2, x1:x2]
    
    # Create visualization
    vis = cv2.cvtColor(gray_image, cv2.COLOR_GRAY2BGR)
    lungs_vis = cv2.cvtColor(connected_lungs, cv2.COLOR_GRAY2BGR)
    vis = cv2.addWeighted(vis, 1, (lungs_vis * 0.3).astype(np.uint8), 0.7, 0)
    cv2.drawContours(vis, [largest_contour], -1, (0, 255, 0), 2)
    cv2.rectangle(vis, (x1, y1), (x2, y2), (0, 0, 255), 2)
    
    return cropped, vis

# Process images
image_paths = ['BTR022.jpg', 'BTR024.jpg', 'BTR056.jpg', 'BTR065.jpg']

# Display original and detection results
plt.figure(figsize=(15, 10))
for i, path in enumerate(image_paths):
    img = cv2.imread(path, cv2.IMREAD_GRAYSCALE)
    if img is None:
        print(f"Cannot load: {path}")
        continue
        
    cropped, vis = autocrop(img)
    
    if cropped is not None:
        plt.subplot(2, 4, i*2 + 1)
        plt.imshow(img, cmap='gray')
        plt.title(f'{path}\nOriginal')
        plt.axis('off')
        
        plt.subplot(2, 4, i*2 + 2)
        plt.imshow(cv2.cvtColor(vis, cv2.COLOR_BGR2RGB))
        plt.title(f'{path}\nDetection')
        plt.axis('off')
        
        print(f"Processed {path}: {cropped.shape[1]}x{cropped.shape[0]}")

plt.tight_layout()
plt.show()

# Display cropped images
plt.figure(figsize=(15, 10))
for i, path in enumerate(image_paths):
    img = cv2.imread(path, cv2.IMREAD_GRAYSCALE)
    if img is None:
        continue
        
    cropped, _ = autocrop(img)
    
    if cropped is not None:
        plt.subplot(2, 2, i + 1)
        plt.imshow(cropped, cmap='gray')
        plt.title(f'{path}\nCropped')
        plt.axis('off')

plt.tight_layout()
plt.show()

print("Completed!")
