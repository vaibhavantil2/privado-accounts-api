package ai.privado.demo.s3store;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import java.nio.file.Path;

public class S3Service {
    
    private final S3Client s3;

    public S3Service() {
        this.s3 = S3Client.builder()
                          .region(Region.YOUR_REGION) // Set your preferred region
                          .credentialsProvider(DefaultCredentialsProvider.create())
                          .build();
    }

    public void uploadFile(String bucketName, String key, Path file) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                                .bucket(bucketName)
                                                                .key(key)
                                                                .build();
            s3.putObject(putObjectRequest, file);
            System.out.println("File uploaded successfully to S3 bucket: " + bucketName);
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
    }

    public void close() {
        s3.close();
    }
}
