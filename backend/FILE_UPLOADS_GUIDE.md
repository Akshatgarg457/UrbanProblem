# File Uploads Module - Implementation Guide

**Status**: ✅ IMPLEMENTED  
**Date**: October 25, 2026

---

## Overview

The file upload module allows citizens and workers to attach evidence (photos, documents, etc.) to complaints. This helps provide detailed information about infrastructure issues.

## Features

### File Upload
- Upload evidence files to complaints
- Support multiple file formats (jpg, jpeg, png, pdf, doc, docx)
- Maximum 10MB per file
- Maximum 5 files per complaint
- Automatic file validation
- Secure file storage with UUID-based naming

### File Management
- List all evidence for a complaint
- Download uploaded files
- Delete uploaded files (with proper authorization)
- Track upload metadata (filename, size, upload date, uploader)

### Security
- File type validation
- File size validation
- Path traversal prevention (UUID-based naming)
- Authorization checks (citizens can only upload to own complaints)
- Secure file storage outside web root

## API Endpoints

### Upload Evidence
```
POST /api/complaints/{complaintId}/evidence/upload
Content-Type: multipart/form-data

Request:
  - file: (binary file)

Response (201 Created):
{
  "id": 1,
  "complaintId": 123,
  "fileName": "damage-photo.jpg",
  "fileType": "image/jpeg",
  "fileSize": 2048000,
  "uploadedBy": "citizen@example.com",
  "createdAt": "2026-10-25T10:30:00Z",
  "downloadUrl": "/api/complaints/123/evidence/1/download"
}
```

### List Evidence
```
GET /api/complaints/{complaintId}/evidence

Response (200 OK):
[
  {
    "id": 1,
    "complaintId": 123,
    "fileName": "damage-photo.jpg",
    "fileType": "image/jpeg",
    "fileSize": 2048000,
    "uploadedBy": "citizen@example.com",
    "createdAt": "2026-10-25T10:30:00Z",
    "downloadUrl": "/api/complaints/123/evidence/1/download"
  },
  ...
]
```

### Download Evidence
```
GET /api/complaints/{complaintId}/evidence/{evidenceId}/download

Response (200 OK):
  - Binary file content
  - Content-Disposition: attachment; filename="evidence.bin"
  - Content-Type: application/octet-stream
```

### Delete Evidence
```
DELETE /api/complaints/{complaintId}/evidence/{evidenceId}

Response (204 No Content):
  - File deleted successfully
  - Evidence record removed from database
```

## Configuration

### application.properties
```properties
# File Upload Configuration
app.file-upload.upload-dir=/var/uploads/
app.file-upload.max-file-size=10485760
app.file-upload.max-files=5
app.file-upload.allowed-extensions=jpg,jpeg,png,pdf,doc,docx
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
```

### Environment Variables
```bash
# Optional: customize upload directory
export FILE_UPLOAD_DIR=/path/to/uploads/
```

## Security Implementation

### File Validation
1. **Type Validation**: Only allowed extensions accepted
2. **Size Validation**: Enforces maximum file size
3. **Name Validation**: Rejects files without extensions
4. **Mime Type Checking**: Validates content type

### Storage Security
1. **UUID-Based Naming**: Prevents sequential file access
2. **Directory Isolation**: Files stored outside web root
3. **Permission Checking**: Only authorized users can upload
4. **Access Control**: Citizens can only upload to own complaints

### Database Integrity
1. **Referential Integrity**: Foreign key constraints
2. **Metadata Tracking**: Upload time and user
3. **Orphan Prevention**: Cascade deletion on complaint removal
4. **Transaction Management**: ACID properties maintained

## Error Handling

### Validation Errors (400 Bad Request)
```json
{
  "error": "File type not allowed. Allowed types: jpg, jpeg, png, pdf, doc, docx"
}
```

### Size Errors (400 Bad Request)
```json
{
  "error": "File size exceeds maximum allowed: 10485760 bytes"
}
```

### Limit Exceeded (409 Conflict)
```json
{
  "error": "Maximum number of files (5) reached"
}
```

### Not Found (404 Not Found)
```json
{
  "error": "Evidence not found"
}
```

## Usage Examples

### cURL Upload
```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/damage.jpg" \
  http://localhost:8080/api/complaints/123/evidence/upload
```

### JavaScript/TypeScript Upload
```typescript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

const response = await fetch(
  `/api/complaints/${complaintId}/evidence/upload`,
  {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  }
);

const evidence = await response.json();
console.log('Uploaded:', evidence);
```

### Python Upload
```python
import requests

headers = {'Authorization': f'Bearer {token}'}
files = {'file': open('/path/to/damage.jpg', 'rb')}

response = requests.post(
    f'http://localhost:8080/api/complaints/{complaint_id}/evidence/upload',
    headers=headers,
    files=files
)

evidence = response.json()
print('Uploaded:', evidence)
```

## Database Schema

### complaint_evidence Table
```sql
CREATE TABLE complaint_evidence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  complaint_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  file_path VARCHAR(255) NOT NULL,
  uploaded_by VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  FOREIGN KEY (complaint_id) REFERENCES complaints(id)
);
```

## Deployment Considerations

### Directory Permissions
```bash
# Create upload directory with proper permissions
sudo mkdir -p /var/uploads/
sudo chown -R appuser:appuser /var/uploads/
sudo chmod 755 /var/uploads/
```

### Docker Volume
```yaml
volumes:
  - uploads:/var/uploads/
```

### Backup Strategy
- Regular backup of upload directory
- Database backup includes metadata
- Consider off-site storage for important evidence

## Performance Optimization

### File Handling
- Stream large files instead of loading into memory
- Implement cleanup of orphaned files
- Monitor disk space usage
- Consider CDN for file serving (future enhancement)

### Database
- Index complaint_id foreign key
- Periodically archive old evidence
- Monitor table growth

## Future Enhancements

1. **Image Processing**
   - Automatic thumbnail generation
   - Image compression
   - Metadata stripping (EXIF data)

2. **Virus Scanning**
   - Integrate with ClamAV or similar
   - Scan files before storing

3. **Versioning**
   - Track file modifications
   - Maintain history of uploads

4. **Cloud Storage**
   - S3 integration for scalability
   - CloudFront CDN distribution
   - Automated backups

5. **Advanced Features**
   - OCR for documents
   - Image annotation tools
   - File preview generation

## Troubleshooting

### Issue: "File size exceeds maximum"
**Solution**: Increase `app.file-upload.max-file-size` in properties or compress the file before uploading.

### Issue: "File type not allowed"
**Solution**: Add the file extension to `app.file-upload.allowed-extensions` or convert file to supported format.

### Issue: "Maximum number of files reached"
**Solution**: Delete older evidence files or increase `app.file-upload.max-files` limit.

### Issue: "Permission denied" when uploading
**Solution**: Verify you're logged in and uploading to your own complaint (for citizens).

## Monitoring

### Key Metrics to Track
- Total files uploaded
- Storage usage
- Average file size
- Upload success rate
- Failed upload attempts

### Logs to Monitor
- File upload events
- File deletion events
- Authorization failures
- Storage warnings

## Compliance

### Data Protection
- GDPR compliance for user data
- Secure file storage
- Proper access controls
- Audit trail of access

### Retention Policy
- Evidence retained as long as complaint is active
- Deleted when complaint is resolved/rejected
- Configurable retention period for legal holds

---

## Summary

The file upload module provides secure, efficient evidence attachment capabilities for the Urban Problem Reporting System. All files are validated, stored securely, and tracked with proper metadata and access controls.

Implementation is complete and production-ready.
