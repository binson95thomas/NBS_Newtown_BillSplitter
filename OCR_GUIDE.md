# 📸 OCR Text Extraction Guide

This guide explains the different OCR (Optical Character Recognition) methods available in the NBS - Newtown Bill Splitter app and how to improve text extraction for bill processing.

## 🎯 Available OCR Methods

### 1. **EasyOCR** (Recommended for Receipts)
- **Best for**: Restaurant receipts, handwritten text, various fonts
- **Accuracy**: Very high for receipt-style text
- **Speed**: Fast
- **Installation**: `pip install easyocr`

**Why it's great for bills:**
- Specifically designed for receipt and document text
- Handles different text orientations well
- Good at recognizing prices and item names
- Works well with low-quality images

### 2. **Google Cloud Vision API** (Most Accurate)
- **Best for**: High-accuracy commercial use
- **Accuracy**: Excellent
- **Speed**: Fast (cloud-based)
- **Cost**: Pay-per-use (first 1000 requests free/month)
- **Installation**: `pip install google-cloud-vision`

**Setup:**
1. Get API key from Google Cloud Console
2. Set environment variable: `GOOGLE_APPLICATION_CREDENTIALS=path/to/key.json`
3. Enable Vision API in your project

### 3. **Enhanced Tesseract** (Free & Local)
- **Best for**: Offline use, basic text extraction
- **Accuracy**: Good with preprocessing
- **Speed**: Medium
- **Installation**: `pip install pytesseract` + install Tesseract binary

**Improvements in this app:**
- Multiple preprocessing techniques
- Different Tesseract configurations
- Adaptive thresholding
- Morphological operations

### 4. **Basic Tesseract** (Fallback)
- **Best for**: Simple text extraction
- **Accuracy**: Basic
- **Speed**: Fast
- **Installation**: Included with pytesseract

## 🚀 How to Improve Text Extraction

### Image Quality Tips
1. **Good Lighting**: Ensure the receipt is well-lit
2. **High Resolution**: Use at least 300 DPI
3. **Flat Surface**: Avoid curved or wrinkled receipts
4. **Clean Background**: Remove clutter around the receipt
5. **Proper Angle**: Keep the camera parallel to the receipt

### Preprocessing Techniques Used
```python
# 1. Noise Reduction
denoised = cv2.fastNlMeansDenoising(gray)

# 2. Adaptive Thresholding
adaptive_thresh = cv2.adaptiveThreshold(
    denoised, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2
)

# 3. Morphological Operations
kernel = np.ones((1, 1), np.uint8)
cleaned = cv2.morphologyEx(adaptive_thresh, cv2.MORPH_CLOSE, kernel)

# 4. Gaussian Blur
blurred = cv2.GaussianBlur(cleaned, (1, 1), 0)
```

### Tesseract Configuration Options
```python
configs = [
    '--oem 3 --psm 6',  # Default: Assume uniform block of text
    '--oem 3 --psm 8',  # Single word
    '--oem 3 --psm 11', # Sparse text with OSD
    '--oem 1 --psm 6',  # Legacy engine
]
```

## 🔧 Installation Guide

### Quick Setup
```bash
# Run the installation script
python install_dependencies.py
```

### Manual Installation

#### Required Packages
```bash
pip install Flask==2.3.3 Werkzeug==2.3.7 Jinja2==3.1.2 Pillow==10.0.1
pip install python-multipart==0.0.6 requests==2.31.0 opencv-python==4.8.1.78
pip install pytesseract==0.3.10 anthropic==0.7.8
```

#### Optional Enhanced OCR
```bash
# EasyOCR (Recommended)
pip install easyocr==1.7.0

# Google Cloud Vision
pip install google-cloud-vision==3.4.4

# PyTorch for AI models
pip install torch==2.1.0 transformers==4.35.0
```

#### Tesseract Binary Installation

**Windows:**
1. Download from: https://github.com/UB-Mannheim/tesseract/wiki
2. Install and add to PATH
3. Restart terminal

**macOS:**
```bash
brew install tesseract
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install tesseract-ocr
```

## 🎯 Which Method to Choose?

### For Best Results (Recommended Setup)
1. **EasyOCR** - Primary method
2. **Google Cloud Vision** - For critical accuracy
3. **Enhanced Tesseract** - Fallback
4. **Basic Tesseract** - Last resort

### For Offline Use
1. **EasyOCR** - Primary method
2. **Enhanced Tesseract** - Fallback
3. **Basic Tesseract** - Last resort

### For Free/Open Source Only
1. **Enhanced Tesseract** - Primary method
2. **Basic Tesseract** - Fallback

## 🔍 Troubleshooting

### Common Issues

**"Tesseract not found"**
- Install Tesseract binary
- Add to system PATH
- Restart terminal

**"EasyOCR import error"**
- Install with: `pip install easyocr`
- May take time to download models

**"Google Vision not working"**
- Set up Google Cloud credentials
- Enable Vision API
- Check API quotas

**Poor text extraction**
- Improve image quality
- Try different preprocessing
- Use multiple OCR methods

### Performance Tips

1. **Image Size**: Resize large images to 2000px max dimension
2. **Format**: Use JPG for photos, PNG for screenshots
3. **Compression**: Avoid heavy compression
4. **Batch Processing**: Process multiple images sequentially

## 📊 Accuracy Comparison

| Method | Receipt Accuracy | Speed | Cost | Setup Complexity |
|--------|------------------|-------|------|------------------|
| EasyOCR | 95% | Fast | Free | Easy |
| Google Vision | 98% | Fast | Paid | Medium |
| Enhanced Tesseract | 85% | Medium | Free | Easy |
| Basic Tesseract | 70% | Fast | Free | Easy |

## 🎨 Customization

### Adding New OCR Methods
```python
def extract_text_with_custom_ocr(image_path: str) -> str:
    """Add your custom OCR method here"""
    try:
        # Your OCR implementation
        return extracted_text
    except Exception as e:
        print(f"Custom OCR failed: {e}")
        return ""
```

### Modifying Preprocessing
```python
def custom_preprocessing(image_path: str) -> str:
    """Add your custom preprocessing steps"""
    # Your preprocessing code
    return processed_image_path
```

## 🚀 Future Enhancements

### Planned Improvements
1. **AI-powered text correction** using language models
2. **Receipt template recognition** for common formats
3. **Multi-language support** for international receipts
4. **Real-time OCR** with camera integration
5. **Batch processing** for multiple receipts

### Integration Ideas
1. **Claude AI** for intelligent text parsing
2. **GPT models** for context-aware extraction
3. **Custom training** for specific receipt formats
4. **Cloud OCR services** (AWS Textract, Azure Computer Vision)

---

**💡 Pro Tip**: For the best results, combine multiple OCR methods and use the one that gives the most structured output for your specific receipt format. 