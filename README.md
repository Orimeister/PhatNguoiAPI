# 🚦 Traffic Violations Lookup System

A Spring Boot application that retrieves traffic violation records for Vietnamese license plates from the official CSGT (Cảnh Sát Giao Thông) website using OCR captcha solving.

## 📋 Table of Contents

- [Features](#features)
- [Technologies](#technologies)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Usage Examples](#usage-examples)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## ✨ Features

- 🔍 **Automated Captcha Solving** - Uses Tesseract OCR to automatically solve captchas
- 🚗 **Multiple Vehicle Types** - Supports cars (ô tô), motorcycles (mô tô), and electric bicycles (xe đạp điện)
- 🔄 **Retry Mechanism** - Automatically retries up to 5 times if captcha verification fails
- 🎯 **Accurate Extraction** - Parses violation details including date, location, type, and status
- 🌐 **RESTful API** - Clean REST API endpoints for easy integration
- 💻 **Modern UI** - Beautiful, responsive web interface
- ✅ **Input Validation** - Validates vehicle types and handles edge cases

## 🛠 Technologies

### Backend
- **Java 17+**
- **Spring Boot 3.x**
- **Apache HttpClient 5.x** - HTTP requests and cookie management
- **Tesseract OCR 5.x** - Captcha recognition
- **Jsoup** - HTML parsing (if used in extractor)

### Frontend
- **HTML5**
- **CSS3** (Vanilla)
- **JavaScript (ES6+)**
- **Fetch API**

## 📦 Prerequisites

Before running this application, ensure you have:

1. **Java 17 or higher** installed
   ```bash
   java -version
   ```

2. **Maven 3.6+** installed
   ```bash
   mvn -version
   ```

3. **Tesseract OCR** installed on your system

   **Windows:**
   ```bash
   # Download installer from: https://github.com/UB-Mannheim/tesseract/wiki
   # Default install path: C:\Program Files\Tesseract-OCR\tesseract.exe
   ```

   **macOS:**
   ```bash
   brew install tesseract
   ```

   **Linux (Ubuntu/Debian):**
   ```bash
   sudo apt-get update
   sudo apt-get install tesseract-ocr
   ```


## 🚀 Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/traffic-violations-lookup.git
   cd traffic-violations-lookup
   ```

2. **Configure Tesseract path** (if not in system PATH)
   
   Edit `application.properties`:
   ```properties
   # Windows
   tesseract.path=C:\\Program Files\\Tesseract-OCR\\tesseract.exe
   
   # macOS/Linux
   tesseract.path=/usr/local/bin/tesseract
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   Or run the JAR directly:
   ```bash
   java -jar target/traffic-violations-0.0.1-SNAPSHOT.jar
   ```

5. **Access the application**
   - API: `http://localhost:8080/api/traffic-violations`
   - UI: Open `traffic_violations_ui.html` in a web browser

## ⚙️ Configuration

### Application Properties

Create `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080

# Tesseract Configuration
tesseract.path=/usr/local/bin/tesseract
tesseract.datapath=/usr/share/tesseract-ocr/4.00/tessdata

# Logging
logging.level.orimeister.phatnong=DEBUG
```

### CORS Configuration (for frontend)

Add `@CrossOrigin` to your controller:

```java
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/traffic-violations")
public class TrafficViolationController {
    // ...
}
```

Or configure globally in a configuration class:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
```

## 📖 API Documentation

### Endpoint

```
GET /api/traffic-violations/{plate}?vehicleType={type}
```

### Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `plate` | String (path) | Yes | License plate number | `29A12345` |
| `vehicleType` | String (query) | Yes | Vehicle type: `1` (car), `2` (motorcycle), `3` (e-bike) | `2` |

### Response Format

**Success (200 OK)** - With violations:
```json
[
  {
    "dateTime": "06:09, 16/12/2024",
    "location": "Đ. Tôn Đức Thắng - CAT đi VKS",
    "violationType": "12321.5.5.a.01.Không chấp hành hiệu lệnh của đèn tín hiệu giao thông",
    "status": "Đã xử phạt"
  }
]
```

**Success (200 OK)** - No violations:
```json
[]
```

**Error (400 Bad Request)** - Missing parameter:
```json
{
  "timestamp": "2024-12-03T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Required request parameter 'vehicleType' is not present"
}
```

**Error (500 Internal Server Error)** - Server error:
```json
{
  "error": "Unable to fetch traffic violations",
  "message": "Maximum retry attempts reached. Could not verify captcha.",
  "plate": "29A12345",
  "vehicleType": "2"
}
```

## 💡 Usage Examples

### cURL

```bash
# Check motorcycle violations
curl -X GET "http://localhost:8080/api/traffic-violations/88A09568?vehicleType=2"

# Check car violations
curl -X GET "http://localhost:8080/api/traffic-violations/29A12345?vehicleType=1"
```

### JavaScript (Fetch API)

```javascript
async function checkViolations(plate, vehicleType) {
  const response = await fetch(
    `http://localhost:8080/api/traffic-violations/${plate}?vehicleType=${vehicleType}`
  );
  const violations = await response.json();
  console.log(violations);
}

checkViolations('88A09568', '2');
```

### Java (RestTemplate)

```java
RestTemplate restTemplate = new RestTemplate();
String url = "http://localhost:8080/api/traffic-violations/88A09568?vehicleType=2";
TrafficViolation[] violations = restTemplate.getForObject(url, TrafficViolation[].class);
```

## 📁 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── orimeister/
│   │       └── phatnong/
│   │           ├── TrafficViolationController.java    # REST API endpoints
│   │           ├── TrafficViolationService.java       # Business logic
│   │           ├── TrafficViolationExtractor.java     # HTML parsing
│   │           ├── TrafficViolation.java              # Data model
│   │           └── TesseractConfig.java               # Tesseract configuration
│   └── resources/
│       ├── application.properties                      # App configuration
│       └── static/
│           └── traffic_violations_ui.html                              # Frontend UI
└── test/
    └── java/
        └── orimeister/
            └── phatnong/
                └── TrafficViolationServiceTest.java    # Unit tests
```

## 🔧 How It Works

### Process Flow

```
1. User submits plate + vehicle type
         ↓
2. Fetch captcha image from CSGT website
         ↓
3. Solve captcha using Tesseract OCR
         ↓
4. Submit form with plate, vehicle type, and captcha
         ↓
5. Check response: 
   - If "404" → Captcha wrong, retry (max 5 times)
   - If success → Continue
         ↓
6. Wait 1 second (let server process)
         ↓
7. Fetch results page
         ↓
8. Check if "Không tìm thấy kết quả !" → Return []
         ↓
9. Parse HTML and extract violations
         ↓
10. Return JSON response
```

### Key Components

**TrafficViolationService**
- Manages HTTP client with cookie store (maintains session)
- Handles captcha solving with OCR
- Implements retry logic for failed captcha attempts
- Adds delay between form submission and result fetching
- Validates vehicle types (1, 2, or 3)

**TrafficViolationExtractor**
- Parses HTML from results page
- Extracts violation details (date, location, type, status)
- Handles various HTML structures

**TesseractConfig**
- Configures Tesseract OCR engine
- Sets language and recognition parameters
- Optimizes for captcha recognition

## 🐛 Troubleshooting

### Issue: Tesseract not found

**Solution:**
```bash
# Check if Tesseract is installed
tesseract --version

# If not installed, install it (see Prerequisites)

# If installed but not found, set the path in application.properties
tesseract.path=/full/path/to/tesseract
```

### Issue: Captcha always fails

**Possible causes:**
1. Poor OCR accuracy
2. Captcha image quality

**Solutions:**
- Train Tesseract with custom captcha data
- Adjust Tesseract parameters (PSM, OEM)
- Increase MAX_RETRIES in service

### Issue: CORS errors in browser

**Solution:**
Add `@CrossOrigin(origins = "*")` to controller or configure global CORS

### Issue: "Không tìm thấy kết quả !" for valid plates

**Possible causes:**
1. Wrong vehicle type selected
2. Plate doesn't exist in system
3. Network/connection issues

**Solution:**
- Verify vehicle type matches the plate
- Try different vehicle types
- Check logs for errors

### Issue: Results always return empty []

**Possible causes:**
1. Timing issue - fetching results too quickly
2. Session/cookie not maintained

**Solutions:**
- Increase sleep delay (currently 1000ms)
- Check HttpClient cookie store configuration

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Coding Standards

- Follow Java naming conventions
- Add JavaDoc comments for public methods
- Write unit tests for new features
- Keep methods small and focused

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## ⚠️ Disclaimer

This application is for educational purposes only. It retrieves publicly available information from the CSGT website. Please use responsibly and in accordance with Vietnamese law.

- Do not abuse the API with excessive requests
- Respect the CSGT website's terms of service
- This tool does not modify or falsify any official records

---

Made with ❤️ for safer roads in Vietnam 🇻🇳
