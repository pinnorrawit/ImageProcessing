import cv2
import matplotlib.pyplot as plt
import numpy as np
import os

# ------------------ Image Preprocessing ------------------ #
def smooth(image):
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
    
    return gray_image[y1:y2, x1:x2]

# ------------------ Dark Region Detection ------------------ #
def find_dark(cropped_image, smoothed_image):
    _, binary = cv2.threshold(smoothed_image, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    contours, hierarchy = cv2.findContours(binary, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    dark_regions = []
    region_masks = []
    
    if hierarchy is not None:
        for i, contour in enumerate(contours):
            if hierarchy[0][i][3] != -1:
                area = cv2.contourArea(contour)
                if area > 300:
                    dark_regions.append(contour)
                    mask = np.zeros_like(cropped_image)
                    cv2.drawContours(mask, [contour], -1, 255, -1)
                    region_masks.append(mask)
    return dark_regions, region_masks

# ------------------ Merge Regions ------------------ #
def merge(dark_regions, region_masks, cropped_image):
    if not dark_regions:
        return None
    
    RIGHT_TRIM_MARGIN = 0 
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
        return None

    hull = cv2.convexHull(all_target_points)
    initial_merged_mask = np.zeros_like(cropped_image, dtype=np.uint8)
    cv2.drawContours(initial_merged_mask, [hull], 0, 255, -1) 

    height, width = cropped_image.shape
    largest_right_boundary = np.zeros(height, dtype=np.int32) 
    largest_y_coords, largest_x_coords = np.where(largest_mask == 255)
    
    max_x_by_y = {}
    for y, x in zip(largest_y_coords, largest_x_coords):
        max_x_by_y[y] = max(max_x_by_y.get(y, 0), x)
    
    for y, max_x in max_x_by_y.items():
        largest_right_boundary[y] = max_x + RIGHT_TRIM_MARGIN 

    trimmed_mask = initial_merged_mask.copy()
    for y in range(height):
        boundary_x = largest_right_boundary[y]
        if boundary_x > 0:
            merged_x_coords = np.where(initial_merged_mask[y, :] == 255)[0]
            pixels_to_trim_x = merged_x_coords[merged_x_coords > boundary_x]
            trimmed_mask[y, pixels_to_trim_x] = 0
            
    return largest_mask, trimmed_mask, left_regions

# ------------------ Post-processing ------------------ #
def postprocess_mask(mask):
    kernel = np.ones((5,5), np.uint8)
    mask_closed = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)
    mask_opened = cv2.morphologyEx(mask_closed, cv2.MORPH_OPEN, kernel)
    mask_smooth = cv2.GaussianBlur(mask_opened, (5,5), 0)
    return mask_smooth.astype(np.uint8)

def calculate_masked_average(cropped_image, mask):
    binary_mask = (mask > 0).astype(np.uint8)
    masked_data = cropped_image * binary_mask
    roi_pixels = masked_data[masked_data > 0]
    if roi_pixels.size == 0:
        return 0.0
    return np.mean(roi_pixels)

# ------------------ Gradient Region Application ------------------ #
def region_gradient(preprocessed_img, region_mask, min_factor=0.0, max_factor=1.0, linear_ratio=0.2):
    """
    linear_ratio: สัดส่วนระยะจากขอบ region ที่จะลด factor แบบ linear
    หลังจากนั้นจะลด factor แบบ exponential
    """
    mask_bin = (region_mask > 0).astype(np.uint8)
    if np.sum(mask_bin) == 0:
        return preprocessed_img.copy()
    
    # ระยะจากขอบของ mask
    dist = cv2.distanceTransform(mask_bin, cv2.DIST_L2, 5)
    max_dist = dist.max()
    if max_dist == 0:
        return preprocessed_img.copy()
    
    # normalize distance 0 (ขอบ) -> 1 (center)
    norm_dist = dist / max_dist
    
    # แบ่งเป็น linear และ exponential
    factor = np.zeros_like(norm_dist, dtype=np.float32)
    linear_threshold = linear_ratio  # สัดส่วน linear
    linear_mask = norm_dist <= linear_threshold
    expo_mask = norm_dist > linear_threshold
    
    # linear decay: ขอบสว่าง -> นิดหน่อยเข้ามา
    factor[linear_mask] = max_factor - (max_factor - (min_factor + 0.1*(max_factor-min_factor))) * (norm_dist[linear_mask]/linear_threshold)
    
    # exponential decay หลังจาก linear zone
    # ใช้ฟังก์ชัน e^(-k*x) เพื่อให้ลดเร็ว
    k = 10  # ปรับให้ลดเร็ว/ช้า
    factor[expo_mask] = (min_factor + 0.1*(max_factor-min_factor)) * np.exp(-k*(norm_dist[expo_mask]-linear_threshold)/(1-linear_threshold))
    
    factor = np.clip(factor, min_factor, max_factor)
    
    result = preprocessed_img.astype(np.float32).copy()
    result[mask_bin == 1] *= factor[mask_bin == 1]
    return np.clip(result, 0, 255).astype(np.uint8)



# ------------------ Visualization ------------------ #
def visualize(cropped_image, merged_mask_post, gradient_applied_img=None):
    plt.figure(figsize=(15,5))
    plt.subplot(1,3,1)
    plt.imshow(cropped_image, cmap='gray')
    plt.title("Cropped X-ray")
    plt.axis('off')
    
    plt.subplot(1,3,2)
    plt.imshow(merged_mask_post, cmap='gray')
    plt.title("Mask (Post-processed)")
    plt.axis('off')
    
    if gradient_applied_img is not None:
        plt.subplot(1,3,3)
        plt.imshow(gradient_applied_img, cmap='gray')
        plt.title("Region Gradient Applied")
        plt.axis('off')
    plt.show()

# ------------------ Run Full Pipeline ------------------ #
def run_pipeline(image_path, output_dir="output"):
    os.makedirs(output_dir, exist_ok=True)
    base = os.path.basename(image_path).split('.')[0]
    
    cropped_img = crop(image_path)
    preprocessed_img, clahe_img = smooth(cropped_img)
    dark_regions, region_masks = find_dark(cropped_img, preprocessed_img)
    
    if not dark_regions:
        print(f"No dark regions found in {image_path}.")
        return
    
    merge_result = merge(dark_regions, region_masks, cropped_img)
    if merge_result is None:
        print(f"Merge failed for {image_path}.")
        return
    
    largest_mask, merged_mask, left_regions = merge_result
    merged_mask_post = postprocess_mask(merged_mask)
    
    avg_intensity = calculate_masked_average(cropped_img, merged_mask_post)
    print(f"Average intensity in merged region for {image_path}: {avg_intensity:.2f}")
    
    # Apply gradient inside region only
    gradient_img = region_gradient(cropped_img, merged_mask_post, min_factor=0.0, max_factor=1.0)
    
    # Save results
    cv2.imwrite(os.path.join(output_dir, f"{base}_cropped.png"), cropped_img)
    cv2.imwrite(os.path.join(output_dir, f"{base}_mask.png"), merged_mask_post)
    cv2.imwrite(os.path.join(output_dir, f"{base}_region_gradient.png"), gradient_img)
    
    visualize(cropped_img, merged_mask_post, gradient_img)
    
    return cropped_img, merged_mask_post, gradient_img

# ------------------ Example Usage ------------------ #
if __name__ == "__main__":
    image_paths = ['BTR022.jpg', 'BTR024.jpg', 'BTR056.jpg', 'BTR065.jpg']
    for path in image_paths:
        run_pipeline(path)
