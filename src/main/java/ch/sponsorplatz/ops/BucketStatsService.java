package ch.sponsorplatz.ops;

import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregierte Object-Storage-Statistiken (Anzahl Objekte + Bytes pro Bucket)
 * für das Ops-Dashboard. Liefert leere Stats wenn provider=lokal — die lokale
 * Disk-Usage wird dort separat reportet.
 */
@Service
public class BucketStatsService {

    private static final Logger log = LoggerFactory.getLogger(BucketStatsService.class);

    private final ObjectStorage client;
    private final String namespace;
    private final String uploadsBucket;
    private final String backupsBucket;

    public BucketStatsService(@Autowired(required = false) ObjectStorage client,
                              @Value("${sponsorplatz.storage.oci.namespace:}") String namespace,
                              @Value("${sponsorplatz.storage.oci.bucket-uploads:sponsorplatz-uploads}") String uploadsBucket,
                              @Value("${sponsorplatz.storage.oci.bucket-backups:sponsorplatz-backups}") String backupsBucket) {
        this.client = client;
        this.namespace = namespace;
        this.uploadsBucket = uploadsBucket;
        this.backupsBucket = backupsBucket;
    }

    public Map<String, BucketStats> alleStats() {
        Map<String, BucketStats> result = new LinkedHashMap<>();
        result.put("uploads", einzelStats(uploadsBucket));
        result.put("backups", einzelStats(backupsBucket));
        return result;
    }

    private BucketStats einzelStats(String bucket) {
        if (client == null || namespace.isBlank()) {
            return new BucketStats(false, 0, 0);
        }
        long count = 0;
        long total = 0;
        String startWith = null;
        try {
            do {
                var resp = client.listObjects(ListObjectsRequest.builder()
                        .namespaceName(namespace)
                        .bucketName(bucket)
                        .start(startWith)
                        .fields("size")
                        .limit(1000)
                        .build());
                var listing = resp.getListObjects();
                for (var obj : listing.getObjects()) {
                    count++;
                    Long size = obj.getSize();
                    if (size != null) total += size;
                }
                startWith = listing.getNextStartWith();
            } while (startWith != null);
            return new BucketStats(true, count, total);
        } catch (BmcException e) {
            log.warn("Bucket-Stats fehlgeschlagen für {}: {} {}",
                    bucket, e.getStatusCode(), e.getMessage());
            return new BucketStats(false, 0, 0);
        }
    }

    public record BucketStats(boolean verfuegbar, long anzahl, long bytesTotal) {
        public double mb() {
            return bytesTotal / 1024.0 / 1024.0;
        }
    }
}
