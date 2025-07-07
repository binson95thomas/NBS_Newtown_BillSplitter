#!/usr/bin/env python3
"""
Simple startup script for NBS - Newtown Bill Splitter App
"""

import sys
import subprocess
import os
from pathlib import Path

def check_python_version():
    """Check if Python version is compatible"""
    if sys.version_info < (3, 8):
        print("❌ Python 3.8 or higher is required")
        print(f"   Current version: {sys.version}")
        return False
    print(f"✅ Python version: {sys.version.split()[0]}")
    return True

def check_dependencies():
    """Check if required packages are installed"""
    required_packages = [
        'flask', 'werkzeug', 'jinja2', 'pillow', 
        'python-multipart', 'requests', 'opencv-python', 
        'pytesseract', 'numpy'
    ]
    
    missing_packages = []
    
    for package in required_packages:
        try:
            __import__(package.replace('-', '_'))
        except ImportError:
            missing_packages.append(package)
    
    if missing_packages:
        print("❌ Missing required packages:")
        for package in missing_packages:
            print(f"   - {package}")
        print("\n📦 Installing missing packages...")
        
        try:
            subprocess.check_call([sys.executable, '-m', 'pip', 'install', '-r', 'requirements.txt'])
            print("✅ Dependencies installed successfully")
            return True
        except subprocess.CalledProcessError:
            print("❌ Failed to install dependencies")
            return False
    
    print("✅ All required packages are installed")
    return True

def check_tesseract():
    """Check if Tesseract OCR is available"""
    try:
        result = subprocess.run(['tesseract', '--version'], 
                              capture_output=True, text=True, timeout=5)
        if result.returncode == 0:
            print("✅ Tesseract OCR is available")
            return True
    except (subprocess.TimeoutExpired, FileNotFoundError):
        pass
    
    print("⚠️  Tesseract OCR not found")
    print("   For better image processing, install Tesseract:")
    print("   - Windows: https://github.com/UB-Mannheim/tesseract/wiki")
    print("   - macOS: brew install tesseract")
    print("   - Ubuntu: sudo apt install tesseract-ocr")
    return False

def check_project_structure():
    """Check if project structure is correct"""
    required_files = [
        'nbs_billsplitter_app.py',
        'requirements.txt',
        'templates/index.html'
    ]
    
    missing_files = []
    for file_path in required_files:
        if not Path(file_path).exists():
            missing_files.append(file_path)
    
    if missing_files:
        print("❌ Missing required files:")
        for file_path in missing_files:
            print(f"   - {file_path}")
        return False
    
    print("✅ Project structure is correct")
    return True

def main():
    """Main startup function"""
    print("🧾 NBS - Newtown Bill Splitter App")
    print("=" * 40)
    
    # Check Python version
    if not check_python_version():
        return
    
    # Check project structure
    if not check_project_structure():
        print("\n❌ Please ensure all required files are present")
        return
    
    # Check dependencies
    if not check_dependencies():
        print("\n❌ Please install dependencies manually:")
        print("   pip install -r requirements.txt")
        return
    
    # Check Tesseract (optional)
    check_tesseract()
    
    print("\n" + "=" * 40)
    print("🚀 Starting NBS - Newtown Bill Splitter App...")
    print("🌐 The app will be available at: http://localhost:5000")
    print("📱 Press Ctrl+C to stop the server")
    print("=" * 40)
    
    try:
        # Import and run the Flask app
        from nbs_billsplitter_app import app
        app.run(debug=True, host='0.0.0.0', port=5000)
    except KeyboardInterrupt:
        print("\n👋 Goodbye!")
    except Exception as e:
        print(f"\n❌ Failed to start app: {e}")
        print("   Please check the error message above")

if __name__ == '__main__':
    main() 