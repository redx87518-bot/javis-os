#!/bin/bash
# JAVIS OS — Push to GitHub
# Usage: bash push-to-github.sh

set -e

echo "🤖 JAVIS OS — GitHub Push Script"
echo "=================================="
echo ""

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "❌ Git is not installed. Please install Git first."
    exit 1
fi

# Check if inside the project directory
if [ ! -f "settings.gradle.kts" ]; then
    echo "❌ Run this script from inside the javis-os/ project directory."
    exit 1
fi

# Config
GITHUB_USERNAME="redx87518-bot"
REPO_NAME="javis-os"
GITHUB_URL="https://github.com/$GITHUB_USERNAME/$REPO_NAME"

echo "📋 GitHub Username: $GITHUB_USERNAME"
echo "📦 Repository: $REPO_NAME"
echo ""
echo "⚠️  IMPORTANT: Before running this script:"
echo "   1. Go to github.com/settings/tokens and create a NEW token"
echo "      (your previous token was shared publicly and must be revoked)"
echo "   2. Create the repository at: https://github.com/new"
echo "      - Name: $REPO_NAME"
echo "      - Visibility: Private (recommended)"
echo "      - Do NOT add any files"
echo ""
read -p "Enter your NEW GitHub personal access token: " GITHUB_TOKEN
echo ""

if [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ No token provided. Exiting."
    exit 1
fi

# Initialize git if needed
if [ ! -d ".git" ]; then
    echo "📁 Initializing git repository..."
    git init
    git branch -M main
fi

# Configure remote
git remote remove origin 2>/dev/null || true
git remote add origin "https://$GITHUB_USERNAME:$GITHUB_TOKEN@github.com/$GITHUB_USERNAME/$REPO_NAME.git"

# Create .gitignore if it doesn't exist
if [ ! -f ".gitignore" ]; then
cat > .gitignore << 'EOF'
# Gradle
.gradle/
build/
*.class
local.properties
.idea/
*.iml
*.ipr
*.iws
.DS_Store
captures/
.externalNativeBuild/
.cxx/
*.apk
*.aab
*.ap_
*.dex
gen/
out/
release/
proguard/
EOF
fi

# Stage and commit
echo "📝 Staging files..."
git add .
git commit -m "🤖 JAVIS OS v1.0 — Complete Android AI Assistant

Features:
- AI chat with Gemini 1.5 Flash / OpenAI GPT-4o-mini
- Voice activation (speech recognition + TTS)
- Wake word detection ('Hey JAVIS')
- Floating bubble overlay
- Weather (no API key needed)
- WhatsApp automation
- Contacts + phone calls with confirmation
- Flashlight, volume, battery control
- Notification listener (WhatsApp, Telegram, SMS)
- Accessibility Service + Quick Settings Tile
- Room DB + DataStore memory system
- Beautiful futuristic dark Jetpack Compose UI
- Onboarding screen
- GitHub Actions APK build workflow"

echo ""
echo "🚀 Pushing to GitHub..."
git push -u origin main

echo ""
echo "✅ Successfully pushed to: $GITHUB_URL"
echo ""
echo "🔨 GitHub Actions will automatically build the APK."
echo "   Check: $GITHUB_URL/actions"
echo ""
echo "📲 Once built, download the APK from:"
echo "   $GITHUB_URL/actions → latest run → Artifacts"
echo ""
echo "⚙️  Remember to add your Gemini API key as a GitHub Secret:"
echo "   $GITHUB_URL/settings/secrets/actions/new"
echo "   Name: GEMINI_API_KEY"
echo "   Value: (your Gemini API key from aistudio.google.com)"
