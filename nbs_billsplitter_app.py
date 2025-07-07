#!/usr/bin/env python3
"""
NBS - Newtown Bill Splitter App
A comprehensive bill splitting application with image processing and smart calculation features.
"""

from flask import Flask, render_template, request, jsonify, send_from_directory
import os
import json
import base64
from datetime import datetime
from werkzeug.utils import secure_filename
import tempfile
from typing import List, Dict, Any
import re
import cv2
import numpy as np
import pytesseract
from PIL import Image
import requests

app = Flask(__name__)
app.config['SECRET_KEY'] = 'your-secret-key-here'
app.config['UPLOAD_FOLDER'] = 'uploads'
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max file size

# Ensure upload directory exists
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

# File to store saved members
MEMBERS_FILE = 'saved_members.json'

# Allowed extensions for image upload
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}

def allowed_file(filename):
    """Check if file extension is allowed"""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def load_saved_members():
    """Load saved members from file"""
    try:
        if os.path.exists(MEMBERS_FILE):
            with open(MEMBERS_FILE, 'r') as f:
                return json.load(f)
    except Exception as e:
        print(f"Error loading saved members: {e}")
    return []

def save_members(members):
    """Save members to file"""
    try:
        with open(MEMBERS_FILE, 'w') as f:
            json.dump(members, f)
    except Exception as e:
        print(f"Error saving members: {e}")

def calculate_totals(members, items, discount_percent=0):
    """Calculate totals for given members and items"""
    if not members or not items:
        return {
            'subtotal': 0,
            'discount_percent': discount_percent,
            'discount_amount': 0,
            'final_total': 0,
            'member_totals': {}
        }
    
    # Calculate totals for each member
    member_totals = {member: {'total': 0, 'items': []} for member in members}
    subtotal = 0
    
    for item in items:
        item_name = item['name']
        item_price = float(item['price'])
        assigned_members = item.get('assignedTo', [])
        
        subtotal += item_price
        
        if assigned_members:
            price_per_person = item_price / len(assigned_members)
            for member in assigned_members:
                if member in member_totals:
                    member_totals[member]['total'] += price_per_person
                    member_totals[member]['items'].append({
                        'name': item_name,
                        'price': price_per_person,
                        'shared_with': len(assigned_members)
                    })
    
    # Apply discount proportionally
    discount_amount = (subtotal * discount_percent) / 100
    final_total = subtotal - discount_amount
    
    # Apply discount to each member proportionally
    for member in member_totals:
        if member_totals[member]['total'] > 0:
            member_discount = (member_totals[member]['total'] / subtotal) * discount_amount
            member_totals[member]['discount'] = member_discount
            member_totals[member]['final_total'] = member_totals[member]['total'] - member_discount
        else:
            member_totals[member]['discount'] = 0
            member_totals[member]['final_total'] = 0
    
    return {
        'subtotal': round(subtotal, 2),
        'discount_percent': discount_percent,
        'discount_amount': round(discount_amount, 2),
        'final_total': round(final_total, 2),
        'member_totals': {
            member: {
                'total': round(data['total'], 2),
                'discount': round(data['discount'], 2),
                'final_total': round(data['final_total'], 2),
                'items': data['items']
            }
            for member, data in member_totals.items()
        }
    }

def preprocess_image_advanced(image_path: str) -> str:
    """
    Advanced image preprocessing for better OCR results
    """
    try:
        # Read image
        image = cv2.imread(image_path)
        if image is None:
            return image_path
        
        # Convert to grayscale
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # Apply noise reduction
        denoised = cv2.fastNlMeansDenoising(gray)
        
        # Apply adaptive thresholding for better text extraction
        adaptive_thresh = cv2.adaptiveThreshold(
            denoised, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2
        )
        
        # Apply morphological operations to clean up the image
        kernel = np.ones((1, 1), np.uint8)
        cleaned = cv2.morphologyEx(adaptive_thresh, cv2.MORPH_CLOSE, kernel)
        
        # Apply Gaussian blur to smooth edges
        blurred = cv2.GaussianBlur(cleaned, (1, 1), 0)
        
        # Save processed image
        processed_path = image_path.replace('.', '_processed.')
        cv2.imwrite(processed_path, blurred)
        
        return processed_path
    except Exception as e:
        print(f"Advanced image preprocessing failed: {e}")
        return image_path

def extract_text_with_gemini(image_path: str) -> List[Dict[str, Any]]:
    """
    Extract items and prices directly from image using Gemini Vision API
    """
    try:
        import google.generativeai as genai
        
        # Get API key from environment variable
        api_key = os.getenv('GEMINI_API_KEY')
        if not api_key:
            print("❌ GEMINI_API_KEY not found in environment variables")
            return []
        
        # Configure Gemini
        genai.configure(api_key=api_key)
        model = genai.GenerativeModel('gemini-1.5-flash')
        
        # Read image using PIL
        from PIL import Image
        pil_image = Image.open(image_path)
        
        # Create prompt for receipt parsing
        prompt = """
        Analyze this restaurant receipt and extract all menu items with their prices.
        Return ONLY a JSON array with this exact format:
        [
            {"name": "Item Name", "price": 12.99},
            {"name": "Another Item", "price": 8.50}
        ]
        
        Rules:
        - Only include actual menu items (not totals, taxes, tips, etc.)
        - Use the exact item names as they appear
        - Convert all prices to decimal numbers
        - Skip any lines that are clearly not food items
        - If you can't extract items, return an empty array []
        """
        
        # Generate response
        try:
            response = model.generate_content([prompt, pil_image])
            
            # Parse JSON response
            try:
                # Extract JSON from response text
                response_text = response.text.strip()
                
                # Find JSON array in the response
                json_start = response_text.find('[')
                json_end = response_text.rfind(']') + 1
                
                if json_start != -1 and json_end != 0:
                    json_str = response_text[json_start:json_end]
                    items = json.loads(json_str)
                    
                    # Validate items
                    valid_items = []
                    for item in items:
                        if isinstance(item, dict) and 'name' in item and 'price' in item:
                            try:
                                price = float(item['price'])
                                if 0.01 <= price <= 1000:  # Reasonable price range
                                    valid_items.append({
                                        'name': item['name'].title(),
                                        'price': price
                                    })
                            except (ValueError, TypeError):
                                continue
                    
                    print(f"✅ Gemini extracted {len(valid_items)} items")
                    return valid_items
                else:
                    print("❌ No valid JSON found in Gemini response")
                    print(f"Response: {response_text}")
                    return []
                    
            except json.JSONDecodeError as e:
                print(f"❌ Failed to parse Gemini JSON response: {e}")
                print(f"Response: {response.text}")
                return []
                
        except Exception as e:
            print(f"❌ Gemini API call failed: {e}")
            return []
            
    except ImportError:
        print("❌ Google Generative AI library not installed")
        print("Install with: pip install google-generativeai")
        return []
    except Exception as e:
        print(f"❌ Gemini extraction failed: {e}")
        return []

def extract_text_with_easyocr(image_path: str) -> str:
    """
    Extract text using EasyOCR (if available)
    """
    try:
        import easyocr
        reader = easyocr.Reader(['en'])
        results = reader.readtext(image_path)
        
        # Extract text from results
        text_lines = []
        for (bbox, text, prob) in results:
            if prob > 0.5:  # Only include text with >50% confidence
                text_lines.append(text)
        
        return '\n'.join(text_lines)
    except ImportError:
        print("EasyOCR not available, falling back to Tesseract")
        return ""
    except Exception as e:
        print(f"EasyOCR extraction failed: {e}")
        return ""

def extract_text_with_google_vision(image_path: str) -> str:
    """
    Extract text using Google Cloud Vision API (if configured)
    """
    try:
        from google.cloud import vision
        
        # Initialize client
        client = vision.ImageAnnotatorClient()
        
        # Read image file
        with open(image_path, 'rb') as image_file:
            content = image_file.read()
        
        image = vision.Image(content=content)
        
        # Perform text detection
        response = client.text_detection(image=image)
        texts = response.text_annotations
        
        if texts:
            return texts[0].description
        return ""
        
    except ImportError:
        print("Google Cloud Vision not available")
        return ""
    except Exception as e:
        print(f"Google Vision extraction failed: {e}")
        return ""

def extract_text_with_tesseract_enhanced(image_path: str) -> str:
    """
    Enhanced Tesseract OCR with multiple configurations
    """
    try:
        # Preprocess image
        processed_path = preprocess_image_advanced(image_path)
        
        # Try different Tesseract configurations
        configs = [
            '--oem 3 --psm 6',  # Default
            '--oem 3 --psm 8',  # Single word
            '--oem 3 --psm 11', # Sparse text
            '--oem 1 --psm 6',  # Legacy engine
        ]
        
        best_text = ""
        best_confidence = 0
        
        for config in configs:
            try:
                text = pytesseract.image_to_string(processed_path, config=config)
                # Simple confidence check based on text length and content
                confidence = len([c for c in text if c.isalnum()]) / max(len(text), 1)
                
                if confidence > best_confidence:
                    best_confidence = confidence
                    best_text = text
            except Exception as e:
                print(f"Tesseract config {config} failed: {e}")
                continue
        
        # Clean up processed image
        if processed_path != image_path and os.path.exists(processed_path):
            os.remove(processed_path)
        
        return best_text
    except Exception as e:
        print(f"Enhanced Tesseract extraction failed: {e}")
        return ""

def extract_text_with_ocr(image_path: str) -> str:
    """
    Multi-method OCR extraction with fallbacks
    """
    # Try EasyOCR first (best for receipt text)
    text = extract_text_with_easyocr(image_path)
    if text.strip():
        print("✅ EasyOCR extraction successful")
        return text
    
    # Try Google Cloud Vision if available
    text = extract_text_with_google_vision(image_path)
    if text.strip():
        print("✅ Google Vision extraction successful")
        return text
    
    # Fallback to enhanced Tesseract
    text = extract_text_with_tesseract_enhanced(image_path)
    if text.strip():
        print("✅ Enhanced Tesseract extraction successful")
        return text
    
    # Final fallback to basic Tesseract
    try:
        text = pytesseract.image_to_string(image_path)
        print("⚠️ Basic Tesseract extraction used")
        return text
    except Exception as e:
        print(f"All OCR methods failed: {e}")
        return ""

def call_claude_ai_for_extraction(text: str) -> List[Dict[str, Any]]:
    """
    Call Claude AI API to extract items and prices from bill text
    This is a placeholder - replace with actual Claude API integration
    """
    try:
        # TODO: Replace with actual Claude API call
        # For now, we'll use an enhanced regex-based approach
        
        # Claude AI would receive the text and return structured data
        # Example API call:
        # response = requests.post(
        #     "https://api.anthropic.com/v1/messages",
        #     headers={
        #         "x-api-key": "your-claude-api-key",
        #         "anthropic-version": "2023-06-01",
        #         "content-type": "application/json"
        #     },
        #     json={
        #         "model": "claude-3-sonnet-20240229",
        #         "max_tokens": 1000,
        #         "messages": [{
        #             "role": "user",
        #             "content": f"Extract menu items and prices from this restaurant bill text. Return as JSON array with 'name' and 'price' fields: {text}"
        #         }]
        #     }
        # )
        
        # For demo purposes, return enhanced regex extraction
        return extract_items_from_text_enhanced(text)
        
    except Exception as e:
        print(f"Claude AI extraction failed: {e}")
        return extract_items_from_text_enhanced(text)

def extract_items_from_text_enhanced(text: str) -> List[Dict[str, Any]]:
    """
    Enhanced extraction using multiple regex patterns and validation
    """
    items = []
    
    # More comprehensive patterns for menu items and prices
    patterns = [
        r'(.+?)\s+(\$?\d+\.?\d*)',  # Item name followed by price
        r'(\d+)\s*x\s*(.+?)\s+(\$?\d+\.?\d*)',  # Quantity x Item name Price
        r'(.+?)\s*-\s*(\$?\d+\.?\d*)',  # Item name - Price
        r'(.+?)\s+(\d+\.\d{2})',  # Item name with decimal price
        r'(.+?)\s+(\d+)\.(\d{2})',  # Item name with separate decimal
        r'(.+?)\s+(\d+,\d{2})',  # Item name with comma decimal
        r'(.+?)\s+(\d+)\s*(\d{2})',  # Item name with space decimal
    ]
    
    # Skip patterns for non-item lines
    skip_patterns = [
        r'total', r'subtotal', r'tax', r'tip', r'receipt', r'thank you',
        r'date', r'time', r'server', r'table', r'order', r'bill', r'change',
        r'cash', r'credit', r'debit', r'visa', r'mastercard', r'amex',
        r'gratuity', r'service charge', r'balance', r'amount due', r'change',
        r'payment', r'card', r'terminal', r'reference', r'transaction'
    ]
    
    lines = text.split('\n')
    for line in lines:
        line = line.strip()
        if not line or len(line) < 3:
            continue
            
        # Skip lines that look like headers, totals, or non-item lines
        if any(re.search(pattern, line.lower()) for pattern in skip_patterns):
            continue
            
        # Try to match item patterns
        for pattern in patterns:
            match = re.search(pattern, line, re.IGNORECASE)
            if match:
                if len(match.groups()) == 2:  # Item name and price
                    name, price = match.groups()
                    name = name.strip()
                    price = re.sub(r'[^\d.]', '', price)  # Remove non-numeric chars
                    
                    if name and price and len(name) > 1:
                        try:
                            price_float = float(price)
                            if 0.01 <= price_float <= 1000:  # Reasonable price range
                                items.append({
                                    'name': name.title(),
                                    'price': price_float
                                })
                        except ValueError:
                            continue
                elif len(match.groups()) == 3:  # Quantity, item name, price or item name, decimal parts
                    if 'x' in line.lower():  # Quantity x Item format
                        qty, name, price = match.groups()
                        name = name.strip()
                        price = re.sub(r'[^\d.]', '', price)
                        
                        if name and price and len(name) > 1:
                            try:
                                price_float = float(price)
                                if 0.01 <= price_float <= 1000:
                                    items.append({
                                        'name': f"{name.title()}",
                                        'price': price_float
                                    })
                            except ValueError:
                                continue
                    else:  # Decimal parts format
                        name, whole, decimal = match.groups()
                        name = name.strip()
                        price = f"{whole}.{decimal}"
                        
                        if name and len(name) > 1:
                            try:
                                price_float = float(price)
                                if 0.01 <= price_float <= 1000:
                                    items.append({
                                        'name': name.title(),
                                        'price': price_float
                                    })
                            except ValueError:
                                continue
                break
    
    # Remove duplicates and sort by price
    unique_items = []
    seen_names = set()
    for item in items:
        if item['name'] not in seen_names:
            unique_items.append(item)
            seen_names.add(item['name'])
    
    return sorted(unique_items, key=lambda x: x['price'], reverse=True)[:20]  # Limit to 20 items max

def extract_items_from_text(text: str) -> List[Dict[str, Any]]:
    """
    Legacy function - now calls the enhanced version
    """
    return extract_items_from_text_enhanced(text)

@app.route('/')
def index():
    """Main application page"""
    return render_template('index.html')

@app.route('/api/members', methods=['GET'])
def get_members():
    """Get saved members"""
    members = load_saved_members()
    return jsonify({'members': members})

@app.route('/api/members', methods=['POST'])
def save_members_api():
    """Save members"""
    try:
        data = request.json
        members = data.get('members', [])
        save_members(members)
        return jsonify({'success': True, 'message': 'Members saved successfully'})
    except Exception as e:
        return jsonify({'error': f'Failed to save members: {str(e)}'}), 500

@app.route('/upload', methods=['POST'])
def upload_file():
    """Handle bill image upload and text extraction"""
    if 'file' not in request.files:
        return jsonify({'error': 'No file uploaded'}), 400
    
    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'No file selected'}), 400
    
    if file and allowed_file(file.filename):
        try:
            # Save uploaded file temporarily
            filename = secure_filename(file.filename)
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S_')
            filename = timestamp + filename
            filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
            file.save(filepath)
            
            # Try Gemini Vision API first (best for receipt parsing)
            extracted_items = extract_text_with_gemini(filepath)
            
            if extracted_items:
                # Clean up the temporary file
                try:
                    os.remove(filepath)
                except:
                    pass
                
                # Load saved members
                members = load_saved_members()
                
                # Set equal split by default (all members selected for all items)
                for item in extracted_items:
                    item['assignedTo'] = members.copy()
                
                # Calculate totals
                totals = calculate_totals(members, extracted_items)
                
                return jsonify({
                    'items': extracted_items,
                    'message': f'🎯 Gemini AI successfully extracted {len(extracted_items)} items from your bill!',
                    'method': 'gemini',
                    'members': members,
                    'totals': totals
                })
            
            # Fallback to OCR + text processing
            print("🔄 Gemini failed, trying OCR methods...")
            extracted_text = extract_text_with_ocr(filepath)
            
            if not extracted_text.strip():
                # If OCR fails, return sample data for demo
                sample_items = [
                    {'name': 'Caesar Salad', 'price': 12.99},
                    {'name': 'Grilled Chicken', 'price': 18.50},
                    {'name': 'Pasta Carbonara', 'price': 16.75},
                    {'name': 'Garlic Bread', 'price': 6.50},
                    {'name': 'Chocolate Cake', 'price': 8.99}
                ]
                
                # Load saved members
                members = load_saved_members()
                
                # Set equal split by default
                for item in sample_items:
                    item['assignedTo'] = members.copy()
                
                # Calculate totals
                totals = calculate_totals(members, sample_items)
                
                # Clean up the temporary file
                try:
                    os.remove(filepath)
                except:
                    pass
                
                return jsonify({
                    'items': sample_items,
                    'message': 'OCR extraction failed. Using sample data for demo.',
                    'extracted_text': 'OCR extraction failed - using sample data',
                    'method': 'sample',
                    'members': members,
                    'totals': totals
                })
            
            # Use Claude AI (or enhanced extraction) to process the text
            extracted_items = call_claude_ai_for_extraction(extracted_text)
            
            # Load saved members
            members = load_saved_members()
            
            # Set equal split by default
            for item in extracted_items:
                item['assignedTo'] = members.copy()
            
            # Calculate totals
            totals = calculate_totals(members, extracted_items)
            
            # Clean up the temporary file
            try:
                os.remove(filepath)
            except:
                pass
            
            if extracted_items:
                return jsonify({
                    'items': extracted_items,
                    'message': f'Successfully extracted {len(extracted_items)} items from your bill!',
                    'extracted_text': extracted_text[:500] + '...' if len(extracted_text) > 500 else extracted_text,
                    'method': 'ocr',
                    'members': members,
                    'totals': totals
                })
            else:
                # Fallback to sample data if no items extracted
                sample_items = [
                    {'name': 'Caesar Salad', 'price': 12.99},
                    {'name': 'Grilled Chicken', 'price': 18.50},
                    {'name': 'Pasta Carbonara', 'price': 16.75},
                    {'name': 'Garlic Bread', 'price': 6.50},
                    {'name': 'Chocolate Cake', 'price': 8.99}
                ]
                
                # Set equal split by default
                for item in sample_items:
                    item['assignedTo'] = members.copy()
                
                # Calculate totals
                totals = calculate_totals(members, sample_items)
                
                return jsonify({
                    'items': sample_items,
                    'message': 'No items could be extracted. Using sample data for demo.',
                    'extracted_text': extracted_text[:500] + '...' if len(extracted_text) > 500 else extracted_text,
                    'method': 'sample',
                    'members': members,
                    'totals': totals
                })
            
        except Exception as e:
            return jsonify({'error': f'Failed to process image: {str(e)}'}), 500
    
    return jsonify({'error': 'Invalid file type'}), 400

@app.route('/calculate', methods=['POST'])
def calculate_split():
    """Calculate bill split based on member assignments"""
    try:
        data = request.json
        members = data.get('members', [])
        items = data.get('items', [])
        discount_percent = float(data.get('discount', 0))
        
        if not members or not items:
            return jsonify({'error': 'Members and items are required'}), 400
        
        # Calculate totals
        totals = calculate_totals(members, items, discount_percent)
        
        return jsonify(totals)
        
    except Exception as e:
        return jsonify({'error': f'Calculation failed: {str(e)}'}), 500

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
