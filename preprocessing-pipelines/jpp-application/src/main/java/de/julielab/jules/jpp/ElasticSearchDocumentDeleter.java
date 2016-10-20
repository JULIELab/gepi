package de.julielab.jules.jpp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class ElasticSearchDocumentDeleter implements IDocumentDeleter {
	private static final Logger log = LoggerFactory.getLogger(ElasticSearchDocumentDeleter.class);

	public static final String CLUSTER = "clusterName";
	public static final String HOST = "host";
	public static final String PORT = "port";

	public static final String TO_DELETE_QUEUE = "elasticSearchDocumentDeletionQueue.lst";
	
	private TransportClient client;

	public ElasticSearchDocumentDeleter(String configFile) {
		try {
			Properties config = new Properties();
			config.load(new FileInputStream(configFile));
			String clusterName = config.getProperty(CLUSTER);
			String host = config.getProperty(HOST);
			int port = Integer.parseInt(config.getProperty(PORT));
			Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build();
			client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(host, port));
		} catch (IOException e) {
			log.error("Could not read ElasticSearch socket data from properties file {} and thus could not set up ElasticSearch client. ", configFile);
			e.printStackTrace();
		}
	}

	@Override
	public void deleteDocuments(List<String> docIds) {
		try {
			String index = "medline";
			String type = "docs";
			BulkRequestBuilder bulkRequest = client.prepareBulk();
			for (String id : docIds)
				bulkRequest.add(client.prepareDelete(index, type, id));
			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				for (int i = 0; i < bulkResponse.getItems().length; i++) {
					BulkItemResponse response = bulkResponse.getItems()[i];
					if (response.isFailed())
						log.error("Delete fail message: {}", response.getFailureMessage());
				}
			} else {
				log.info("Successfully deleted {} documents from ElasticSearch.", docIds.size());
			}
		} catch (Exception e) {
			log.error("Exception occurred while trying to delete documents from ElasticSearch. Document IDs that should have been deleted are stored in file {}.", TO_DELETE_QUEUE);
			try {
				FileUtils.writeLines(new File(TO_DELETE_QUEUE), "UTF-8", docIds, "\n", true);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
//	public static void main(String[] args) {
//		IDocumentDeleter deleter = new ElasticSearchDocumentDeleter("conf/elasticsearch.properties");
//		deleter.deleteDocuments(Lists.newArrayList("15369677", "19966271", "14701835"));
//		deleter.deleteDocuments(Lists.newArrayList("15369677", "19966271", "14701835"));
//	}
}
