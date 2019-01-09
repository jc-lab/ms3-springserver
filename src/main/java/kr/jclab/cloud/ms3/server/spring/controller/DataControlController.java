/*
 * Copyright 2018 JC-Lab. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.jclab.cloud.ms3.server.spring.controller;

import kr.jclab.cloud.ms3.common.dto.BucketsListDTO;
import kr.jclab.cloud.ms3.common.dto.ListObjectsDTO;
import kr.jclab.cloud.ms3.common.dto.GenerateUriDTO;
import kr.jclab.cloud.ms3.common.dto.ResultBase;
import kr.jclab.cloud.ms3.server.spring.MS3SpringServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

@RestController
@RequestMapping(path = "/api")
public class DataControlController {
    private MS3SpringServer ms3SpringServer;

    public DataControlController(MS3SpringServer ms3SpringServer) {
        this.ms3SpringServer = ms3SpringServer;
    }

    @RequestMapping(path = "/buckets/list")
    public ResponseEntity<BucketsListDTO.Response> bucketsList() {
        BucketsListDTO.Response responseBody = new BucketsListDTO.Response();
        File dataDir = ms3SpringServer.getDataDirectory();

        for(File file : dataDir.listFiles()) {
            if(file.isDirectory()) {
                try {
                    BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                    BucketsListDTO.BucketSummary bucketSummary = new BucketsListDTO.BucketSummary();
                    bucketSummary.bucketName = file.getName();
                    bucketSummary.creationTime = attr.creationTime().toMillis();
                    bucketSummary.lastModified = file.lastModified();
                    responseBody.list.add(bucketSummary);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    @RequestMapping(path = "/buckets/create/{bucket}", method = RequestMethod.PUT)
    public ResponseEntity<ResultBase> bucketCreate(@PathVariable("bucket") String bucket) {
        ResultBase responseBody = new ResultBase();
        File bucketDir = ms3SpringServer.getBucketDirectory(bucket);
        if(bucketDir.exists()) {
            responseBody.code = HttpStatus.ACCEPTED.value();
        }else if(bucketDir.mkdir()) {
            responseBody.code = HttpStatus.OK.value();
        }else{
            responseBody.code = HttpStatus.SERVICE_UNAVAILABLE.value();
            return new ResponseEntity(responseBody, HttpStatus.SERVICE_UNAVAILABLE);
        }

        return new ResponseEntity(responseBody, HttpStatus.OK);
    }

    @RequestMapping(path = "/bucket/list/{bucket}", method = RequestMethod.GET)
    public ResponseEntity<ListObjectsDTO.Response> bucketList(@PathVariable("bucket") String bucket) {
        ListObjectsDTO.Response responseBody = new ListObjectsDTO.Response();
        File bucketDir = ms3SpringServer.getBucketDirectory(bucket);

        if(!bucketDir.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        for(File file : bucketDir.listFiles((File dir, String name) -> name.startsWith("data-"))) {
            ListObjectsDTO.ObjectSummary objectSummary = new ListObjectsDTO.ObjectSummary();
            objectSummary.bucketName = bucket;
            objectSummary.key = file.getName().substring(5);
            objectSummary.lastModified = file.lastModified();
            objectSummary.size = file.length();
            responseBody.list.add(objectSummary);
        }

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    @RequestMapping(path = "/bucket/generateuri/{bucket}/{key:.+}", method = RequestMethod.GET)
    public ResponseEntity<GenerateUriDTO.Response> generateUri(@PathVariable("bucket") String bucket, @PathVariable("key") String key) {
        GenerateUriDTO.Response responseBody = new GenerateUriDTO.Response();
        String shareid = ms3SpringServer.generateShareId(bucket, key);
        if(shareid == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        responseBody.code = HttpStatus.OK.value();
        responseBody.uri = "public/" + shareid;

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }
}
