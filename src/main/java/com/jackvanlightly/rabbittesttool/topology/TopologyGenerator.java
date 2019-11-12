package com.jackvanlightly.rabbittesttool.topology;

import com.jackvanlightly.rabbittesttool.BrokerConfiguration;
import com.jackvanlightly.rabbittesttool.clients.ConnectionSettings;
import com.jackvanlightly.rabbittesttool.topology.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class TopologyGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyGenerator.class);
    private ConnectionSettings connectionSettings;
    private BrokerConfiguration brokerConfig;
    private String baseUrl;
    private Random rand;

    public TopologyGenerator(ConnectionSettings connectionSettings,
                             BrokerConfiguration brokerConfig) {
        this.connectionSettings = connectionSettings;
        this.brokerConfig = brokerConfig;
        this.baseUrl = "http://" + connectionSettings.getHostOnly() + ":" + connectionSettings.getManagementPort();
        this.rand = new Random();
    }

    public void declareVHost(VirtualHost vhost) {
        String vhostUrl = getVHostUrl(vhost.getName());
        delete(vhostUrl, true);
        put(vhostUrl, "{}");

        String permissionsJson = "{\"configure\":\".*\",\"write\":\".*\",\"read\":\".*\"}";
        put(getVHostUserPermissionsUrl(vhost.getName(), connectionSettings.getUser()), permissionsJson);

        LOGGER.info("Added vhost " + vhost.getName()+ " and added permissions to user " + connectionSettings.getUser());
    }

    public void deleteVHost(VirtualHost vhost) {
        String vhostUrl = getVHostUrl(vhost.getName());
        delete(vhostUrl, false);
    }

    public void declareExchanges(VirtualHost vhost) {
        for(ExchangeConfig exchangeConfig : vhost.getExchanges())
            declareExchange(exchangeConfig);

        for(ExchangeConfig exchangeConfig : vhost.getExchanges())
            declareExchangeBindings(exchangeConfig);
    }

    private void declareExchange(ExchangeConfig exchangeConfig) {
        String exchangeTemplate = "{\"type\":\"[ex]\",\"auto_delete\":false,\"durable\":true,\"internal\":false,\"arguments\":{}}";
        String exchangeJson = exchangeTemplate.replace("[ex]", exchangeConfig.getExchangeTypeName());
        put(getExchangeUrl(exchangeConfig.getVhostName(), exchangeConfig.getName()), exchangeJson);
    }

    private void declareExchangeBindings(ExchangeConfig exchangeConfig) {
        for (BindingConfig bindingConfig : exchangeConfig.getBindings()) {
            JSONObject binding = new JSONObject();

            if (bindingConfig.getBindingKey() != null || StringUtils.isEmpty(bindingConfig.getBindingKey()))
                binding.put("routing_key", bindingConfig.getBindingKey());

            if(!bindingConfig.getProperties().isEmpty()) {
                JSONObject properties = new JSONObject();
                for (Property p : bindingConfig.getProperties()) {
                    properties.put(p.getKey(), p.getValue());
                }
                binding.put("arguments", properties);
            }

            String bindingJson = binding.toString();

            post(getExchangeToExchangeBindingUrl(exchangeConfig.getVhostName(), bindingConfig.getFrom(), exchangeConfig.getName()), bindingJson);
        }
    }

    public void declareQueuesAndBindings(QueueConfig queueConfig) {
        int nodeIndex = rand.nextInt(brokerConfig.getNodes().size());
        for(int i = 1; i<= queueConfig.getScale(); i++) {
            declareQueue(queueConfig, i, nodeIndex);
            declareQueueBindings(queueConfig, i);

            nodeIndex++;
            if(nodeIndex >= brokerConfig.getNodes().size())
                nodeIndex = 0;
        }
    }

    public void declareQueue(QueueConfig queueConfig, int ordinal, int nodeIndex) {
        //TODO queue properties
        //String queueTemplate = "{\"auto_delete\":false,\"durable\":true,\"arguments\":{},\"node\":\"rabbit@rabbitmq" + node + "\"}";

        JSONObject arguments = new JSONObject();

        if(queueConfig.getProperties() != null && !queueConfig.getProperties().isEmpty()) {
            for(Property prop : queueConfig.getProperties()) {
                if(!prop.getKey().startsWith("ha-"))
                    arguments.put(prop.getKey(), prop.getValue());
            }
        }

        String queueName = queueConfig.getQueueName(ordinal);

        JSONObject queue = new JSONObject();
        queue.put("auto_delete", false);
        queue.put("durable", true);
        queue.put("node", brokerConfig.getNodes().get(nodeIndex));
        queue.put("arguments", arguments);

        put(getQueueUrl(queueConfig.getVhostName(), queueName), queue.toString());
        QueueHosts.register(queueConfig.getVhostName(), queueName, connectionSettings.getHostAndPort(nodeIndex));

        if(queueConfig.getProperties().stream().anyMatch(x -> x.getKey().equals("ha-mode"))) {
            JSONObject policyJson = new JSONObject();
            policyJson.put("pattern", queueConfig.getQueueName(ordinal));
            policyJson.put("priority", 0);
            policyJson.put("apply-to", "queues");

            JSONObject definition = new JSONObject();

            if(queueConfig.getProperties() != null && !queueConfig.getProperties().isEmpty()) {
                for(Property prop : queueConfig.getProperties()) {
                    if(prop.getKey().startsWith("ha-"))
                        definition.put(prop.getKey(), prop.getValue());
                }
            }
            policyJson.put("definition", definition);
            put(getHaQueuesPolicyUrl(queueName, queueConfig.getVhostName()), policyJson.toString());
        }
    }

    public void declareQueueBindings(QueueConfig queueConfig, int ordinal) {
        for (BindingConfig bindingConfig : queueConfig.getBindings()) {
            JSONObject binding = new JSONObject();

            if (bindingConfig.getBindingKey() != null && !StringUtils.isEmpty(bindingConfig.getBindingKey()))
                binding.put("routing_key", bindingConfig.getBindingKey());
            else
                binding.put("routing_key", "");

            if(!bindingConfig.getProperties().isEmpty()) {
                JSONObject properties = new JSONObject();
                for (Property p : bindingConfig.getProperties()) {
                    properties.put(p.getKey(), p.getValue());
                }
                binding.put("arguments", properties);
            }

            String bindingJson = binding.toString();

            post(getExchangeToQueueBindingUrl(queueConfig.getVhostName(), bindingConfig.getFrom(), queueConfig.getQueueName(ordinal)), bindingJson);
        }
    }

    public void declarePolicies(String vhostName, List<Policy> policies) {
        for(Policy policy : policies) {
            JSONObject policyJson = new JSONObject();
            policyJson.put("pattern", policy.getPattern());
            policyJson.put("priority", policy.getPriority());
            policyJson.put("apply-to", policy.getApplyTo());

            JSONObject definition = new JSONObject();

            for (Property prop : policy.getProperties()) {
                definition.put(prop.getKey(), prop.getValue());
            }

            policyJson.put("definition", definition);

            put(getHaQueuesPolicyUrl(policy.getName(), vhostName), policyJson.toString());
        }
    }

    private void post(String url, String json) {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(url);

            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            UsernamePasswordCredentials creds
                    = new UsernamePasswordCredentials(connectionSettings.getUser(), connectionSettings.getPassword());
            httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));

            CloseableHttpResponse response = client.execute(httpPost);
            int responseCode = response.getStatusLine().getStatusCode();
            client.close();

            if(responseCode != 201 && responseCode != 204)
                throw new TopologyException("Received a non success response code executing POST " + url
                        + " Code:" + responseCode
                        + " Response: " + response.toString());
        }
        catch(Exception e) {
            throw new TopologyException("An exception occurred executing POST " + url, e);
        }
    }

    private void put(String url, String json) {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPut httpPut = new HttpPut(url);

            StringEntity entity = new StringEntity(json);
            httpPut.setEntity(entity);
            httpPut.setHeader("Accept", "application/json");
            httpPut.setHeader("Content-type", "application/json");
            UsernamePasswordCredentials creds
                    = new UsernamePasswordCredentials(connectionSettings.getUser(), connectionSettings.getPassword());
            httpPut.addHeader(new BasicScheme().authenticate(creds, httpPut, null));

            CloseableHttpResponse response = client.execute(httpPut);
            int responseCode = response.getStatusLine().getStatusCode();
            client.close();

            if(responseCode != 201 && responseCode != 204)
                throw new TopologyException("Received a non success response code executing PUT " + url
                        + " Code:" + responseCode
                        + " Response: " + response.toString());
        }
        catch(Exception e) {
            throw new TopologyException("An exception occurred executing PUT " + url, e);
        }
    }

    private void delete(String url, boolean allow404) {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpDelete httpDelete = new HttpDelete(url);

            UsernamePasswordCredentials creds
                    = new UsernamePasswordCredentials(connectionSettings.getUser(), connectionSettings.getPassword());
            httpDelete.addHeader(new BasicScheme().authenticate(creds, httpDelete, null));

            CloseableHttpResponse response = client.execute(httpDelete);
            int responseCode = response.getStatusLine().getStatusCode();
            client.close();

            if(responseCode != 200 && responseCode != 204) {
                if(responseCode == 404 && allow404)
                    return;

                throw new TopologyException("Received a non success response code executing DELETE " + url
                        + " Code:" + responseCode
                        + " Response: " + response.toString());
            }
        }
        catch(Exception e) {
            throw new TopologyException("An exception occurred executing DELETE " + url, e);
        }
    }

    private String getVHostUrl(String vhost) {
        return this.baseUrl + "/api/vhosts/" + vhost;
    }

    private String getVHostUserPermissionsUrl(String vhost, String user) {
        return this.baseUrl + "/api/permissions/"+ vhost + "/" + user;
    }

    private String getExchangeUrl(String vhost, String exchange) {
        return this.baseUrl + "/api/exchanges/" + vhost + "/" + exchange;
    }

    private String getQueueUrl(String vhost, String queue) {
        return this.baseUrl + "/api/queues/" + vhost + "/" + queue;
    }

    private String getExchangeToQueueBindingUrl(String vhost, String from, String to) {
        return this.baseUrl + "/api/bindings/" + vhost + "/e/" + from + "/q/" + to;
    }

    private String getExchangeToExchangeBindingUrl(String vhost, String from, String to) {
        return this.baseUrl + "/api/bindings/" + vhost + "/e/" + from + "/e/" + to;
    }

    private String getHaQueuesPolicyUrl(String name, String vhost) {
        return this.baseUrl + "/api/policies/" + vhost + "/" + name;
    }
}
