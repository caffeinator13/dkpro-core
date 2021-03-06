/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.dkpro.core.fs.hdfs;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.OutputStreamWriter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class HdfsResourceLoaderLocatorTest
{
    @Test
    public void testExternalLoaderLocator()
        throws Exception
    {
        String document = "This is a test.";
        
        // Start dummy HDFS
        File target = testContext.getTestOutputFolder();
        File baseDir = new File(target, "hdfs").getAbsoluteFile();
        FileUtil.fullyDelete(baseDir);
        Configuration conf = new Configuration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
        MiniDFSCluster hdfsCluster = builder.build();
        String hdfsURI = "hdfs://localhost:"+ hdfsCluster.getNameNodePort() + "/";
        
        // Write test document
        hdfsCluster.getFileSystem().mkdirs(new Path("/user/test"));
        try (OutputStreamWriter os = new OutputStreamWriter(
                hdfsCluster.getFileSystem().create(new Path("/user/test/file.txt")), "UTF-8")) {
            os.write(document);
        }
        
        // Set up HDFS resource locator
        ExternalResourceDescription locator = createExternalResourceDescription(
                HdfsResourceLoaderLocator.class,
                HdfsResourceLoaderLocator.PARAM_FILESYSTEM, hdfsURI);
                //"hdfs://node-00a.ukp.informatik.tu-darmstadt.de:8020");
        
        // Configure reader to read from HDFS
        CollectionReader reader = createReader(
                TextReader.class,
                TextReader.PARAM_SOURCE_LOCATION, "hdfs:/user/test",
                        TextReader.PARAM_PATTERNS, "file.txt",
                        TextReader.KEY_RESOURCE_RESOLVER, locator);
        
        // Read data
        JCas cas = JCasFactory.createJCas();
        reader.getNext(cas.getCas());
        
        // Verify content
        assertEquals(document, cas.getDocumentText());
    }
    
    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
