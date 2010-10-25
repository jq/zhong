/*
 * Copyright 2010 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * This sample demonstrates how to make basic requests to Amazon S3 using
 * the AWS SDK for Java.
 * <p>
 * <b>Prerequisites:</b> You must have a valid Amazon Web Services developer
 * account, and be signed up to use Amazon S3. For more information on
 * Amazon S3, see http://aws.amazon.com/s3.
 * <p>
 * <b>Important:</b> Be sure to fill in your AWS access credentials in the
 *                   AwsCredentials.properties file before you try to run this
 *                   sample.
 * http://aws.amazon.com/security-credentials
 */
public class S3Sample {
	
	
	public static void main(String[] args) throws IOException
	{
		// s3
		String imageBucketName = "ringtone_image";
		String ringBucketName = "ringtone_ring";
		AmazonS3 s3 = new AmazonS3Client(new PropertiesCredentials(
				S3Sample.class.getResourceAsStream("AwsCredentials.properties")));
		String bucketName;
		System.out.println("Start Sending Files To Amazon S3");
		
		// directory and files
		String pathName = "/home/liutao/workspace/python/1-fetch/download_Holiday/";
		File dir = new File(pathName);
		File list[] =  dir.listFiles();
		String fileName;
		
		// sort by record index
		Arrays.sort(list, new Comparator<File>() {
			public int compare(File f1, File f2)
			{
				String s1 = f1.getName();
				String s2 = f2.getName();
				int idx1 = s1.indexOf(".xml");
				int idx2 = s2.indexOf(".xml");
				if(idx1!=-1 && idx2!=-1)
				{
					int num1 = Integer.parseInt(s1.substring(s1.indexOf('d')+1, idx1));
					int num2 = Integer.parseInt(s2.substring(s2.indexOf('d')+1, idx2));
					return num1 - num2;
				}
				else if(idx1 == -1)
					return -1;
				else 
					return 1;
			}
		});
		
		
		// xml parser
		DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
		InputStream   in;
		
		// 
		String uuid;
		String key;
		URL  url;
		int i=0,j;
		
		
		try 
		{
			DocumentBuilder docBuilder=factory.newDocumentBuilder();
			FileWriter log = new FileWriter(pathName+"log");
			
			while(i<list.length)  
			{
				if(list[i].getName().endsWith(".xml"))	break;
				i ++;
			}
			// start process xml file
			for(; i<list.length; i++)
			{
					System.out.println(list[i].getName());	
					log.write(list[i].getName()+",");
					try 
					{
						in = new FileInputStream(pathName+list[i].getName());
						Document  doc  =  docBuilder.parse(in);
						org.w3c.dom.Element  root = doc.getDocumentElement();
						//System.out.println(root.getNodeName());
						NodeList childen = root.getChildNodes();
						org.w3c.dom.Node curNode,node;
						
						uuid =  UUID.randomUUID().toString();
						
						for(j=0; j<childen.getLength(); j++)
						{
							curNode = childen.item(j);
							if(curNode.getNodeType() == Node.ELEMENT_NODE)
								if(curNode.getNodeName().equals("Ring") || curNode.getNodeName().equals("Image"))
								{ 
									//System.out.println(curNode.getFirstChild().getNodeValue());
									fileName = curNode.getFirstChild().getNodeValue();
									File file = new File(pathName+fileName);
									if(!file.exists())  
									{
										log.write(fileName+" not Found!\n");
										break ;
									}
									
									key = uuid + fileName;
									//bucketName = curNode.getNodeName().equals("Ring")?ringBucketName:imageBucketName;
									bucketName = "ringtone_test_2010";
									try
									{
										s3.putObject(new PutObjectRequest(bucketName, key, file));      		// 上传文件    	
										s3.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);	// 设置权限
										// url = s3.generatePresignedUrl(bucketName, key, expireDate);
										// System.out.println(url);
									}
									catch (AmazonServiceException ase) 
									{
										log.write("AmazonServiceException\n");
									}
									catch (AmazonClientException ace) 
									{
										log.write("AmazonClientException\n");		
									}
								}
						}
						if(j == childen.getLength())	
						{// if success, record it
							log.write("uuid:"+uuid+"\n");
						}
					}
					catch (FileNotFoundException e) 
					{
						log.write("FileNotFound\n");
					}
					catch (SAXException e) 
					{
						log.write("xml parse error\n");
					}
					catch (IOException e) 
					{
						log.write("IOexception\n");
					}
					 
				}	
				
			log.flush();
			log.close();
		} catch (ParserConfigurationException e) 
		{
			e.printStackTrace();
		}
		
		System.out.println("Sending Finished!");
	
	}
	
	
	
	
    public static void main2(String[] args) throws IOException {
        /*
         * Important: Be sure to fill in your AWS access credentials in the
         *            AwsCredentials.properties file before you try to run this
         *            sample.
         * http://aws.amazon.com/security-credentials
         */
        AmazonS3 s3 = new AmazonS3Client(new PropertiesCredentials(
                S3Sample.class.getResourceAsStream("AwsCredentials.properties")));

        //String bucketName = "my-first-s3-bucket-" + UUID.randomUUID();
        String bucketName = "ringtone_image";
        String key = "test";
        
        System.out.println("being...");
        System.out.println("size:");        
        System.out.println("end.\n");

        try {
            /*
             * Create a new S3 bucket - Amazon S3 bucket names are globally unique,
             * so once a bucket name has been taken by any user, you can't create
             * another bucket with that same name.
             *
             * You can optionally specify a location for your bucket if you want to
             * keep your data closer to your applications or users.
            
            System.out.println("Creating bucket " + bucketName + "\n");
            s3.createBucket(bucketName);
             */
        	
        	
            /*
             * List the buckets in your account
             
            System.out.println("Listing buckets");
            for (Bucket bucket : s3.listBuckets()) {
                System.out.println(" - " + bucket.getName());
            }
            System.out.println();
             */
        	
        	
        	
            /*
             * Upload an object to your bucket - You can easily upload a file to
             * S3, or upload directly an InputStream if you know the length of
             * the data in the stream. You can also specify your own metadata
             * when uploading to S3, which allows you set a variety of options
             * like content-type and content-encoding, plus additional metadata
             * specific to your applications.
             	
        	System.out.println("Uploading a new object to S3 from a file\n");
    		s3.putObject(new PutObjectRequest(bucketName, key, createSampleFile()));         
    		 */  
        	
        	
            /*
             * Download an object - When you download an object, you get all of
             * the object's metadata and a stream from which to read the contents.
             * It's important to read the contents of the stream as quickly as
             * possibly since the data is streamed directly from Amazon S3 and your
             * network connection will remain open until you read all the data or
             * close the input stream.
             *
             * GetObjectRequest also supports several other options, including
             * conditional downloading of objects based on modification times,
             * ETags, and selectively downloading a range of an object.
            
    
            System.out.println("Downloading an object");
            S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
            System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
            displayTextInputStream(object.getObjectContent());
             */
        	
        	
            /*
             * List objects in your bucket by prefix - There are many options for
             * listing the objects in your bucket.  Keep in mind that buckets with
             * many objects might truncate their results when listing their objects,
             * so be sure to check if the returned object listing is truncated, and
             * use the AmazonS3.listNextBatchOfObjects(...) operation to retrieve
             * additional results.
     
           
            
    		System.out.println("Listing objects");
          
    		ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                    .withBucketName(bucketName)
                    .withPrefix("My"));
          
    	
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                System.out.println(" - " + objectSummary.getKey() + "  " +
                                   "(size = " + objectSummary.getSize() + ")");
            	
            }
         */
             
        	
        	
            /*
             * Delete an object - Unless versioning has been turned on for your bucket,
             * there is no way to undelete an object, so use caution when deleting objects.
           
    
            System.out.println("Deleting an object\n");
            s3.deleteObject(bucketName, key);
             */
        	
        	
            /*
             * Delete a bucket - A bucket must be completely empty before it can be
             * deleted, so remember to delete any objects from your buckets before
             * you try to delete them.
           
   
            System.out.println("Deleting bucket " + bucketName + "\n");
            s3.deleteBucket(bucketName);
             */  
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    /**
     * Creates a temporary file with text data to demonstrate uploading a file
     * to Amazon S3
     *
     * @return A newly created temporary file with text data.
     *
     * @throws IOException
     */
    private static File createSampleFile() throws IOException {
        File file = File.createTempFile("aws-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.write("01234567890112345678901234\n");
        writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
        writer.write("01234567890112345678901234\n");
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.close();

        return file;
    }

    /**
     * Displays the contents of the specified input stream as text.
     *
     * @param input
     *            The input stream to display as text.
     *
     * @throws IOException
     */
    private static void displayTextInputStream(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null) break;

            System.out.println("    " + line);
        }
        System.out.println();
    }

}
