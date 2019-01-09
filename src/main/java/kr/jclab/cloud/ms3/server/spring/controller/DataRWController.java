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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.jclab.cloud.ms3.common.dto.PutObjectDTO;
import kr.jclab.cloud.ms3.common.dto.ResultBase;
import kr.jclab.cloud.ms3.common.model.ObjectMetadata;
import kr.jclab.cloud.ms3.server.spring.MS3SpringServer;
import kr.jclab.cloud.ms3.server.spring.object.MS3ObjectFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.*;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Controller
@RequestMapping(path = "/api")
public class DataRWController {
    private MS3SpringServer ms3SpringServer;

    public DataRWController(MS3SpringServer ms3SpringServer) {
        this.ms3SpringServer = ms3SpringServer;
    }

    @RequestMapping(path =  "/bucket/data/{bucket}/{key:.+}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> getData(@PathVariable("bucket") String bucket, @PathVariable("key") String key) {
        MS3ObjectFile objectFile = ms3SpringServer.getMS3Object(bucket, key);
        if(objectFile == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        if(!objectFile.dataFile.exists())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        try {
            ObjectMetadata objectMetadata = ms3SpringServer.getObjectMetadata(objectFile);
            HttpHeaders httpHeaders = new HttpHeaders();
            if(objectMetadata != null) {
                String contentType = objectMetadata.getContentType();
                String contentEncoding = objectMetadata.getContentEncoding();
                if(contentType != null)
                    httpHeaders.set("Content-Type", contentType);
                if(contentEncoding != null)
                    httpHeaders.set("Content-Encoding", contentEncoding);
            }
            return new ResponseEntity<>(new InputStreamResource(new FileInputStream(objectFile.dataFile)), httpHeaders, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @RequestMapping(path =  "/bucket/data/{bucket}/{key:.+}", method = RequestMethod.PUT)
    public ResponseEntity<ResultBase> putData(HttpServletRequest request, @PathVariable("bucket") String bucket, @PathVariable("key") String key, InputStream dataStream) {
        ResultBase result = new ResultBase();
        ObjectMapper objectMapper = new ObjectMapper();
        MS3ObjectFile objectFile = ms3SpringServer.getMS3Object(bucket, key);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        FileOutputStream fosMeta = null;
        FileOutputStream fosData = null;

        if(objectFile == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        try {
            byte[] buffer = new byte[16 * 1048576];
            int readlen;

            String contentType = request.getContentType();
            String contentEncoding = request.getHeader("content-encoding");
            if(contentType != null)
                objectMetadata.setContentType(contentType);
            if(contentEncoding != null)
                objectMetadata.setContentEncoding(contentEncoding);

            fosMeta = new FileOutputStream(objectFile.metadataFile);
            objectMapper.writeValue(fosMeta, objectMetadata);

            fosData = new FileOutputStream(objectFile.dataFile);
            while(dataStream.available() > 0 && (readlen = dataStream.read(buffer)) > 0) {
                fosData.write(buffer, 0, readlen);
            }

            result.code = HttpStatus.OK.value();
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fosMeta != null) {
                try { fosMeta.close(); }catch (IOException e) {}
            }
            if(fosData != null) {
                try { fosData.close(); }catch (IOException e) {}
            }
        }
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @RequestMapping(path =  "/bucket/object/{bucket}/{key:.+}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> getObject(@PathVariable("bucket") String bucket, @PathVariable("key") String key) {
        MS3ObjectFile objectFile = ms3SpringServer.getMS3Object(bucket, key);
        if(objectFile == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        if(!objectFile.dataFile.exists())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        try {
            byte[] metadata = null;
            int metadataSize = 0;
            HttpHeaders httpHeaders = new HttpHeaders();

            if(objectFile.metadataFile.exists()) {
                ObjectMapper objectMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
                ObjectMetadata objectMetadata = objectMapper.readValue(objectFile.metadataFile, ObjectMetadata.class);
                metadata = objectMapper.writeValueAsBytes(objectMetadata);
                metadataSize = metadata.length;
            }
            httpHeaders.set("MS3-METADATA-SIZE", String.valueOf(metadataSize));
            return new ResponseEntity<>(new InputStreamResource(new ObjectResponseInputStream(metadata, new FileInputStream(objectFile.dataFile))), httpHeaders, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @RequestMapping(path =  "/bucket/object/{bucket}/{key:.+}", method = RequestMethod.PUT)
    public ResponseEntity<PutObjectDTO.Response> putObject(@PathVariable("bucket") String bucket, @PathVariable("key") String key, HttpServletRequest request, InputStream dataStream) {
        PutObjectDTO.Response responseBody = new PutObjectDTO.Response();
        MS3ObjectFile objectFile = ms3SpringServer.getMS3Object(bucket, key);
        boolean isNewFile = !objectFile.dataFile.exists();
        do {
            int metadataSize;
            byte[] metadataBin = null;
            FileOutputStream metadataOutputStream = null;
            FileOutputStream contentOutputStream = null;
            try {
                metadataSize = Integer.parseInt(request.getHeader("MS3-METADATA-SIZE"));
            } catch(NumberFormatException e) {
                responseBody.code = HttpStatus.FORBIDDEN.value();
                responseBody.message = "No MS3-METADATA-SIZE header";
                break;
            }
            if (isNewFile) {
                try {
                    if (!objectFile.dataFile.createNewFile()) {
                        responseBody.code = HttpStatus.SERVICE_UNAVAILABLE.value();
                        responseBody.message = "create file failed";
                        break;
                    }
                } catch (IOException e) {
                    responseBody.code = HttpStatus.SERVICE_UNAVAILABLE.value();
                    responseBody.message = e.getMessage();
                    break;
                }
            }
            try {
                if (metadataSize > 0) {
                    int readlen;
                    int pos = 0;
                    byte[] buffer = new byte[metadataSize];
                    metadataOutputStream = new FileOutputStream(objectFile.metadataFile);
                    while ((pos < metadataSize) && (readlen = dataStream.read(buffer, 0, metadataSize - pos)) > 0) {
                        pos += readlen;
                        metadataOutputStream.write(buffer, 0, readlen);
                    }
                    if(pos != metadataSize) {
                        responseBody.code = HttpStatus.SERVICE_UNAVAILABLE.value();
                        responseBody.message = "metadata read failed";
                        break;
                    }
                }
                {
                    int readlen;
                    byte[] buffer = new byte[1048576];
                    contentOutputStream = new FileOutputStream(objectFile.dataFile);
                    while ((readlen = dataStream.read(buffer)) > 0) {
                        contentOutputStream.write(buffer, 0, readlen);
                    }
                }
            }catch(IOException e) {
                responseBody.code = HttpStatus.SERVICE_UNAVAILABLE.value();
                responseBody.message = e.getMessage();
                break;
            } finally {
                if(metadataOutputStream != null) {
                    try { metadataOutputStream.close(); } catch (IOException e) { }
                }
                if(contentOutputStream != null) {
                    try { contentOutputStream.close(); } catch (IOException e) { }
                }
            }

            responseBody.code = HttpStatus.OK.value();
        } while (false);

        if(responseBody.code != HttpStatus.OK.value()) {
            if(isNewFile) {
                if (objectFile.dataFile.exists())
                    objectFile.dataFile.delete();
                if (objectFile.metadataFile.exists())
                    objectFile.metadataFile.delete();
            }
            return new ResponseEntity<>(responseBody, HttpStatus.SERVICE_UNAVAILABLE);
        }

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    @RequestMapping(path =  "/bucket/metadata/{bucket}/{key:.+}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> getMetadata(@PathVariable("bucket") String bucket, @PathVariable("key") String key) {
        MS3ObjectFile objectFile = ms3SpringServer.getMS3Object(bucket, key);
        if(objectFile == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        if(!objectFile.dataFile.exists())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        if(!objectFile.metadataFile.exists())
            return new ResponseEntity<>(HttpStatus.OK);
        try {
            return new ResponseEntity<>(new InputStreamResource(new FileInputStream(objectFile.metadataFile)), HttpStatus.OK);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    public static class ObjectResponseInputStream extends InputStream {
        private byte[] metadata;
        private InputStream in;
        private int metadataRemaining;

        public ObjectResponseInputStream(byte[] metadata, InputStream in) {
            this.metadata = metadata;
            this.in = in;
            if(this.metadata != null)
                this.metadataRemaining = this.metadata.length;
            else
                this.metadataRemaining = 0;
        }

        @Override
        public int available() throws IOException {
            return this.metadataRemaining + this.in.available();
        }

        @Override
        public void close() throws IOException {
            this.in.close();
        }

        @Override
        public int read() throws IOException {
            if(this.metadataRemaining > 0) {
                int pos = this.metadata.length - this.metadataRemaining;
                this.metadataRemaining--;
                return this.metadata[pos];
            }
            return this.in.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return this.read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int procoff = off;
            int processedsize = 0;
            int remainsize = len;
            if(this.metadataRemaining > 0) {
                int availlen = this.metadataRemaining > remainsize ? remainsize : this.metadataRemaining;
                int metapos = this.metadata.length - this.metadataRemaining;
                System.arraycopy(this.metadata, metapos, b, procoff, availlen);
                this.metadataRemaining -= availlen;
                remainsize -= availlen;
                processedsize = availlen;
                procoff += availlen;
            }
            if(remainsize > 0) {
                byte[] buffer = new byte[remainsize];
                int tmp = in.read(buffer);
                if(tmp > 0) {
                    System.arraycopy(buffer, 0, b, procoff, remainsize);
                    processedsize += tmp;
                }else if(tmp < 0 && processedsize == 0) {
                    return tmp;
                }
            }
            return processedsize;
        }
    }
}
