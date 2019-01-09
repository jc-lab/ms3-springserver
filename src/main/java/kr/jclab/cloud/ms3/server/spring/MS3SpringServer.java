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
package kr.jclab.cloud.ms3.server.spring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.jclab.cloud.ms3.common.model.ObjectMetadata;
import kr.jclab.cloud.ms3.server.spring.controller.DataControlController;
import kr.jclab.cloud.ms3.server.spring.controller.DataRWController;
import kr.jclab.cloud.ms3.server.spring.controller.PublicController;
import kr.jclab.cloud.ms3.server.spring.object.MS3ObjectFile;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.Properties;
import java.util.UUID;

public class MS3SpringServer {

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Could not find a org.sqlite.JDBC class");
        }
    }

    @Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private MS3SpringServerConfigurerAdapter configurerAdapter;

    private File resourceDirectory;

    private Connection dbConnection;

    MS3SpringServer(MS3SpringServerConfigurerAdapter configurerAdapter) {
        this.configurerAdapter = configurerAdapter;
    }

    @PostConstruct
    protected void init() throws SQLException, MalformedURLException {
        this.configurerAdapter.configure(this);
        File dbFile = new File(this.resourceDirectory, "database.db");
        File dataDir = new File(this.resourceDirectory, "data");
        if(!dataDir.exists()) {
            dataDir.mkdirs();
        }

        Statement statement;
        this.dbConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        statement = this.dbConnection.createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS `share_object` (" +
                "`shareid` VARCHAR(40) PRIMARY KEY," +
                "`created_at` BIGINT, " +
                "`bucket` VARCHAR(256)," +
                "`key` VARCHAR(512), " +
                "UNIQUE (`bucket`, `key`)" +
                ")");

    }

    public void setResourceDirectory(File resourceDirectory) {
        this.resourceDirectory = resourceDirectory;
    }

    /**
     *
     * @param urlPrefix
     * @param order
     */
    public void registerRequestMapping(String urlPrefix, Integer order) {
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) webAppContext).getBeanFactory();

        Properties urlProperties = new Properties();
        Object controllers[] = {new DataControlController(this), new DataRWController(this), new PublicController(this)};

        for(Object controller : controllers) {
            RequestMapping classRequestMapping = controller.getClass().getAnnotation(RequestMapping.class);
            String beanName = controller.getClass().getCanonicalName(); //"ms3springserver_" + controller.getClass().getName().toLowerCase();
            beanFactory.autowireBean(controller);
            beanFactory.initializeBean(controller, beanName);
            beanFactory.registerSingleton(beanName, controller);
            for(Method method : controller.getClass().getMethods()) {
                RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
                if(methodRequestMapping != null) {
                    StringBuilder url = new StringBuilder(urlPrefix);
                    url.append(classRequestMapping.path()[0]);
                    url.append(methodRequestMapping.path()[0]);

                    RequestMappingInfo requestMappingInfo = RequestMappingInfo
                            .paths(url.toString())
                            .methods(methodRequestMapping.method())
                            .produces(methodRequestMapping.produces())
                            .headers(methodRequestMapping.headers())
                            .consumes(methodRequestMapping.consumes())
                            .build();
                    requestMappingHandlerMapping.registerMapping(requestMappingInfo, controller, method);
                }
            }
        }
    }

    public File getDataDirectory() {
        File dataDir = new File(resourceDirectory, "data/");
        return dataDir;
    }

    public File getBucketDirectory(String bucket) {
        File bucketDir = new File(this.resourceDirectory, "data/" + bucket);
        return bucketDir;
    }

    public boolean createBucket(String bucket) {
        File bucketDir = getBucketDirectory(bucket);
        return bucketDir.mkdirs();
    }

    public boolean deleteBucket(String bucket) {
        File bucketDir = getBucketDirectory(bucket);
        if(!bucketDir.exists()) {
            return true;
        }
        try {
            FileUtils.deleteDirectory(bucketDir);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public MS3ObjectFile getMS3Object(String bucket, String key) {
        MS3ObjectFile objectFile = new MS3ObjectFile(this.resourceDirectory, bucket, key);
        return objectFile;
    }

    public MS3ObjectFile findMS3ObjectByShareid(String shareid) {
        try {
            PreparedStatement statment = this.dbConnection.prepareStatement("SELECT `bucket`,`key` FROM share_object WHERE shareid=?");
            statment.setString(1, shareid);
            ResultSet resultSet = statment.executeQuery();
            if(resultSet.next()) {
                String bucket = resultSet.getString(1);
                String key = resultSet.getString(2);
                MS3ObjectFile objectFile = new MS3ObjectFile(this.resourceDirectory, bucket, key);
                if(objectFile.dataFile.exists())
                {
                    return objectFile;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ObjectMetadata getObjectMetadata(MS3ObjectFile ms3ObjectFile) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ObjectMetadata objectMetadata = null;
        if(!ms3ObjectFile.metadataFile.exists())
            return null;
        try {
            objectMetadata = objectMapper.readValue(ms3ObjectFile.metadataFile, ObjectMetadata.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return objectMetadata;
    }

    public String generateShareId(String bucket, String key) {
        synchronized (this.dbConnection) {
            String shareid = UUID.randomUUID().toString();
            PreparedStatement statement = null;
            try {
                statement = this.dbConnection.prepareStatement("INSERT INTO `share_object` (`shareid`, `created_at`, `bucket`, `key`) VALUES (?, ?, ?, ?)");
                statement.setString(1, shareid);
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, bucket);
                statement.setString(4, key);
                statement.execute();
            } catch(SQLException e) {
                shareid = null;
            } finally {
                if(statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                    statement = null;
                }
            }
            if(shareid == null) {
                try {
                    statement = this.dbConnection.prepareStatement("SELECT `shareid` FROM `share_object` WHERE `bucket`=? AND `key`=?");
                    statement.setString(1, bucket);
                    statement.setString(2, key);
                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        shareid = resultSet.getString(1);
                    }
                }catch(SQLException e) {
                    e.printStackTrace();
                } finally {
                    if(statement != null) {
                        try {
                            statement.close();
                        } catch (SQLException e) {
                        }
                        statement= null;
                    }
                }
            }
            return shareid;
        }

    }
}
