# Ollama AI Image Recognition Setup

This guide helps you set up Ollama for AI-powered image recognition in the photo gallery app.

## Prerequisites

1. **Install Ollama** (if not already installed):
   ```bash
   # macOS
   brew install ollama
   
   # Linux
   curl -fsSL https://ollama.com/install.sh | sh
   
   # Windows
   # Download from https://ollama.com/download
   ```

## Setup Steps

### 1. Start Ollama Service
```bash
ollama serve
```
Keep this running in the background.

### 2. Install Vision Model
```bash
# Install LLaVA (recommended - 4.5GB)
ollama pull llava

# Alternative models:
ollama pull llava:7b      # Smaller, faster
ollama pull llava:13b     # Larger, more accurate
ollama pull llama3.2-vision  # Latest vision model
```

### 3. Verify Installation
```bash
# List installed models
ollama list

# Test the model
ollama run llava "Describe this image" --image path/to/test/image.jpg
```

## Usage in App

1. **Launch the photo gallery app**:
   ```bash
   ./gradlew :composeApp:run
   ```

2. **Click on any photo** in the gallery

3. **View AI analysis** in the popup dialog

## Error Messages & Solutions

| Error Message | Solution |
|---------------|----------|
| "Ollama service not running" | Run `ollama serve` |
| "Vision model not found" | Run `ollama pull llava` |
| "Connection failed" | Check if Ollama is running on port 11434 |
| "Unsupported image format" | Use JPG, PNG, GIF, BMP, WebP, or TIFF |

## Model Information

### LLaVA (Recommended)
- **Size**: ~4.5GB
- **Capabilities**: Object detection, scene description, text recognition
- **Performance**: Good balance of speed and accuracy

### Llama 3.2 Vision
- **Size**: ~7.9GB  
- **Capabilities**: Advanced vision understanding, better reasoning
- **Performance**: Higher accuracy, slower processing

## Troubleshooting

### Performance Issues
- Use smaller models (llava:7b) for faster processing
- Ensure sufficient RAM (8GB+ recommended)
- Close other applications during analysis

### Network Issues
- Verify Ollama is listening on `http://localhost:11434`
- Check firewall settings
- Try restarting Ollama service

### Image Issues
- Supported formats: JPG, JPEG, PNG, GIF, BMP, WebP, TIFF
- File size limit: ~10MB (practical limit)
- Resolution: Works with various resolutions

## Example Outputs

**Nature Photo**: "This image shows a beautiful mountain landscape with snow-capped peaks, a clear blue sky, and evergreen trees in the foreground. The scene appears to be captured during golden hour with warm lighting."

**Portrait**: "The image depicts a person smiling at the camera, wearing casual clothing. The background appears to be an indoor setting with soft lighting."

**Urban Scene**: "This is a city street scene with buildings, cars, and pedestrians. The architecture appears modern with glass and concrete structures."

## API Details

The app uses the Ollama REST API directly:
- **Endpoint**: `POST http://localhost:11434/api/generate`
- **Model**: `llava` (configurable)
- **Input**: Base64-encoded image + text prompt
- **Output**: JSON response with description

## Next Steps

- **Experiment** with different models to find the best balance
- **Customize prompts** in the code for specific use cases
- **Add model selection** in the UI (future enhancement)