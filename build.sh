#!/bin/bash
# Build script for SKGuard - Linux/Mac
# Generates both Lite and Premium versions

echo "========================================"
echo "  SKGuard Build System"
echo "========================================"
echo ""

show_menu() {
    echo "Select build option:"
    echo "1. Build Lite (FREE version)"
    echo "2. Build Premium (PAID version)"
    echo "3. Build BOTH versions"
    echo "4. Clean build directory"
    echo "5. Exit"
    echo ""
    read -p "Enter choice (1-5): " choice
    
    case $choice in
        1) build_lite ;;
        2) build_premium ;;
        3) build_both ;;
        4) clean_build ;;
        5) exit 0 ;;
        *) echo "Invalid choice!"; show_menu ;;
    esac
}

build_lite() {
    echo ""
    echo "Building SKGuard Lite..."
    mvn clean package -P lite
    echo ""
    echo "Lite build complete! Check target/SKGuard-Lite-1.0-SNAPSHOT.jar"
    read -p "Press Enter to continue..."
    show_menu
}

build_premium() {
    echo ""
    echo "Building SKGuard Premium..."
    mvn clean package -P premium
    echo ""
    echo "Premium build complete! Check target/SKGuard-Premium-1.0-SNAPSHOT.jar"
    read -p "Press Enter to continue..."
    show_menu
}

build_both() {
    echo ""
    echo "Building both versions..."
    echo ""
    echo "[1/2] Building Lite..."
    mvn clean package -P lite
    echo ""
    echo "[2/2] Building Premium..."
    mvn clean package -P premium
    echo ""
    echo "Both builds complete!"
    echo "- Lite: target/SKGuard-Lite-1.0-SNAPSHOT.jar"
    echo "- Premium: target/SKGuard-Premium-1.0-SNAPSHOT.jar"
    read -p "Press Enter to continue..."
    show_menu
}

clean_build() {
    echo ""
    echo "Cleaning build directory..."
    mvn clean
    echo "Clean complete!"
    read -p "Press Enter to continue..."
    show_menu
}

show_menu

