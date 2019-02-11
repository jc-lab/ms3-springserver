# ms3-springserver
MS3 Server for spring-boot

MS3는 Mini Simple Storage Service의 약자로 만들어진 프로젝트 명으로써

Amazon S3 클라이언트와 호환되는! (라이브러리 자체가 호환됩니다!)

Object Storage 서버 및 클라이언트 입니다.



<https://bintray.com/jc-lab/cloud/ms3-springserver>

<https://bintray.com/jc-lab/cloud/ms3-client>



jcenter repository를 통해 사용하실 수 있습니다.



서버/클라이언트 함께 있는 예제소스입니다.

<https://github.com/jc-lab/ms3-springserver-test>



추후 상세 설명을 올리겠습니다...(언젠가...ㅠㅠ)

```java
package kr.jclab.cloud.ms3.test.ms3springservertest;

import kr.jclab.cloud.ms3.server.spring.EnableMS3SpringServer;
import kr.jclab.cloud.ms3.server.spring.MS3SpringServer;
import kr.jclab.cloud.ms3.server.spring.MS3SpringServerConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.File;

@EnableMS3SpringServer
public class MS3ServerConfig implements MS3SpringServerConfigurerAdapter {

    @Autowired
    ApplicationContext applicationContext;

    @Override
    public void configure(MS3SpringServer server) {
        server.setResourceDirectory(new File("D:\\test\\zeroupserver-resource"));
        server.registerRequestMapping("/ms3", Integer.MAX_VALUE - 2);
    }
}
```



서버는 이런식으로 @EnableMS3SpringServer Annotation으로 설정된 설정 클래스에 MS3SpringServerConfigurerAdapter을 구현해서 사용하시면 됩니다.

(서버 설정은 이게 끝이에요^^)



아 build.gradle의 dependency에 아래항목이 추가되어야 합니다.

```groovy
// https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk-s3
compile group: 'com.amazonaws', name: 'aws-java-sdk-s3', version: '1.11.470'

// https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
compile group: 'org.xerial', name: 'sqlite-jdbc', version: '3.7.2'
```





클라이언트 사용법 : 

```java
@RequestMapping(path = "list")
public ResponseEntity<Object> list() {
    AmazonS3 s3 = MS3ClientBuilder.standard().serverUrl("http://localhost:9000/ms3/").build();
    Object response = s3.listObjects("test");
    return new ResponseEntity<>(response, HttpStatus.OK);
}

@RequestMapping(path = "test1")
public ResponseEntity<InputStreamResource> test1() {
    AmazonS3 s3 = MS3ClientBuilder.standard().serverUrl("http://localhost:9000/ms3/").build();
    S3Object response = s3.getObject("test", "bbbb");
    System.err.println(response.getObjectMetadata());
    return new ResponseEntity<>(new InputStreamResource(response.getObjectContent()), HttpStatus.OK);
}
```

이런식으로 AmazonS3와 호환됩니다~



build.gradle의 dependency에 아래항목이 추가되어야 합니다.

```groovy
// https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk-s3
compile group: 'com.amazonaws', name: 'aws-java-sdk-s3', version: '1.11.470'
```



사용 가능한 MS3Client (AmazonS3) 메서드들



v1.0.0

- getUrl ( 절대경로가 아닌 상대경로형식으로 리턴됩니다! 예시: public/12345678-1111-2222-333333333333 )
- listObjects
- listObjectsV2
- getObjectMetadata
- getObject
- putObject
- getObjectAsString
- deleteObject
  v1.0.2
- listBuckets
- createBucket

지원하지 않는 메서드들

- setEndpoint
- setRegion
- setS3ClientOoptions
- changeObjectStorageClass
- setObjectRedirectLocation
- listNextBatchOfObjects
- listVersions
- listNextBatchOfVersions
- getS3AccountOwner
- getS3AccountOwner
- doesBucketExist
- doesBucketExistV2
- headBucket
- getBucketLocation
- createBucket(region)
- getObjectAcl
- setObjectAcl
- getBucketAcl
- setBucketAcl
- getObjectTagging
- setObjectTagging
- deleteObjectTagging
- deleteBucket
- copyObject
- copyPart
- deleteObjects
- deleteVersion
- getBucketLoggingConfiguration
- setBucketLoggingConfiguration
- getBucketLifecycleConfiguration
- setBucketLifecycleConfiguration
- deleteBucketLifecycleConfiguration
- getBucketCrossOriginConfiguration
- setBucketCrossOriginConfiguration
- deleteBucketCrossOriginConfiguration
- getBucketTaggingConfiguration
- setBucketTaggingConfiguration
- deleteBucketTaggingConfiguration
- getBucketNotificationConfiguration
- setBucketNotificationConfiguration
- getBucketWebsiteConfiguration
- setBucketWebsiteConfiguration
- deleteBucketWebsiteConfiguration
- getBucketPolicy
- setBucketPolicy
- deleteBucketPolicy
- generatePresignedUrl
- initiateMultipartUpload
- uploadPart
- listParts
- abortMultipartUpload
- completeMultipartUpload
- listMultipartUploads
- getCachedResponseMetadata
- restoreObject
- restoreObjectV2
- enableRequesterPays
- disableRequesterPays
- isRequesterPaysEnabled
- setBucketReplicationConfiguration
- getBucketReplicationConfiguration
- deleteBucketReplicationConfiguration
- doesObjectExist
- getBucketAccelerateConfiguration
- setBucketAccelerateConfiguration
- deleteBucketMetricsConfiguration
- getBucketMetricsConfiguration
- setBucketMetricsConfiguration
- listBucketMetricsConfigurations
- deleteBucketAnalyticsConfiguration
- getBucketAnalyticsConfiguration
- setBucketAnalyticsConfiguration
- listBucketAnalyticsConfiguration
- deleteBucketInventoryConfiguration
- getBucketInventoryConfiguration
- setBucketInventoryConfiguration
- listBucketInventoryConfiguration
- deleteBucketEncryption
- getBucketEncryption
- setBucketEncryption
- setPublicAccessBlock
- deletePublicAccessBlock
- getBucketPolicyStatus
- selectObjectContent
- setObjectLegalHold
- getObjectLegalHold
- setObjectLockConfiguration
- getObjectLockConfiguration
- setObjectRetention
- getObjectRetention
- getRegion
- getRegionName
- 기타 Deprecated된 메서드들



권한관리같은거 없습니다.. 직접 spring-security으로 구현해서 쓰시구요...^^



metadata와 data는 모두 파일로 관리됩니다.

DB는 SQLite가 쓰이는데 그저 share-object(getUrl으로 가져온것)에 대한 정보만 저장합니다.



성능같은거 아직 신경쓰지 못하고 만든 prototype입니다..

개선 너무 감사하고 환영합니다^^



Apache License 2.0 라이센스를 사용합니다.



블로그 : https://jsty.tistory.com/153
