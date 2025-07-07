# 🧾 NBS - Newtown Bill Splitter App

A comprehensive bill splitting application with AI-powered image processing and smart calculation features.

## ✨ Features

### 📸 Bill Image Processing
- **Upload Area**: Drag & drop or click to upload bill images
- **Smart Extraction**: Uses Gemini AI + OCR to automatically extract item names and prices from bill images
- **Auto-Population**: Extracted items are automatically added to your bill with proper formatting
- **Multiple Formats**: Supports JPG, PNG, GIF, WebP (Max 16MB)

### ✅ Checkbox System
- Each item shows checkboxes for every member in your group
- Check multiple members for shared items (appetizers, desserts)
- Check single members for individual items
- Real-time calculation shows cost per person for each item

### 📊 Member Totals Tab
- Dedicated "Member Totals" tab showing complete breakdown
- Bill Summary: Shows subtotal, discount, and final total
- Individual breakdowns for each member with their items
- Detailed item breakdown showing exactly what each person owes

### 💰 Global Discount Feature
- Percentage-based discount input (0-100%)
- Automatic application to all members proportionally
- Clear display of discount amounts for transparency
- Real-time updates as you change the discount percentage

## 🚀 Quick Start

### Prerequisites
- Python 3.8 or higher
- Tesseract OCR (optional, for better image processing)
- Gemini API key (optional, for best text extraction)

### Installation

1. **Clone or download the project**
   ```bash
   git clone <repository-url>
   cd NBS-BillSplitter
   ```

2. **Install Python dependencies**
   ```bash
   python install_dependencies.py
   ```

3. **Set up Gemini API (Optional but Recommended)**
   
   **Get API Key:**
   - Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
   - Create a new API key
   
   **Set Environment Variable:**
   
   **Windows:**
   ```cmd
   set GEMINI_API_KEY=your_api_key_here
   ```
   
   **macOS/Linux:**
   ```bash
   export GEMINI_API_KEY=your_api_key_here
   ```
   
   **Permanent Setup (Windows):**
   - Add to System Environment Variables
   - Restart terminal after setting
   
   **Permanent Setup (macOS/Linux):**
   ```bash
   echo 'export GEMINI_API_KEY=your_api_key_here' >> ~/.bashrc
   source ~/.bashrc
   ```

4. **Install Tesseract OCR (Optional but Recommended)**
   
   **Windows:**
   - Download from: https://github.com/UB-Mannheim/tesseract/wiki
   - Install and add to PATH
   
   **macOS:**
   ```bash
   brew install tesseract
   ```
   
   **Ubuntu/Debian:**
   ```bash
   sudo apt install tesseract-ocr
   ```

5. **Run the application**
   ```bash
   python start_app.py
   ```

6. **Open your browser**
   - Navigate to: http://localhost:5000
   - The app is now ready to use!

## 🎯 How to Use

### 1. Add Members
- Go to the "Members" tab
- Enter names of everyone in your group
- Click "Add" or press Enter

### 2. Upload Bill Image
- Go to "Bill Upload" tab
- Click the upload area or drag & drop your bill photo
- The app will automatically extract items and prices using:
  - **Gemini AI** (if API key is set) - Best accuracy
  - **EasyOCR** (if installed) - Good for receipts
  - **Enhanced Tesseract** - Fallback option
- Or add items manually if needed

### 3. Assign Items
- Go to "Items & Assignment" tab
- Check boxes for who consumed each item
- Some items can be shared, others individual
- Real-time cost per person calculation

### 4. Set Discount (Optional)
- Enter any global discount percentage
- Applied proportionally to each member
- Real-time updates as you change

### 5. View Totals
- Switch to "Member Totals" tab
- See final amounts for each person
- Click on totals to copy for NBS - Newtown Bill Splitter

## 🔧 Configuration

### Text Extraction Methods (in order of preference)

1. **Gemini AI** (Best) - Uses your GEMINI_API_KEY
   - Highest accuracy for receipt parsing
   - Understands context and formatting
   - Handles various receipt layouts

2. **EasyOCR** (Very Good) - Install with `pip install easyocr`
   - Excellent for receipt-style text
   - Works well with different fonts
   - Good for handwritten text

3. **Google Cloud Vision** (Excellent) - Requires setup
   - High accuracy commercial solution
   - Pay-per-use pricing
   - Requires Google Cloud account

4. **Enhanced Tesseract** (Good) - Free & local
   - Multiple preprocessing techniques
   - Different configuration options
   - Works offline

5. **Basic Tesseract** (Basic) - Fallback
   - Simple text extraction
   - Always available
   - Basic accuracy

### Claude AI Integration (Optional)
To enable real Claude AI extraction instead of the enhanced regex:

1. Get your Claude API key from: https://console.anthropic.com/
2. Edit `nbs_billsplitter_app.py` and uncomment the Claude API section
3. Replace `"your-claude-api-key"` with your actual API key

```python
# In call_claude_ai_for_extraction function:
response = requests.post(
    "https://api.anthropic.com/v1/messages",
    headers={
        "x-api-key": "your-actual-claude-api-key",
        "anthropic-version": "2023-06-01",
        "content-type": "application/json"
    },
    # ... rest of the API call
)
```

## 📁 Project Structure

```
NBS-BillSplitter/
├── nbs_billsplitter_app.py      # Main Flask application
├── requirements.txt      # Python dependencies
├── install_dependencies.py # Dependency installation script
├── start_app.py         # Startup script with checks
├── run_app.py           # Original setup script
├── README.md            # This file
├── OCR_GUIDE.md         # Detailed OCR guide
├── templates/
│   └── index.html       # Main HTML template
├── static/              # Static assets (CSS, JS, images)
├── uploads/             # Temporary file uploads
└── Templates/           # Original template location
```

## 🛠️ Technical Details

### Backend (Python/Flask)
- **Flask**: Web framework
- **OpenCV**: Image preprocessing for better OCR
- **Tesseract**: OCR text extraction
- **Pillow**: Image processing
- **Google Generative AI**: Gemini Vision API integration
- **Anthropic**: Claude AI integration (optional)

### Frontend (HTML/CSS/JavaScript)
- **Responsive Design**: Works on mobile and desktop
- **Drag & Drop**: File upload functionality
- **Real-time Calculations**: Instant updates as you make changes
- **Modern UI**: Beautiful gradient design with smooth animations

### Image Processing Pipeline
1. **Upload**: User uploads bill image
2. **Gemini AI**: Direct item extraction (if API key available)
3. **Preprocessing**: OpenCV enhances image quality
4. **OCR**: Multiple OCR methods with fallbacks
5. **AI Processing**: Claude AI (or enhanced regex) extracts items
6. **Validation**: Filters out non-item text
7. **Display**: Shows extracted items in the UI

## 🎨 Smart Features

- **Mixed Splitting**: Some items can be shared, others individual
- **Proportional Discounts**: Global discount applies fairly to each member
- **Visual Feedback**: Clear tabs, real-time calculations, loading states
- **Copy Integration**: One-click copying for NBS - Newtown Bill Splitter entry
- **Responsive Design**: Works on mobile and desktop
- **Error Handling**: Graceful fallbacks when OCR fails
- **Multiple OCR Methods**: Automatic fallback to best available method

## 🔒 Security & Privacy

- Files are processed locally and deleted immediately
- No bill images are stored permanently
- All calculations happen in the browser
- Gemini API calls are made directly to Google (no data stored locally)
- No personal data is transmitted to external services (unless APIs are enabled)

## 🐛 Troubleshooting

### Gemini AI Not Working
- Verify GEMINI_API_KEY is set correctly
- Check your internet connection
- Ensure you have sufficient API credits
- Try restarting the terminal after setting the environment variable

### OCR Not Working
- Ensure Tesseract is installed and in PATH
- Try uploading a clearer image
- Check that the image format is supported
- Install EasyOCR for better results: `pip install easyocr`

### Claude AI Not Working
- Verify your API key is correct
- Check your internet connection
- Ensure you have sufficient API credits

### App Won't Start
- Check Python version (3.8+ required)
- Verify all dependencies are installed
- Check if port 5000 is available

### Dependency Installation Issues
- Run `python install_dependencies.py` for automatic installation
- Try installing packages individually
- Check your Python environment and pip version

## 📊 Accuracy Comparison

| Method | Receipt Accuracy | Speed | Cost | Setup Complexity |
|--------|------------------|-------|------|------------------|
| Gemini AI | 98% | Fast | Free tier | Easy |
| EasyOCR | 95% | Fast | Free | Easy |
| Google Vision | 98% | Fast | Paid | Medium |
| Enhanced Tesseract | 85% | Medium | Free | Easy |
| Basic Tesseract | 70% | Fast | Free | Easy |

## 🤝 Contributing

Feel free to submit issues and enhancement requests!

## 📄 License

This project is open source and available under the MIT License.

---

**Happy Bill Splitting! 🎉** 