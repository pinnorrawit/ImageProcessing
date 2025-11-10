import cv2
import matplotlib.pyplot as plt
import numpy as np
from scipy import ndimage

# --- Image Pre-processing Functions (Unchanged) ---

def smooth(image):
    """
    Apply CLAHE for contrast enhancement, then ultra heavy smoothing.
    """
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    clahe_image = clahe.apply(image)
    
    image_float = clahe_image.astype(np.float32)
    
    smoothed = cv2.GaussianBlur(image_float, (15, 15), 4.0)
    smoothed = cv2.GaussianBlur(smoothed, (11, 11), 3.0)
    
    smoothed_uint8 = smoothed.astype(np.uint8)
    smoothed = cv2.medianBlur(smoothed_uint8, 15)
    smoothed = cv2.medianBlur(smoothed, 11)
    
    smoothed = cv2.bilateralFilter(smoothed, 31, 200, 200)
    
    smoothed = cv2.GaussianBlur(smoothed.astype(np.float32), (9, 9), 2.5)
    
    return smoothed.astype(np.uint8), clahe_image

def crop(input_image, kernel_size=15, target_ratio=3.0/2.0):
    """Autocrop function"""
    if isinstance(input_image, str):
        gray_image = cv2.imread(input_image, cv2.IMREAD_GRAYSCALE)
        if gray_image is None:
            raise ValueError(f"Cannot load image: {input_image}")
    else:
        gray_image = input_image.copy()
    
    _, binary = cv2.threshold(gray_image, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    contours, hierarchy = cv2.findContours(binary, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    lung_mask = np.zeros_like(binary)
    
    if hierarchy is not None:
        for i, contour in enumerate(contours):
            if hierarchy[0][i][3] != -1:
                cv2.drawContours(lung_mask, [contour], -1, 255, -1)
    
    kernel = np.ones((kernel_size, kernel_size), np.uint8)
    connected_lungs = cv2.dilate(lung_mask, kernel, iterations=1)
    contours, _ = cv2.findContours(connected_lungs, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    if not contours: return gray_image
    
    largest_contour = max(contours, key=cv2.contourArea)
    x, y, w, h = cv2.boundingRect(largest_contour)
    img_h, img_w = gray_image.shape
    
    padding_x, padding_y = int(w * 0.25), int(h * 0.25)
    bbox_w, bbox_h = w + 2 * padding_x, h + 2 * padding_y
    current_ratio = bbox_w / bbox_h
    
    if current_ratio > target_ratio:
        target_h = bbox_w / target_ratio
        padding_y = max(padding_y, int((target_h - h) / 2))
    else:
        target_w = bbox_h * target_ratio
        padding_x = max(padding_x, int((target_w - w) / 2))
    
    x1, y1 = max(0, x - padding_x), max(0, y - padding_y)
    x2, y2 = min(img_w, x + w + padding_x), min(img_h, y + h + padding_y)
    
    cropped = gray_image[y1:y2, x1:x2]
    return cropped

def find_dark(cropped_image, smoothed_image):
    """Find all dark regions (holes) using the smoothed image."""
    _, binary = cv2.threshold(smoothed_image, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    contours, hierarchy = cv2.findContours(binary, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    dark_regions = []
    region_masks = []
    
    if hierarchy is not None:
        for i, contour in enumerate(contours):
            if hierarchy[0][i][3] != -1:  # Dark regions (holes)
                area = cv2.contourArea(contour)
                if area > 300:
                    dark_regions.append(contour)
                    mask = np.zeros_like(cropped_image)
                    cv2.drawContours(mask, [contour], -1, 255, -1)
                    region_masks.append(mask)
    return dark_regions, region_masks, binary

# --- Convex Hull + Trim Function (MODIFIED) ---

def merge(dark_regions, region_masks, cropped_image):
    """
    Merge the LARGEST dark region with ALL regions to its LEFT,
    apply Convex Hull, and then TRIM the Hull's right side based on the Largest Region's boundary.
    """
    if not dark_regions:
        return None
    
    # *** MODIFICATION: Set margin to 0 ***
    RIGHT_TRIM_MARGIN = 0 
    
    # 1. Setup regions & Collect points for Convex Hull
    largest_idx = np.argmax([cv2.contourArea(contour) for contour in dark_regions])
    largest_region = dark_regions[largest_idx]
    largest_mask = region_masks[largest_idx]
    
    lx, ly, lw, lh = cv2.boundingRect(largest_region)
    largest_center_x = lx + lw // 2
    
    left_regions = []
    all_target_points = largest_region.reshape(-1, 2) 

    for i, region in enumerate(dark_regions):
        if i == largest_idx: continue
        x, y, w, h = cv2.boundingRect(region)
        region_center_x = x + w // 2
        
        if region_center_x < largest_center_x:
            left_regions.append(region)
            all_target_points = np.concatenate((all_target_points, region.reshape(-1, 2)), axis=0)
    
    if all_target_points.size == 0:
        print("Error: No target points found for Convex Hull.")
        return None

    # 2. Calculate Convex Hull
    hull = cv2.convexHull(all_target_points)
    initial_merged_mask = np.zeros_like(cropped_image, dtype=np.uint8)
    cv2.drawContours(initial_merged_mask, [hull], 0, 255, -1) 

    # 3. Apply Trimming Logic (No Margin)
    height, width = cropped_image.shape
    largest_right_boundary = np.zeros(height, dtype=np.int32) 
    largest_y_coords, largest_x_coords = np.where(largest_mask == 255)
    
    max_x_by_y = {}
    for y, x in zip(largest_y_coords, largest_x_coords):
        max_x_by_y[y] = max(max_x_by_y.get(y, 0), x)
    
    for y, max_x in max_x_by_y.items():
        # Margin is 0, so the boundary is max_x + 0
        largest_right_boundary[y] = max_x + RIGHT_TRIM_MARGIN 

    trimmed_mask = initial_merged_mask.copy()
    
    for y in range(height):
        boundary_x = largest_right_boundary[y]
        
        if boundary_x > 0:
            merged_x_coords = np.where(initial_merged_mask[y, :] == 255)[0]
            pixels_to_trim_x = merged_x_coords[merged_x_coords > boundary_x]
            trimmed_mask[y, pixels_to_trim_x] = 0
            
    return largest_mask, trimmed_mask, left_regions

# --- Visualization Function (Unchanged) ---

def create_region_visualization(cropped_image, largest_mask, merged_mask, dark_regions, left_regions):
    """
    Create visualization, marking largest (red) and left (green) regions, 
    and the final merged/trimmed mask (Yellow/Cyan).
    """
    original_rgb = cv2.cvtColor(cropped_image, cv2.COLOR_GRAY2RGB)
    
    # 3. Comparison: Largest (Red) + Left (Green)
    comparison_vis = original_rgb.copy()
    comparison_vis[largest_mask > 0] = comparison_vis[largest_mask > 0] * 0.5 + np.array([0, 0, 255]) * 0.5 # Red
    for region in left_regions:
        temp_mask = np.zeros_like(cropped_image)
        cv2.drawContours(temp_mask, [region], -1, 255, -1)
        comparison_vis[temp_mask > 0] = comparison_vis[temp_mask > 0] * 0.5 + np.array([0, 255, 0]) * 0.5 # Green
            
    # 4. Final merged mask visualization (Yellow/Cyan overlay)
    merged_vis = original_rgb.copy()
    merged_vis[merged_mask > 0] = merged_vis[merged_mask > 0] * 0.5 + np.array([0, 255, 255]) * 0.5 
    
    return {
        'comparison_largest_left': comparison_vis,
        'final_merged_mask': merged_vis,
        'clahe_image': cv2.cvtColor(original_rgb, cv2.COLOR_RGB2GRAY)
    }

# --- Main Execution Function (Unchanged) ---

def run(image_path):
    """
    Complete analysis flow: Crop -> CLAHE -> Blur -> Find Dark Regions -> Merge (Largest + Left, Trimmed) -> Visualize
    """
    print(f"\n=== ANALYZING {image_path} ===")
    
    # 1. Autocrop
    cropped_image = crop(image_path)
    print("1. Image autocropped")
    
    # 2. CLAHE and Ultra Heavy Smoothing
    smoothed_image, clahe_image = smooth(cropped_image)
    print("2. CLAHE applied and image smoothed")
    
    # 3. Find all dark regions
    dark_regions, region_masks, binary_mask = find_dark(cropped_image, smoothed_image)
    print(f"3. Found {len(dark_regions)} dark regions")
    
    if len(dark_regions) == 0:
        print("No dark regions found!")
        return None
    
    # 4. Merge largest with left regions (Convex Hull + Trim, No Margin)
    merge_result = merge(dark_regions, region_masks, cropped_image)
    
    if merge_result is None:
        print("Merging failed!")
        return None
        
    largest_mask, merged_mask, left_regions = merge_result
    
    print(f"4. Merged Largest region with {len(left_regions)} left regions, bridged using Convex Hull, and **trimmed the right side (No Margin)**.")
    
    # 5. Create visualizations
    visualizations = create_region_visualization(
        cropped_image, largest_mask, merged_mask, dark_regions, left_regions)
    
    # 6. Display results
    fig, axes = plt.subplots(2, 2, figsize=(10, 10))
    axes = axes.flatten()
    
    # Plotting Sequence
    axes[0].imshow(cropped_image, cmap='gray')
    axes[0].set_title('1. Original Cropped', fontsize=12, weight='bold')
    axes[0].axis('off')
    
    axes[1].imshow(clahe_image, cmap='gray')
    axes[1].set_title('2. CLAHE Applied', fontsize=12, weight='bold')
    axes[1].axis('off')
    
    axes[2].imshow(visualizations['comparison_largest_left'])
    axes[2].set_title(f'3. Target Regions (Largest + {len(left_regions)} Left)', fontsize=12, weight='bold')
    axes[2].axis('off')
    
    axes[3].imshow(visualizations['final_merged_mask'])
    axes[3].set_title('4. Final Merged Mask (Trimmed Hull - No Margin)', fontsize=12, weight='bold')
    axes[3].axis('off')
    
    plt.suptitle(f'Region Merging Visualization (Largest + Left, TRIMMED - No Margin) - {image_path}', fontsize=16, weight='bold')
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.show()
    
    return {
        'cropped_image': cropped_image,
        'merged_mask': merged_mask
    }

if __name__ == "__main__":
    image_paths = ['BTR022.jpg', 'BTR024.jpg', 'BTR056.jpg', 'BTR065.jpg']
    
    if image_paths:
        try:
            # Example run: Processes 'BTR056.jpg' (index 2)
            result = run(image_paths[0]) 
        except ValueError as e:
            print(f"Error during run: {e}. Check if the image file exists.")

    print("\nProcessing completed!")