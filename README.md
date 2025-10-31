# AWS S3 File Uploader

A Spring Boot REST API application with a web UI for uploading files to AWS S3. Features a clean, centered web interface where you can upload files and view all files stored in your S3 bucket.

## ✨ Features

- 📤 **File Upload**: Upload files to AWS S3 with optional folder organization
- 📋 **File List**: View all files in your S3 bucket with details (name, size, last modified)
- 🌐 **Web UI**: Modern, responsive web interface accessible at `http://localhost:8080`
- 🔗 **Presigned URLs**: Secure, time-limited URLs (24 hours) for file access
- ⚙️ **Easy Configuration**: Simple YAML-based configuration

## 🛠️ Tech Stack

- **Backend**: Java 17, Spring Boot 3.3.4
- **Cloud**: AWS S3 (AWS SDK v2)
- **Build Tool**: Maven
- **UI**: Vanilla JavaScript, HTML5, CSS3

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- AWS Account with:
  - S3 bucket created
  - IAM credentials configured (Access Key ID & Secret Access Key)
  - Required permissions: `s3:PutObject`, `s3:GetObject`, `s3:ListBucket`

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd s3-uploader
```

### 2. Configure AWS Credentials

Configure your AWS credentials using one of these methods:

**Option A: AWS CLI (Recommended)**
```bash
aws configure
```
Enter your:
- AWS Access Key ID
- AWS Secret Access Key
- Default region name (e.g., `eu-central-1`)
- Default output format (e.g., `json`)

**Option B: Environment Variables**
```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_DEFAULT_REGION=eu-central-1
```

### 3. Configure Application

Edit `src/main/resources/application.yml`:

```yaml
aws:
  region: eu-central-1                    # Your AWS region
  s3:
    bucket: your-bucket-name              # Your S3 bucket name
    publicReadAcl: true                    # Attempt to set public-read ACL (optional)
```

### 4. Create S3 Bucket (if not exists)

```bash
aws s3api create-bucket \
  --bucket your-bucket-name \
  --region eu-central-1 \
  --create-bucket-configuration LocationConstraint=eu-central-1
```

### 5. Run the Application

**Option A: Using Maven**
```bash
mvn spring-boot:run
```

**Option B: Using Run Script**
```bash
./run.sh
```

**Option C: Using IDE**
- Open `S3UploaderApplication.java` in IntelliJ IDEA or VS Code
- Click the "Run" button next to the `main` method

### 6. Access the Web UI

Open your browser and navigate to:
```
http://localhost:8080
```

## 📡 API Endpoints

### Upload File
- **Endpoint**: `POST /api/upload`
- **Content-Type**: `multipart/form-data`
- **Parameters**:
  - `file` (required): The file to upload
  - `folder` (optional): Folder path in S3 (e.g., `receipts/2025`)

**Example using curl:**
```bash
curl -F "file=@/path/to/file.pdf" \
     -F "folder=documents" \
     http://localhost:8080/api/upload
```

**Response:**
```
https://your-bucket.s3.eu-central-1.amazonaws.com/uuid-filename.pdf?X-Amz-Algorithm=...
```

### List Files
- **Endpoint**: `GET /api/files`
- **Response**: JSON array of file information

**Example:**
```bash
curl http://localhost:8080/api/files
```

**Response:**
```json
[
  {
    "name": "document.pdf",
    "key": "uuid-document.pdf",
    "url": "https://...",
    "size": "2.5 MB",
    "lastModified": "2025-10-31 12:00:00"
  }
]
```

## 🔐 AWS IAM Permissions

Your AWS IAM user/role needs the following permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::your-bucket-name/*",
        "arn:aws:s3:::your-bucket-name"
      ]
    }
  ]
}
```

## 🌐 Presigned URLs

The application uses **presigned URLs** to provide secure access to files:
- URLs are valid for **24 hours**
- Work regardless of bucket public access settings
- More secure than making objects publicly accessible

## 📁 Project Structure

```
s3-uploader/
├── src/
│   ├── main/
│   │   ├── java/com/sarptekin/awsuploader/
│   │   │   ├── S3UploaderApplication.java    # Main application class
│   │   │   ├── config/
│   │   │   │   └── S3Config.java             # AWS S3 client configuration
│   │   │   ├── controller/
│   │   │   │   └── UploadController.java    # REST API endpoints
│   │   │   └── service/
│   │   │       └── S3StorageService.java    # S3 upload/list logic
│   │   └── resources/
│   │       ├── application.yml               # Configuration file
│   │       └── static/
│   │           └── index.html               # Web UI
│   └── test/                                 # Test files
├── pom.xml                                   # Maven dependencies
├── run.sh                                    # Quick run script (macOS/Linux)
├── run.bat                                   # Quick run script (Windows)
└── README.md                                 # This file
```

## 🐛 Troubleshooting

### "Access Denied" when opening files
- This was fixed by using presigned URLs. Make sure you're running the latest version.

### "Bucket does not exist"
- Verify your bucket name in `application.yml`
- Create the bucket using AWS CLI or AWS Console

### "Credentials not found"
- Ensure AWS credentials are configured via `aws configure`
- Or set environment variables `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`

### Port 8080 already in use
- Change the port in `application.yml`: `server.port: 8081`

## 📝 Configuration Options

Edit `src/main/resources/application.yml`:

| Setting | Description | Default |
|---------|-------------|---------|
| `server.port` | Server port | `8080` |
| `aws.region` | AWS region | `eu-central-1` |
| `aws.s3.bucket` | S3 bucket name | Required |
| `aws.s3.publicReadAcl` | Attempt to set public-read ACL | `true` |
| `spring.servlet.multipart.max-file-size` | Max upload size | `25MB` |

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is open source and available under the MIT License.

## 👤 Author

**Sarptekin**

## 🙏 Acknowledgments

- Spring Boot team
- AWS SDK for Java team

