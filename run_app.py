#!/usr/bin/env python3
"""
Setup and run script for NBS - Newtown Bill Splitter App
"""

import os
import sys
import subprocess
from pathlib import Path

def create_project_structure():
    """Create the necessary project structure"""
    
    # Create directories
    directories = [
        'templates',
        'static',
        'uploads',
        'static/css',
        'static/js',
        'static/images'
    ]
    
    for directory in directories:
        Path(directory).mkdir(parents=True, exist_ok=True)
        print(f"✅ Created directory: {directory}")

def create_html_template():
    """Create the HTML template file"""
    
    # The HTML content is already provided in the artifacts above
    # You would copy the HTML template content to templates/index.html
    template_path = Path('templates/index.html')
    
    if not template_path.exists():
        print("⚠️  Please copy the HTML template content to templates/index.html")
        print("   The HTML template is provided in the artifacts above.")
    else:
        print("✅ HTML template already exists")

def install_requirements():
    """Install Python requirements"""
    
    print("📦 Installing Python requirements...")
    try:
        subprocess.check_call([sys.executable, '-m', 'pip', 'install', '-r', 'requirements.txt'])
        print("✅ Requirements installed successfully")
    except subprocess.CalledProcessError as e:
        print(f"❌ Failed to install requirements: {e}")
        return False
    except FileNotFoundError:
        print("⚠️  requirements.txt not found. Please create it with the provided content.")
        return False
    
    return True

def check_tesseract():
    """Check if Tesseract OCR is installed (optional for advanced OCR)"""
    
    try:
        subprocess.check_call(['tesseract', '--version'], 
                            stdout=subprocess.DEVNULL, 
                            stderr=subprocess.DEVNULL)
        print("✅ Tesseract OCR is installed")
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("⚠️  Tesseract OCR not found. Install it for better image text extraction:")
        print("   - Windows: Download from https://github.com/UB-Mannheim/tesseract/wiki")
        print("   - macOS: brew install tesseract")
        print("   - Ubuntu: sudo apt install tesseract-ocr")
        return False

def main():
    """Main setup and run function"""
    
    print("🚀 Setting up NBS - Newtown Bill Splitter App...")
    print("=" * 50)
    
    # Create project structure
    create_project_structure()
    
    # Check HTML template
    create_html_template()
    
    # Install requirements
    if not install_requirements():
        print("\n❌ Setup failed. Please resolve the issues above.")
        return
    
    # Check optional dependencies
    check_tesseract()
    
    print("\n" + "=" * 50)
    print("✅ Setup complete!")
    print("\n📋 Next steps:")
    print("1. Copy the HTML template content to templates/index.html")
    print("2. Copy the main Flask app code to app.py")
    print("3. Run the app with: python app.py")
    print("\n🌐 The app will be available at: http://localhost:5000")
    
    # Ask if user wants to run the app now
    try:
        run_now = input("\n🤔 Would you like to run the app now? (y/n): ").lower().strip()
        if run_now in ['y', 'yes']:
            print("\n🚀 Starting the app...")
            if Path('start_app.py').exists():
                os.system('python start_app.py')
            else:
                print("❌ start_app.py not found. Please create it with the Flask app code provided above.")
    except KeyboardInterrupt:
        print("\n👋 Goodbye!")

if __name__ == '__main__':
    main()
