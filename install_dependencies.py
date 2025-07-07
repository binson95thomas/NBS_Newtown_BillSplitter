#!/usr/bin/env python3
"""
Dependency installation script for NBS - Newtown Bill Splitter App
Handles both required and optional dependencies with fallbacks
"""

import subprocess
import sys
import os

def install_package(package_name, fallback_packages=None):
    """Install a package with fallback options"""
    try:
        print(f"📦 Installing {package_name}...")
        subprocess.check_call([sys.executable, '-m', 'pip', 'install', package_name])
        print(f"✅ {package_name} installed successfully")
        return True
    except subprocess.CalledProcessError:
        print(f"❌ Failed to install {package_name}")
        
        if fallback_packages:
            for fallback in fallback_packages:
                try:
                    print(f"🔄 Trying fallback: {fallback}")
                    subprocess.check_call([sys.executable, '-m', 'pip', 'install', fallback])
                    print(f"✅ {fallback} installed successfully")
                    return True
                except subprocess.CalledProcessError:
                    print(f"❌ Failed to install {fallback}")
                    continue
        
        return False

def main():
    """Main installation function"""
    print("🧾 NBS - Newtown Bill Splitter - Dependency Installation")
    print("=" * 50)
    
    # Required packages (core functionality)
    required_packages = [
        "Flask==2.3.3",
        "Werkzeug==2.3.7", 
        "Jinja2==3.1.2",
        "Pillow==10.0.1",
        "python-multipart==0.0.6",
        "requests==2.31.0",
        "opencv-python==4.8.1.78",
        "pytesseract==0.3.10",
        "anthropic==0.7.8",
        "google-generativeai==0.3.2"  # For Gemini API
    ]
    
    # Optional packages (enhanced OCR)
    optional_packages = [
        ("easyocr==1.7.0", ["easyocr"]),  # Best for receipt text
        ("google-cloud-vision==3.4.4", ["google-cloud-vision"]),  # Google Cloud Vision
        ("transformers==4.35.0", ["transformers"]),  # For advanced AI models
        ("torch==2.1.0", ["torch", "torchvision"])  # PyTorch for AI models
    ]
    
    # Install required packages
    print("\n🔧 Installing required packages...")
    failed_required = []
    
    for package in required_packages:
        if not install_package(package):
            failed_required.append(package)
    
    if failed_required:
        print(f"\n❌ Failed to install required packages: {failed_required}")
        print("Please install them manually or check your Python environment.")
        return False
    
    # Install optional packages
    print("\n🎯 Installing optional packages (enhanced OCR)...")
    installed_optional = []
    
    for package, fallbacks in optional_packages:
        if install_package(package, fallbacks):
            installed_optional.append(package.split('==')[0])
    
    print(f"\n✅ Installation complete!")
    print(f"📋 Installed optional packages: {installed_optional}")
    
    # Check for Gemini API key
    gemini_key = os.getenv('GEMINI_API_KEY')
    if gemini_key:
        print(f"🎯 Found GEMINI_API_KEY environment variable")
        print(f"   Gemini AI integration is ready!")
    else:
        print(f"⚠️  GEMINI_API_KEY not found in environment variables")
        print(f"   To use Gemini AI, set the environment variable:")
        print(f"   Windows: set GEMINI_API_KEY=your_api_key_here")
        print(f"   Linux/Mac: export GEMINI_API_KEY=your_api_key_here")
    
    # Create a simple requirements file for future use
    with open('requirements_simple.txt', 'w') as f:
        f.write("# Core requirements\n")
        for package in required_packages:
            f.write(f"{package}\n")
        f.write("\n# Optional packages (for enhanced OCR)\n")
        for package, _ in optional_packages:
            f.write(f"# {package}\n")
    
    print("\n📝 Created requirements_simple.txt for future reference")
    print("\n🚀 You can now run the app with: python start_app.py")
    
    return True

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\n👋 Installation cancelled by user")
    except Exception as e:
        print(f"\n❌ Installation failed: {e}") 