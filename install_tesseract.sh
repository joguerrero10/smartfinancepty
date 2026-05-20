#!/bin/bash
# Script para instalar Tesseract en Ubuntu/Debian

echo "🔍 Instalando Tesseract OCR..."

# Ubuntu / Debian
sudo apt-get update
sudo apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-spa \
    tesseract-ocr-eng \
    libtesseract-dev

echo "✅ Tesseract instalado. Verificando versión:"
tesseract --version

echo "📚 Idiomas disponibles:"
tesseract --list-langs

echo ""
echo "📁 Ruta de tessdata:"
echo "  $(dpkg -L tesseract-ocr | grep tessdata | head -1 | xargs dirname)"
echo ""
echo "⚙️ Agrega esta ruta a application.yml:"
echo "  app.ocr.tessdata-path: $(find /usr -name 'eng.traineddata' 2>/dev/null | xargs dirname 2>/dev/null | head -1)"

# Mac (Homebrew)
# brew install tesseract tesseract-lang
# tessdata en: /usr/local/share/tessdata o /opt/homebrew/share/tessdata

# Verificar que español está instalado
if tesseract --list-langs 2>/dev/null | grep -q "spa"; then
    echo "✅ Español (spa) disponible"
else
    echo "⚠️ Instalando datos de español..."
    sudo apt-get install -y tesseract-ocr-spa
fi
