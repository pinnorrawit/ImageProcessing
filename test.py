import cv2
import matplotlib.pyplot as plt
import numpy as np
from scipy import ndimage

def smooth(image):
    """
    Apply CLAHE for contrast enhancement, then ultra heavy smoothing.
    """
    # 1. Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    clahe_image = clahe.apply(image)
    
    image_float = clahe_image.astype(np.float32)
    
    # Step 2: Multiple large Gaussian blurs
    smoothed = cv2.GaussianBlur(image_float, (15, 15), 4.0)
    smoothed = cv2.GaussianBlur(smoothed, (11, 11), 3.0)
    
    # Step 3: Large median filters
    smoothed_uint8 = smoothed.astype(np.uint8)
    smoothed = cv2.medianBlur(smoothed_uint8, 15)
    smoothed = cv2.medianBlur(smoothed, 11)
    
    # Step 4: Strong bilateral filter
    smoothed = cv2.bilateralFilter(smoothed, 31, 200, 200)
    
    # Step 5: Additional Gaussian
    smoothed = cv2.GaussianBlur(smoothed.astype(np.float32), (9, 9), 2.5)
    
    return smoothed.astype(np.uint8), clahe_image

def crop(input_image, kernel_size=15, target_ratio=3.0/2.0):
    """Autocrop function (unchanged)"""
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
    
    if not contours:
        return gray_image
    
    largest_contour = max(contours, key=cv2.contourArea)
    x, y, w, h = cv2.boundingRect(largest_contour)
    img_h, img_w = gray_image.shape
    
    padding_x = int(w * 0.25)
    padding_y = int(h * 0.25)
    
    bbox_w = w + 2 * padding_x
    bbox_h = h + 2 * padding_y
    current_ratio = bbox_w / bbox_h
    
    if current_ratio > target_ratio:
        target_h = bbox_w / target_ratio
        padding_y = max(padding_y, int((target_h - h) / 2))
    else:
        target_w = bbox_h * target_ratio
        padding_x = max(padding_x, int((target_w - w) / 2))
    
    x1, y1 = max(0, x - padding_x), max(0, y - padding_y)
    x2, y2 = min(img_w, x + w + padding_x), min(img_h, y + h + padding_y)
    
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
    return cropped

def find_dark(cropped_image, smoothed_image):
    """
    Find all dark regions (holes) using the smoothed image. (unchanged)
    """
    # Create binary image from smoothed version
    _, binary = cv2.threshold(smoothed_image, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    
    # Find contours
    contours, hierarchy = cv2.findContours(binary, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    
    dark_regions = []
    region_masks = []
    
    if hierarchy is not None:
        for i, contour in enumerate(contours):
            if hierarchy[0][i][3] != -1:  # Dark regions (holes)
                area = cv2.contourArea(contour)
                if area > 300:  # Filter small regions
                    dark_regions.append(contour)
                    
                    # Create individual mask for this region
                    mask = np.zeros_like(cropped_image)
                    cv2.drawContours(mask, [contour], -1, 255, -1)
                    region_masks.append(mask)
    
    return dark_regions, region_masks, binary


def merge(dark_regions, region_masks, cropped_image):
    """
    Merge the LARGEST dark region with ALL regions to its LEFT using Convex Hull.
    Then, it CLEARS any part of the merged mask that extends RIGHT of the 
    rightmost boundary of the original Largest region.
    """
    if not dark_regions:
        return None
    
    # 1. Find the largest region and its bounding box/center
    largest_idx = np.argmax([cv2.contourArea(contour) for contour in dark_regions])
    largest_region = dark_regions[largest_idx]
    largest_mask = region_masks[largest_idx]
    
    lx, ly, lw, lh = cv2.boundingRect(largest_region)
    largest_center_x = lx + lw // 2
    
    # *** จุดสำคัญ: ขอบขวาที่สุดของ Largest Region (Rightmost boundary) ***
    largest_right_boundary_x = lx + lw
    
    # 2. Identify left regions and collect all points for Convex Hull
    left_regions = []
    # เริ่มต้นด้วย Largest region เป็นพื้นฐานของจุดที่เราจะนำมาทำ Convex Hull
    all_target_points = largest_region.reshape(-1, 2) 

    for i, region in enumerate(dark_regions):
        if i == largest_idx:
            continue
            
        x, y, w, h = cv2.boundingRect(region)
        region_center_x = x + w // 2
        
        # Check if region is to the left of largest region's center
        if region_center_x < largest_center_x:
            left_regions.append(region)
            # เพิ่มจุดของ region นี้เข้าไปในเซตของจุดทั้งหมด
            all_target_points = np.concatenate((all_target_points, region.reshape(-1, 2)), axis=0)
    
    if all_target_points.size == 0:
        print("Error: No target points found for Convex Hull.")
        return None

    # 3. คำนวณ Convex Hull จากจุดทั้งหมดของ Largest + Left regions
    hull = cv2.convexHull(all_target_points)
    
    # 4. สร้าง merged_mask จาก Convex Hull
    merged_mask = np.zeros_like(cropped_image)
    cv2.drawContours(merged_mask, [hull], 0, 255, -1) # วาด Hull แบบเติมเต็ม

    # --- การปรับแก้ใหม่: ลบพื้นที่ที่อยู่ด้านขวาของขอบขวาที่สุดของ Largest Region ---
    
    # 5. สร้าง Mask สำหรับพื้นที่ด้านขวาที่ต้องลบ
    # Mask นี้จะกำหนดให้พื้นที่ตั้งแต่ขอบขวาของ largest_region ไปจนสุดขอบภาพมีค่าเป็น 0 (ดำ)
    image_h, image_w = cropped_image.shape
    right_clip_mask = np.ones_like(cropped_image, dtype=np.uint8) * 255
    
    # กำหนดพื้นที่ด้านขวาของขอบขวา largest_region ให้เป็น 0
    # y1:y2, x1:x2
    right_clip_mask[:, largest_right_boundary_x:] = 0
    
    # 6. ใช้ right_clip_mask ลบส่วนเกินด้านขวาออกจาก merged_mask
    final_merged_mask = cv2.bitwise_and(merged_mask, merged_mask, mask=right_clip_mask)
    
    # เนื่องจากเราใช้ Convex Hull, 'left_regions' list ยังคงมีไว้สำหรับ stats
    return largest_mask, final_merged_mask, left_regions

# คุณสามารถใช้ฟังก์ชัน run() เดิมได้เลย เพราะ merge() จะถูกเรียกใช้ภายใน

def create_region_visualization(cropped_image, largest_mask, merged_mask, dark_regions, left_regions):
    """
    Create visualization, marking largest (red) and left (green) regions.
    """
    # Create RGB images for visualization
    original_rgb = cv2.cvtColor(cropped_image, cv2.COLOR_GRAY2RGB)
    
    # Mark all dark regions in blue (Initial detection)
    all_regions_vis = original_rgb.copy()
    for region in dark_regions:
        cv2.drawContours(all_regions_vis, [region], -1, (255, 0, 0), 2)  # Blue
    
    # Largest region in red (Original)
    largest_vis = original_rgb.copy()
    largest_vis[largest_mask > 0] = largest_vis[largest_mask > 0] * 0.5 + np.array([0, 0, 255]) * 0.5
    
    # Comparison: Largest (Red) + Left (Green)
    comparison_vis = original_rgb.copy()
    # Draw largest in Red
    comparison_vis[largest_mask > 0] = comparison_vis[largest_mask > 0] * 0.5 + np.array([0, 0, 255]) * 0.5
    # Draw left regions in Green
    for region in left_regions:
        temp_mask = np.zeros_like(cropped_image)
        cv2.drawContours(temp_mask, [region], -1, 255, -1)
        comparison_vis[temp_mask > 0] = comparison_vis[temp_mask > 0] * 0.5 + np.array([0, 255, 0]) * 0.5
        
    # Final merged mask visualization (Yellow overlay for visibility)
    merged_vis = original_rgb.copy()
    merged_vis[merged_mask > 0] = merged_vis[merged_mask > 0] * 0.5 + np.array([0, 255, 255]) * 0.5 # Yellow/Cyan
    
    # Apply merged mask to original image
    merged_region_image = cv2.bitwise_and(cropped_image, cropped_image, mask=merged_mask)
    
    return {
        'original_cropped': original_rgb,
        'all_dark_regions': all_regions_vis,
        'largest_region_original': largest_vis,
        'comparison_largest_left': comparison_vis,
        'final_merged_mask': merged_vis,
        'merged_region_image': merged_region_image,
    }

def analyze_merge_quality(largest_mask, merged_mask, left_regions):
    """
    Analyze how well the merging worked (Largest + Left regions only)
    """
    original_pixels = np.sum(largest_mask > 0)
    merged_pixels = np.sum(merged_mask > 0)
    
    # Calculate the total pixel area of Largest + ALL left regions
    target_initial_pixels = original_pixels
    left_region_pixels = 0
    for region in left_regions:
        temp_mask = np.zeros_like(largest_mask)
        cv2.drawContours(temp_mask, [region], -1, 255, -1)
        left_region_pixels += np.sum(temp_mask > 0)
    
    target_initial_pixels += left_region_pixels
    
    merge_gain = merged_pixels - target_initial_pixels
    
    # Calculate the ratio of the final merged area to the total initial area of the target regions
    merge_coverage_ratio = merged_pixels / target_initial_pixels if target_initial_pixels > 0 else 0
    
    stats = {
        'original_largest_pixels': original_pixels,
        'left_region_pixels': left_region_pixels,
        'target_initial_pixels': target_initial_pixels,
        'merged_pixels': merged_pixels,
        'merge_gain': merge_gain,
        'merge_coverage_ratio': merge_coverage_ratio,
        'left_regions_count': len(left_regions)
    }
    
    return stats


def run(image_path):
    """
    Complete analysis flow: Crop -> CLAHE -> Blur -> Find Dark Regions -> Merge (Largest + Left) -> Visualize
    """
    print(f"\n=== ANALYZING {image_path} ===")
    
    # 1. Autocrop
    cropped_image = crop(image_path)
    print("1. Image autocropped")
    
    # 2. CLAHE and Ultra Heavy Smoothing
    smoothed_image, clahe_image = smooth(cropped_image)
    print("2. CLAHE applied and image smoothed")
    
    # 3. Find all dark regions (using the smoothed image for binarization)
    dark_regions, region_masks, binary_mask = find_dark(cropped_image, smoothed_image)
    print(f"3. Found {len(dark_regions)} dark regions")
    
    if len(dark_regions) == 0:
        print("No dark regions found!")
        return None
    
    # 4. Merge largest with left regions
    merge_result = merge(dark_regions, region_masks, cropped_image)
    
    if merge_result is None:
        print("Merging failed!")
        return None
        
    largest_mask, merged_mask, left_regions = merge_result
    
    print(f"4. Merged Largest region with {len(left_regions)} left regions, and bridged gaps.")
    
    # 5. Create visualizations
    visualizations = create_region_visualization(
        cropped_image, largest_mask, merged_mask, dark_regions, left_regions)
    
    # 6. Analyze merge quality
    stats = analyze_merge_quality(largest_mask, merged_mask, left_regions)
    
    # 7. Display results
    fig, axes = plt.subplots(2, 3, figsize=(15, 10))
    
    # Row 1
    axes[0, 0].imshow(cropped_image, cmap='gray')
    axes[0, 0].set_title('1. Original Cropped', fontsize=12, weight='bold')
    axes[0, 0].axis('off')
    
    axes[0, 1].imshow(clahe_image, cmap='gray')
    axes[0, 1].set_title('2. CLAHE Applied', fontsize=12, weight='bold')
    axes[0, 1].axis('off')
    
    axes[0, 2].imshow(visualizations['all_dark_regions'])
    axes[0, 2].set_title(f'3. All Dark Regions ({len(dark_regions)} Total)', fontsize=12, weight='bold')
    axes[0, 2].axis('off')

    # Row 2
    axes[1, 0].imshow(visualizations['comparison_largest_left'])
    axes[1, 0].set_title(f'4. Target Regions (Largest+Left: {stats["left_regions_count"]})', fontsize=12, weight='bold')
    axes[1, 0].axis('off')
    
    axes[1, 1].imshow(visualizations['final_merged_mask'])
    axes[1, 1].set_title('5. Final Merged Mask (Bridged Gaps)', fontsize=12, weight='bold')
    axes[1, 1].axis('off')
    
    # Statistics
    axes[1, 2].axis('off')
    stats_text = f"""MERGE & COVERAGE STATISTICS:

Target Regions: Largest + {stats['left_regions_count']} Left Regions

Initial Target Pixels: {stats['target_initial_pixels']:,}
Final Merged Pixels: {stats['merged_pixels']:,}
Merge Gain (Bridged Pixels): +{stats['merge_gain']:,} pixels
Coverage Ratio: {stats['merge_coverage_ratio']:.1%}

(Ratio > 100% means successful bridging/filling of gaps)
"""
    
    axes[1, 2].text(0.1, 0.9, stats_text, transform=axes[1, 2].transAxes, 
                    fontsize=10, family='monospace', verticalalignment='top')
    
    plt.suptitle(f'Region Merging Analysis (Largest + Left) - {image_path}', fontsize=16, weight='bold')
    plt.tight_layout()
    plt.show()
    
    return {
        'visualizations': visualizations,
        'stats': stats,
        'cropped_image': cropped_image,
        'merged_mask': merged_mask
    }

if __name__ == "__main__":
    image_paths = ['BTR022.jpg', 'BTR024.jpg', 'BTR056.jpg', 'BTR065.jpg']
    
    if image_paths:
        result = run(image_paths[3])
    
    print("\nProcessing completed!")