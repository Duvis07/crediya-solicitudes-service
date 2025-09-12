package co.com.crediya.solicitudes.aws.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
public class AwsLocalStackConfig {

    @Value("${AWS_ENDPOINT_URL:http://localhost:4566}")
    private String localStackEndpoint;

    @Value("${AWS_REGION:us-east-1}")
    private String awsRegion;

    @Value("${AWS_ACCESS_KEY_ID:test}")
    private String accessKey;

    @Value("${AWS_SECRET_ACCESS_KEY:test}")
    private String secretKey;

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)
        );
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .endpointOverride(URI.create(localStackEndpoint))
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider())
            .forcePathStyle(true)
            .build();
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
            .endpointOverride(URI.create(localStackEndpoint))
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider())
            .build();
    }

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
            .endpointOverride(URI.create(localStackEndpoint))
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider())
            .build();
    }

    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
            .endpointOverride(URI.create(localStackEndpoint))
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider())
            .build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
            .endpointOverride(URI.create(localStackEndpoint))
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider())
            .build();
    }

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
            .endpointOverride(URI.create(localStackEndpoint))
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider())
            .build();
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
            .endpointOverride(URI.create(localStackEndpoint))
            .region(Region.of(awsRegion))
            .credentialsProvider(credentialsProvider())
            .build();
    }

}
