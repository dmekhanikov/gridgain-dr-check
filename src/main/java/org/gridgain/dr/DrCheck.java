package org.gridgain.dr;

import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.Query;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.yaml.snakeyaml.Yaml;

import javax.cache.Cache;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DrCheck {
    private static final String DATA_CENTERS = "data-centers";
    private static final String CACHES = "caches";

    public static void main(String[] args) throws CacheContentsMismatchException, IOException {
        if (args.length != 1) {
            System.err.println("Invalid number of arguments: " + args.length +
                    "\n\tUsage: " + "java -jar dr-check.jar <PATH TO YAML CONFIG>");

            System.exit(1);
        }

        String configPath = args[0];

        Map<String, Object> config = loadConfiguration(configPath);

        List<String> dataCenters = (List<String>) config.get(DATA_CENTERS);
        List<String> caches = (List<String>) config.get(CACHES);

        new DrCheck().validateConsistency(dataCenters, caches);
    }

    private static Map<String, Object> loadConfiguration(String configPath) throws IOException {
        try (InputStream inputStream = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            return yaml.load(inputStream);
        }
    }

    private void validateConsistency(List<String> dataCenters, List<String> caches) throws CacheContentsMismatchException {
        List<IgniteClient> clients = dataCenters.stream()
                .map(this::startClient)
                .collect(Collectors.toList());

        IgniteClient client1 = clients.get(0);
        String addr1 = dataCenters.get(0);

        for (int i = 1; i < clients.size(); i++) {
            IgniteClient client2 = clients.get(i);
            String addr2 = dataCenters.get(i);

            for (String cacheName : caches) {
                ClientCache<Object, Object> cache1 = client1.cache(cacheName);
                ClientCache<Object, Object> cache2 = client2.cache(cacheName);

                try {
                    matchContents(cache1, cache2);
                } catch (CacheContentsMismatchException ex) {
                    throw new CacheContentsMismatchException("Cache contents in different data centers don't match" +
                            "[cacheName=" + cacheName + ", addr1=" + addr1 + ", addr2=" + addr2 + "]", ex);
                }
            }
        }
    }

    private IgniteClient startClient(String ip) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setAddresses(ip);

        return Ignition.startClient(clientConfiguration);
    }

    private void matchContents(ClientCache<Object, Object> cache1, ClientCache<Object, Object> cache2)
            throws CacheContentsMismatchException {
        int size1 = cache1.size();
        int size2 = cache2.size();

        if (size1 != size2) {
            throw new CacheContentsMismatchException("Cache sizes don't match [size1=" + size1 + ", size2=" + size2 + "]");
        }

        Query<Cache.Entry<Object, Object>> qry = new ScanQuery<>();

        QueryCursor<Cache.Entry<Object, Object>> cur = cache1.query(qry);

        for (Cache.Entry<Object, Object> entry : cur) {
            Object key = entry.getKey();
            Object val1 = entry.getValue();
            Object val2 = cache2.get(key);

            if (!val1.equals(val2)) {
                throw new CacheContentsMismatchException("Values don't match " +
                        "[key=" + key + ", val1=" + val1 + ", val2=" + val2 + "]");
            }
        }
    }
}
